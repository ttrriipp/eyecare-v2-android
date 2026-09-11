package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SignInState {
    data class EnterCredentials(
        val phoneNumber: String = "",
        val password: String = "",
        val error: String? = null,
        val isSubmitting: Boolean = false,
        val cooldownRemainingSeconds: Int = 0,
    ) : SignInState

    data class VerifyOtp(
        val challengeId: String,
        val expiresAt: String,
        val phoneNumber: String,
        val password: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
        val isVerifying: Boolean = false,
        val resendCooldownSeconds: Int = 0,
    ) : SignInState

    data class Success(val session: AuthenticatedSession) : SignInState
}
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<SignInState>(SignInState.EnterCredentials())
    val state: StateFlow<SignInState> = _state.asStateFlow()
    private var cooldownJob: Job? = null

    fun updatePhone(value: String) {
        val current = _state.value
        if (current is SignInState.EnterCredentials && !current.isSubmitting) {
            _state.value = current.copy(phoneNumber = value, error = null)
        }
    }

    fun updatePassword(value: String) {
        val current = _state.value
        if (current is SignInState.EnterCredentials && !current.isSubmitting) {
            _state.value = current.copy(password = value, error = null)
        }
    }

    fun signIn() {
        val current = _state.value
        if (
            current !is SignInState.EnterCredentials ||
            current.isSubmitting ||
            current.cooldownRemainingSeconds > 0
        ) return

        if (current.phoneNumber.isBlank()) {
            _state.value = current.copy(error = "Phone number is required")
            return
        }
        if (current.password.isBlank()) {
            _state.value = current.copy(error = "Password is required")
            return
        }

        // Set this synchronously so a second tap cannot launch another request
        // before the coroutine gets its first turn on the main dispatcher.
        _state.value = current.copy(error = null, isSubmitting = true)

        viewModelScope.launch {
            authRepository.beginLogin(
                phone = current.phoneNumber,
                password = current.password,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { outcome ->
                when (outcome) {
                    is LoginOutcome.Authenticated -> {
                        _state.value = SignInState.Success(
                            AuthenticatedSession(token = outcome.token, account = outcome.account),
                        )
                    }

                    is LoginOutcome.OtpRequired -> {
                        _state.value = SignInState.VerifyOtp(
                            challengeId = outcome.challengeId,
                            expiresAt = outcome.expiresAt,
                            phoneNumber = current.phoneNumber,
                            password = current.password,
                        )
                    }
                }
            }.onFailure { error ->
                val latest = _state.value
                if (latest is SignInState.EnterCredentials && latest.isSubmitting) {
                    val cooldownSeconds = rateLimitCooldownSeconds(error)
                    _state.value = latest.copy(
                        isSubmitting = false,
                        error = signInErrorMessage(error),
                        cooldownRemainingSeconds = cooldownSeconds,
                    )
                    if (cooldownSeconds > 0) startCooldown(cooldownSeconds)
                }
            }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (
            current is SignInState.VerifyOtp &&
            !current.isResending &&
            !current.isVerifying
        ) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyOtp() {
        val current = _state.value
        if (
            current !is SignInState.VerifyOtp ||
            current.isResending ||
            current.isVerifying ||
            current.code.length != 6
        ) return

        _state.value = current.copy(error = null, isVerifying = true)

        viewModelScope.launch {
            authRepository.verifyLogin(
                challengeId = current.challengeId,
                code = current.code,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { session ->
                val latest = _state.value
                if (
                    latest is SignInState.VerifyOtp &&
                    latest.isVerifying &&
                    latest.challengeId == current.challengeId
                ) {
                    _state.value = SignInState.Success(session)
                }
            }.onFailure { error ->
                val latest = _state.value
                if (
                    latest is SignInState.VerifyOtp &&
                    latest.isVerifying &&
                    latest.challengeId == current.challengeId
                ) {
                    _state.value = latest.copy(
                        isVerifying = false,
                        error = authErrorMessage(error, "Could not verify the code."),
                    )
                }
            }
        }
    }

    fun resendOtp() {
        val current = _state.value
        if (
            current !is SignInState.VerifyOtp ||
            current.isResending ||
            current.isVerifying
        ) return

        _state.value = current.copy(isResending = true, error = null, resendCooldownSeconds = 0)

        viewModelScope.launch {
            authRepository.beginLogin(
                phone = current.phoneNumber,
                password = current.password,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { outcome ->
                when (outcome) {
                    is LoginOutcome.OtpRequired -> {
                        val latest = _state.value
                        if (
                            latest is SignInState.VerifyOtp &&
                            latest.isResending &&
                            latest.challengeId == current.challengeId
                        ) {
                            _state.value = latest.copy(
                                challengeId = outcome.challengeId,
                                expiresAt = outcome.expiresAt,
                                code = "",
                                error = null,
                                isResending = false,
                                resendCooldownSeconds = 0,
                            )
                        }
                    }

                    is LoginOutcome.Authenticated -> {
                        val latest = _state.value
                        if (
                            latest is SignInState.VerifyOtp &&
                            latest.isResending &&
                            latest.challengeId == current.challengeId
                        ) {
                            _state.value = SignInState.Success(
                                AuthenticatedSession(token = outcome.token, account = outcome.account),
                            )
                        }
                    }
                }
            }.onFailure { error ->
                val latest = _state.value
                if (
                    latest is SignInState.VerifyOtp &&
                    latest.isResending &&
                    latest.challengeId == current.challengeId
                ) {
                    _state.value = latest.copy(
                        isResending = false,
                        resendCooldownSeconds = rateLimitCooldownSeconds(error),
                        error = authErrorMessage(error, "Could not resend the code."),
                    )
                }
            }
        }
    }

    fun back() {
        cooldownJob?.cancel()
        _state.value = when (val current = _state.value) {
            is SignInState.VerifyOtp -> {
                if (current.isResending || current.isVerifying) {
                    current
                } else {
                    SignInState.EnterCredentials(
                        phoneNumber = current.phoneNumber,
                        password = "",
                    )
                }
            }
            else -> SignInState.EnterCredentials()
        }
    }

    private fun signInErrorMessage(error: Throwable): String =
        if (isRateLimited(error)) {
            "Too many sign-in attempts. Please wait before trying again."
        } else {
            authErrorMessage(error, "Sign in failed. Please try again.")
        }

    private fun rateLimitCooldownSeconds(error: Throwable): Int {
        if (!isRateLimited(error)) return 0

        val apiError = error as? ApiDomainError
        return (apiError?.retryAfterSeconds ?: DEFAULT_RATE_LIMIT_SECONDS)
            .coerceIn(1L, MAX_RATE_LIMIT_SECONDS)
            .toInt()
    }

    private fun isRateLimited(error: Throwable): Boolean {
        val apiError = error as? ApiDomainError ?: return false
        return apiError.httpStatus == 429 || apiError.code in RATE_LIMIT_CODES
    }

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in seconds downTo 1) {
                val current = _state.value as? SignInState.EnterCredentials ?: return@launch
                _state.value = current.copy(cooldownRemainingSeconds = remaining)
                delay(1_000)
            }

            val current = _state.value as? SignInState.EnterCredentials ?: return@launch
            _state.value = current.copy(cooldownRemainingSeconds = 0)
        }
    }

    private companion object {
        const val DEFAULT_RATE_LIMIT_SECONDS = 60L
        const val MAX_RATE_LIMIT_SECONDS = 15 * 60L
        val RATE_LIMIT_CODES = setOf(
            AuthApiCodes.API_RATE_LIMIT_REACHED,
            AuthApiCodes.OTP_RATE_LIMIT_REACHED,
        )
    }
}
