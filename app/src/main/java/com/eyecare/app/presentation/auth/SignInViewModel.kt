package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SignInState {
    data class EnterContact(
        val contactValue: String = "",
        val password: String = "",
        val error: String? = null,
    ) : SignInState
    data class VerifyOtp(
        val challengeId: String,
        val expiresAt: String,
        val contactValue: String,
        val password: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
    ) : SignInState
    data class Success(val session: AuthenticatedSession) : SignInState
}

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<SignInState>(SignInState.EnterContact())
    val state: StateFlow<SignInState> = _state.asStateFlow()

    fun updateContact(value: String) {
        val current = _state.value
        if (current is SignInState.EnterContact) {
            _state.value = current.copy(contactValue = value, error = null)
        }
    }

    fun updatePassword(value: String) {
        val current = _state.value
        if (current is SignInState.EnterContact) {
            _state.value = current.copy(password = value, error = null)
        }
    }

    fun signIn() {
        val current = _state.value
        if (current !is SignInState.EnterContact) return

        if (current.contactValue.isBlank()) {
            _state.value = current.copy(error = "Contact is required")
            return
        }
        if (current.password.isBlank()) {
            _state.value = current.copy(error = "Password is required")
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.beginLogin(
                contactValue = current.contactValue,
                password = current.password,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { outcome ->
                when (outcome) {
                    is LoginOutcome.Authenticated -> {
                        _state.value = SignInState.Success(
                            AuthenticatedSession(token = outcome.token, account = outcome.account)
                        )
                    }
                    is LoginOutcome.OtpRequired -> {
                        _state.value = SignInState.VerifyOtp(
                            challengeId = outcome.challengeId,
                            expiresAt = outcome.expiresAt,
                            contactValue = current.contactValue,
                            password = current.password,
                        )
                    }
                }
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                _state.value = current.copy(error = apiError?.message ?: "Sign in failed")
            }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (current is SignInState.VerifyOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyOtp() {
        val current = _state.value
        if (current !is SignInState.VerifyOtp || current.code.length != 6) return

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.verifyLogin(
                challengeId = current.challengeId,
                code = current.code,
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { session ->
                _state.value = SignInState.Success(session)
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                _state.value = current.copy(error = apiError?.message ?: "Invalid code")
            }
        }
    }

    fun resendOtp() {
        val current = _state.value
        if (current !is SignInState.VerifyOtp) return

        viewModelScope.launch {
            _state.value = current.copy(isResending = true)
            authRepository.beginLogin(
                contactValue = current.contactValue,
                password = current.password,
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            ).onSuccess { outcome ->
                when (outcome) {
                    is LoginOutcome.OtpRequired -> {
                        _state.value = current.copy(
                            challengeId = outcome.challengeId,
                            expiresAt = outcome.expiresAt,
                            code = "",
                            error = null,
                            isResending = false,
                        )
                    }
                    is LoginOutcome.Authenticated -> {
                        _state.value = SignInState.Success(
                            AuthenticatedSession(token = outcome.token, account = outcome.account)
                        )
                    }
                }
            }.onFailure {
                _state.value = current.copy(isResending = false)
            }
        }
    }

    fun back() {
        _state.value = when (val current = _state.value) {
            is SignInState.VerifyOtp -> SignInState.EnterContact(
                contactValue = current.contactValue,
                password = "",
            )
            else -> SignInState.EnterContact()
        }
    }

    fun clearPassword() {
        val current = _state.value
        if (current is SignInState.EnterContact) {
            _state.value = current.copy(password = "")
        }
    }
}
