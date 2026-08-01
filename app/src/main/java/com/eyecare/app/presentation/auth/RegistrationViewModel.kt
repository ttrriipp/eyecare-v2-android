package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RegistrationState {
    data object ChooseMethod : RegistrationState
    data class EnterContact(val method: ContactType, val contactValue: String = "", val error: String? = null) : RegistrationState
    data class VerifyContactOtp(
        val method: ContactType,
        val contactValue: String,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
    ) : RegistrationState
    data class EnterDetails(
        val registrationToken: String,
        val contactType: ContactType,
        val firstName: String = "",
        val middleName: String = "",
        val lastName: String = "",
        val dateOfBirth: String = "",
        val password: String = "",
        val passwordConfirmation: String = "",
        val invitationCode: String = "",
        val privacyAccepted: Boolean = false,
        val termsAccepted: Boolean = false,
        val policies: PolicyMetadata? = null,
        val isLoadingPolicies: Boolean = true,
        val errors: Map<String, String> = emptyMap(),
    ) : RegistrationState
    data class OptionalSecondary(
        val session: AuthenticatedSession,
        val secondaryType: ContactType,
        val secondaryValue: String = "",
    ) : RegistrationState
    data class VerifySecondaryOtp(
        val session: AuthenticatedSession,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
    ) : RegistrationState
    data class Success(val session: AuthenticatedSession) : RegistrationState
    data class Error(val message: String) : RegistrationState
}

@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.ChooseMethod)
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    fun chooseMethod(method: ContactType) {
        _state.value = RegistrationState.EnterContact(method = method)
    }

    fun updateContactValue(value: String) {
        val current = _state.value
        if (current is RegistrationState.EnterContact) {
            _state.value = current.copy(contactValue = value, error = null)
        }
    }

    fun requestContactOtp() {
        val current = _state.value
        if (current !is RegistrationState.EnterContact) return
        val contactType = if (current.method == ContactType.EMAIL) "email" else "phone"

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.requestRegistrationOtp(contactType, current.contactValue)
                .onSuccess { challenge ->
                    _state.value = RegistrationState.VerifyContactOtp(
                        method = current.method,
                        contactValue = current.contactValue,
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
                    )
                }
                .onFailure { error ->
                    _state.value = current.copy(error = error.message ?: "Failed to send code")
                }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (current is RegistrationState.VerifyContactOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyContactOtp() {
        val current = _state.value
        if (current !is RegistrationState.VerifyContactOtp || current.code.length != 6) return

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.verifyRegistrationOtp(current.challengeId, current.code)
                .onSuccess { proof ->
                    loadPoliciesAndShowDetails(proof.token, current.method, current.contactValue)
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    _state.value = current.copy(
                        error = apiError?.message ?: "Invalid code",
                    )
                }
        }
    }

    fun resendOtp() {
        val current = _state.value
        if (current !is RegistrationState.VerifyContactOtp) return
        val contactType = if (current.method == ContactType.EMAIL) "email" else "phone"

        viewModelScope.launch {
            _state.value = current.copy(isResending = true)
            authRepository.requestRegistrationOtp(contactType, current.contactValue)
                .onSuccess { challenge ->
                    _state.value = current.copy(
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
                        code = "",
                        error = null,
                        isResending = false,
                    )
                }
                .onFailure {
                    _state.value = current.copy(isResending = false)
                }
        }
    }

    fun updateDetails(
        firstName: String? = null,
        middleName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        password: String? = null,
        passwordConfirmation: String? = null,
        invitationCode: String? = null,
        privacyAccepted: Boolean? = null,
        termsAccepted: Boolean? = null,
    ) {
        val current = _state.value
        if (current !is RegistrationState.EnterDetails) return
        _state.value = current.copy(
            firstName = firstName ?: current.firstName,
            middleName = middleName ?: current.middleName,
            lastName = lastName ?: current.lastName,
            dateOfBirth = dateOfBirth ?: current.dateOfBirth,
            password = password ?: current.password,
            passwordConfirmation = passwordConfirmation ?: current.passwordConfirmation,
            invitationCode = invitationCode ?: current.invitationCode,
            privacyAccepted = privacyAccepted ?: current.privacyAccepted,
            termsAccepted = termsAccepted ?: current.termsAccepted,
            errors = emptyMap(),
        )
    }

    fun submitRegistration() {
        val current = _state.value
        if (current !is RegistrationState.EnterDetails) return

        val errors = validateDetails(current)
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return
        }

        val policies = current.policies
        if (policies == null) {
            _state.value = current.copy(errors = mapOf("_" to "Policy information not loaded"))
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(errors = emptyMap())
            authRepository.register(
                registrationToken = current.registrationToken,
                firstName = current.firstName.trim(),
                middleName = current.middleName.trim().ifBlank { null },
                lastName = current.lastName.trim(),
                dateOfBirth = current.dateOfBirth,
                password = current.password,
                passwordConfirmation = current.passwordConfirmation,
                privacyPolicyVersion = policies.privacyPolicyVersion,
                termsVersion = policies.termsVersion,
                invitationCode = current.invitationCode.trim().ifBlank { null },
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            )
                .onSuccess { session ->
                    handlePostRegistration(session, current.contactType)
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    _state.value = current.copy(
                        errors = mapOf("_" to (apiError?.message ?: "Registration failed")),
                    )
                }
        }
    }

    fun skipSecondary() {
        val current = _state.value
        if (current is RegistrationState.OptionalSecondary) {
            _state.value = RegistrationState.Success(current.session)
        }
    }

    fun updateSecondaryValue(value: String) {
        val current = _state.value
        if (current is RegistrationState.OptionalSecondary) {
            _state.value = current.copy(secondaryValue = value)
        }
    }

    fun startSecondaryVerification() {
        val current = _state.value
        if (current !is RegistrationState.OptionalSecondary || current.secondaryValue.isBlank()) return

        viewModelScope.launch {
            val contactType = if (current.secondaryType == ContactType.EMAIL) "email" else "phone"
            accountRepository.requestContactOtp(
                stepUpToken = "", // Will need step-up first in real flow
                contactType = contactType,
                contactValue = current.secondaryValue.trim(),
            ).onSuccess { challenge ->
                _state.value = RegistrationState.VerifySecondaryOtp(
                    session = current.session,
                    challengeId = challenge.challengeId,
                    expiresAt = challenge.expiresAt,
                )
            }.onFailure { error ->
                // Secondary verification failure doesn't block account
                _state.value = RegistrationState.Success(current.session)
            }
        }
    }

    fun updateSecondaryOtp(code: String) {
        val current = _state.value
        if (current is RegistrationState.VerifySecondaryOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifySecondaryOtp() {
        val current = _state.value
        if (current !is RegistrationState.VerifySecondaryOtp || current.code.length != 6) return

        viewModelScope.launch {
            accountRepository.verifyContactOtp(current.challengeId, current.code)
                .onSuccess {
                    _state.value = RegistrationState.Success(current.session)
                }
                .onFailure { error ->
                    // Secondary failure doesn't block account
                    _state.value = RegistrationState.Success(current.session)
                }
        }
    }

    fun back() {
        _state.value = when (val current = _state.value) {
            is RegistrationState.EnterContact -> RegistrationState.ChooseMethod
            is RegistrationState.VerifyContactOtp -> RegistrationState.EnterContact(current.method, current.contactValue)
            is RegistrationState.EnterDetails -> RegistrationState.EnterContact(current.contactType, "")
            is RegistrationState.OptionalSecondary -> RegistrationState.Success(current.session)
            is RegistrationState.VerifySecondaryOtp -> RegistrationState.Success(current.session)
            else -> current
        }
    }

    private fun loadPoliciesAndShowDetails(registrationToken: String, contactType: ContactType, contactValue: String) {
        viewModelScope.launch {
            authRepository.getPolicies()
                .onSuccess { policies ->
                    _state.value = RegistrationState.EnterDetails(
                        registrationToken = registrationToken,
                        contactType = contactType,
                        policies = policies,
                        isLoadingPolicies = false,
                    )
                }
                .onFailure {
                    _state.value = RegistrationState.EnterDetails(
                        registrationToken = registrationToken,
                        contactType = contactType,
                        policies = null,
                        isLoadingPolicies = false,
                    )
                }
        }
    }

    private fun handlePostRegistration(session: AuthenticatedSession, contactType: ContactType) {
        val account = session.account
        val hasEmail = !account.email.isNullOrBlank()
        val hasPhone = !account.phone.isNullOrBlank()
        val secondaryType = when {
            contactType == ContactType.EMAIL && !hasPhone -> ContactType.PHONE
            contactType == ContactType.PHONE && !hasEmail -> ContactType.EMAIL
            else -> null
        }

        if (secondaryType != null) {
            _state.value = RegistrationState.OptionalSecondary(
                session = session,
                secondaryType = secondaryType,
            )
        } else {
            _state.value = RegistrationState.Success(session)
        }
    }

    private fun validateDetails(d: RegistrationState.EnterDetails): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (d.firstName.isBlank()) errors["firstName"] = "First name is required"
        if (d.lastName.isBlank()) errors["lastName"] = "Last name is required"
        if (d.dateOfBirth.isBlank()) errors["dateOfBirth"] = "Date of birth is required"
        if (d.password.length < 12) errors["password"] = "Password must be at least 12 characters"
        if (d.password != d.passwordConfirmation) errors["passwordConfirmation"] = "Passwords do not match"
        if (!d.privacyAccepted) errors["privacy"] = "You must accept the Privacy Policy"
        if (!d.termsAccepted) errors["terms"] = "You must accept the Terms of Service"
        return errors
    }
}
