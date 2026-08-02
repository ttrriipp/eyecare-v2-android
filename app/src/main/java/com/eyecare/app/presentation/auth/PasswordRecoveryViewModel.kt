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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RecoveryState {
    data class EnterPhone(
        val phoneNumber: String = "",
        val error: String? = null,
    ) : RecoveryState

    data class EnterOtp(
        val challengeId: String,
        val expiresAt: String,
        val phoneNumber: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
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

    fun updatePhone(value: String) {
        val current = _state.value
        if (current is RecoveryState.EnterPhone) {
            _state.value = current.copy(phoneNumber = value, error = null)
        }
    }

    fun requestOtp() {
        val current = _state.value
        if (
            current !is RecoveryState.EnterPhone ||
            toPhilippineLocalDigits(current.phoneNumber).length < 10
        ) return

        val fullPhone = toPhilippineE164(current.phoneNumber)

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.requestPasswordRecoveryOtp(fullPhone)
                .onSuccess { challenge ->
                    _state.value = RecoveryState.EnterOtp(
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
                        phoneNumber = current.phoneNumber,
                    )
                }
                .onFailure { error ->
                    _state.value = current.copy(
                        error = authErrorMessage(
                            error,
                            "Could not send a verification code. Please try again.",
                        ),
                    )
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

        viewModelScope.launch {
            _state.value = current.copy(isResending = true, error = null, code = "")
            authRepository.requestPasswordRecoveryOtp(toPhilippineE164(current.phoneNumber))
                .onSuccess { challenge ->
                    _state.value = current.copy(
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
                        code = "",
                        error = null,
                        isResending = false,
                    )
                }
                .onFailure { error ->
                    _state.value = current.copy(
                        isResending = false,
                        error = authErrorMessage(error, "Could not resend the code."),
                    )
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
}
