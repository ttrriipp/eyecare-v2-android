package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.toPhilippineE164
import com.eyecare.app.domain.model.toPhilippineLocalDigits
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.presentation.common.isRateLimited
import com.eyecare.app.presentation.common.rateLimitCooldownSeconds
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecoveryState {
    data class EnterPhone(
        val phoneNumber: String = "",
        val error: String? = null,
        val isRequesting: Boolean = false,
        val cooldownRemainingSeconds: Int = 0,
    ) : RecoveryState

    data class EnterOtp(
        val challengeId: String,
        val expiresAt: String,
        val phoneNumber: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
        val resendCooldownSeconds: Int = 0,
    ) : RecoveryState

    data class EnterNewPassword(
        val challengeId: String,
        val code: String,
        val phoneNumber: String = "",
        val expiresAt: String = "",
        val password: String = "",
        val passwordConfirmation: String = "",
        val errors: Map<String, String> = emptyMap(),
    ) : RecoveryState

    data class Success(val session: AuthenticatedSession) : RecoveryState
}
@HiltViewModel
class PasswordRecoveryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<RecoveryState>(RecoveryState.EnterPhone())
    val state: StateFlow<RecoveryState> = _state.asStateFlow()
    private var phoneOtpCooldownJob: Job? = null

    fun updatePhone(value: String) {
        val current = _state.value
        if (current is RecoveryState.EnterPhone && !current.isRequesting) {
            _state.value = current.copy(phoneNumber = value, error = null)
        }
    }

    fun requestOtp() {
        val current = _state.value
        if (
            current !is RecoveryState.EnterPhone ||
            toPhilippineLocalDigits(current.phoneNumber).length < 10 ||
            current.isRequesting ||
            current.cooldownRemainingSeconds > 0
        ) return

        val fullPhone = toPhilippineE164(current.phoneNumber)

        _state.value = current.copy(error = null, isRequesting = true)
        viewModelScope.launch {
            authRepository.requestPasswordRecoveryOtp(fullPhone)
                .onSuccess { challenge ->
                    val latest = _state.value
                    if (latest is RecoveryState.EnterPhone && latest.isRequesting) {
                        _state.value = RecoveryState.EnterOtp(
                            challengeId = challenge.challengeId,
                            expiresAt = challenge.expiresAt,
                            phoneNumber = latest.phoneNumber,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is RecoveryState.EnterPhone && latest.isRequesting) {
                        val cooldownSeconds = error.rateLimitCooldownSeconds()
                        _state.value = latest.copy(
                            isRequesting = false,
                            cooldownRemainingSeconds = cooldownSeconds,
                            error = recoveryCodeErrorMessage(
                                error,
                                "Could not send a verification code. Please try again.",
                            ),
                        )
                        if (cooldownSeconds > 0) startPhoneOtpCooldown(cooldownSeconds)
                    }
                }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (current is RecoveryState.EnterOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyOtp() {
        val current = _state.value
        if (
            current !is RecoveryState.EnterOtp ||
            current.isResending ||
            current.code.length != 6
        ) return

        _state.value = RecoveryState.EnterNewPassword(
            challengeId = current.challengeId,
            code = current.code,
            phoneNumber = current.phoneNumber,
            expiresAt = current.expiresAt,
        )
    }

    fun resendOtp() {
        val current = _state.value
        if (current !is RecoveryState.EnterOtp || current.isResending) return

        _state.value = current.copy(isResending = true, error = null, code = "", resendCooldownSeconds = 0)
        viewModelScope.launch {
            authRepository.requestPasswordRecoveryOtp(toPhilippineE164(current.phoneNumber))
                .onSuccess { challenge ->
                    val latest = _state.value
                    if (latest is RecoveryState.EnterOtp && latest.isResending && latest.challengeId == current.challengeId) {
                        _state.value = latest.copy(
                            challengeId = challenge.challengeId,
                            expiresAt = challenge.expiresAt,
                            code = "",
                            error = null,
                            isResending = false,
                            resendCooldownSeconds = 0,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is RecoveryState.EnterOtp && latest.isResending && latest.challengeId == current.challengeId) {
                        _state.value = latest.copy(
                            isResending = false,
                            resendCooldownSeconds = error.rateLimitCooldownSeconds(),
                            error = recoveryCodeErrorMessage(error, "Could not resend the code."),
                        )
                    }
                }
        }
    }

    fun updatePassword(value: String) {
        val current = _state.value
        if (current is RecoveryState.EnterNewPassword) {
            _state.value = current.copy(password = value, errors = current.errors - "password")
        }
    }

    fun updatePasswordConfirmation(value: String) {
        val current = _state.value
        if (current is RecoveryState.EnterNewPassword) {
            _state.value = current.copy(
                passwordConfirmation = value,
                errors = current.errors - "passwordConfirmation",
            )
        }
    }

    fun resetPassword() {
        val current = _state.value
        if (current !is RecoveryState.EnterNewPassword) return

        val errors = mutableMapOf<String, String>()
        if (current.password.length < 12) errors["password"] = "Password must be at least 12 characters"
        if (current.password != current.passwordConfirmation) errors["passwordConfirmation"] = "Passwords do not match"
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(errors = emptyMap())
            authRepository.recoverPassword(
                challengeId = current.challengeId,
                code = current.code,
                password = current.password,
                passwordConfirmation = current.passwordConfirmation,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { session ->
                _state.value = RecoveryState.Success(session)
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                if (apiError?.code == AuthApiCodes.INVALID_OTP) {
                    _state.value = RecoveryState.EnterOtp(
                        challengeId = current.challengeId,
                        expiresAt = current.expiresAt,
                        phoneNumber = current.phoneNumber,
                        code = "",
                        error = "Invalid or expired code. Please try again.",
                    )
                } else {
                    _state.value = current.copy(
                        errors = mapOf(
                            "_" to authErrorMessage(error, "Password reset failed."),
                        ),
                    )
                }
            }
        }
    }

    fun back() {
        phoneOtpCooldownJob?.cancel()
        _state.value = when (val current = _state.value) {
            is RecoveryState.EnterOtp -> RecoveryState.EnterPhone(current.phoneNumber)
            is RecoveryState.EnterNewPassword -> RecoveryState.EnterOtp(
                challengeId = current.challengeId,
                expiresAt = current.expiresAt,
                phoneNumber = current.phoneNumber,
                code = current.code,
            )
            else -> RecoveryState.EnterPhone()
        }
    }

    private fun startPhoneOtpCooldown(seconds: Int) {
        phoneOtpCooldownJob?.cancel()
        phoneOtpCooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                val current = _state.value as? RecoveryState.EnterPhone ?: return@launch
                if (!current.isRequesting) {
                    _state.value = current.copy(cooldownRemainingSeconds = remaining)
                }
                delay(1_000)
            }
            val current = _state.value as? RecoveryState.EnterPhone ?: return@launch
            _state.value = current.copy(cooldownRemainingSeconds = 0)
        }
    }

    private fun recoveryCodeErrorMessage(error: Throwable, fallback: String): String =
        if (error.isRateLimited()) {
            "Too many code requests. Please wait before requesting another code."
        } else {
            authErrorMessage(error, fallback)
        }
}
