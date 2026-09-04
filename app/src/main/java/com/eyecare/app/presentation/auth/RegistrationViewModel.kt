package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private val registrationZone = ZoneId.of("Asia/Manila")

sealed interface RegistrationState {
    data class EnterPhone(
        val phoneNumber: String = "",
        val error: String? = null,
    ) : RegistrationState

    data class VerifyPhoneOtp(
        val phoneNumber: String,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
        val isVerifying: Boolean = false,
    ) : RegistrationState

    data class EnterDetails(
        val registrationToken: String,
        val firstName: String = "",
        val middleName: String = "",
        val lastName: String = "",
        val dateOfBirth: String = "",
        val email: String = "",
        val password: String = "",
        val passwordConfirmation: String = "",
        val invitationCode: String = "",
        val privacyAccepted: Boolean = false,
        val termsAccepted: Boolean = false,
        val policies: PolicyMetadata? = null,
        val isLoadingPolicies: Boolean = true,
        val errors: Map<String, String> = emptyMap(),
    ) : RegistrationState

    data class Success(val session: AuthenticatedSession) : RegistrationState
}
@HiltViewModel
class RegistrationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : ViewModel() {

    private val _state = MutableStateFlow<RegistrationState>(RegistrationState.EnterPhone())
    val state: StateFlow<RegistrationState> = _state.asStateFlow()

    fun updatePhone(value: String) {
        val current = _state.value
        if (current is RegistrationState.EnterPhone) {
            _state.value = current.copy(phoneNumber = value, error = null)
        }
    }

    fun requestPhoneOtp() {
        val current = _state.value
        if (current !is RegistrationState.EnterPhone || current.phoneNumber.isBlank()) return

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            authRepository.requestRegistrationOtp(current.phoneNumber)
                .onSuccess { challenge ->
                    _state.value = RegistrationState.VerifyPhoneOtp(
                        phoneNumber = current.phoneNumber,
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
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
        if (
            current is RegistrationState.VerifyPhoneOtp &&
            !current.isResending &&
            !current.isVerifying
        ) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyPhoneOtp() {
        val current = _state.value
        if (
            current !is RegistrationState.VerifyPhoneOtp ||
            current.isResending ||
            current.isVerifying ||
            current.code.length != 6
        ) return

        _state.value = current.copy(error = null, isVerifying = true)

        viewModelScope.launch {
            authRepository.verifyRegistrationOtp(current.challengeId, current.code)
                .onSuccess { proof ->
                    val latest = _state.value
                    if (
                        latest is RegistrationState.VerifyPhoneOtp &&
                        latest.isVerifying &&
                        latest.challengeId == current.challengeId
                    ) {
                        loadPoliciesAndShowDetails(proof.token)
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (
                        latest is RegistrationState.VerifyPhoneOtp &&
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
            current !is RegistrationState.VerifyPhoneOtp ||
            current.isResending ||
            current.isVerifying
        ) return

        _state.value = current.copy(isResending = true, error = null)

        viewModelScope.launch {
            authRepository.requestRegistrationOtp(current.phoneNumber)
                .onSuccess { challenge ->
                    val latest = _state.value
                    if (
                        latest is RegistrationState.VerifyPhoneOtp &&
                        latest.isResending &&
                        latest.challengeId == current.challengeId
                    ) {
                        _state.value = latest.copy(
                            challengeId = challenge.challengeId,
                            expiresAt = challenge.expiresAt,
                            code = "",
                            error = null,
                            isResending = false,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (
                        latest is RegistrationState.VerifyPhoneOtp &&
                        latest.isResending &&
                        latest.challengeId == current.challengeId
                    ) {
                        _state.value = latest.copy(
                            isResending = false,
                            error = authErrorMessage(error, "Could not resend the code."),
                        )
                    }
                }
        }
    }

    fun updateDetails(
        firstName: String? = null,
        middleName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        email: String? = null,
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
            email = email ?: current.email,
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
                email = current.email.trim().ifBlank { null },
                password = current.password,
                passwordConfirmation = current.passwordConfirmation,
                privacyPolicyVersion = policies.privacyPolicyVersion,
                termsVersion = policies.termsVersion,
                invitationCode = current.invitationCode.trim().ifBlank { null },
                deviceName = deviceIdentityProvider.deviceName(),
                installationId = deviceIdentityProvider.getOrCreateInstallationId(),
            )
                .onSuccess { session ->
                    _state.value = RegistrationState.Success(session)
                }
                .onFailure { error ->
                    _state.value = current.copy(errors = registrationErrors(error))
                }
        }
    }

    fun retryLoadPolicies() {
        val current = _state.value
        if (current !is RegistrationState.EnterDetails || current.policies != null) return

        _state.value = current.copy(isLoadingPolicies = true)
        viewModelScope.launch {
            authRepository.getPolicies()
                .onSuccess { policies ->
                    val latest = _state.value
                    if (latest is RegistrationState.EnterDetails) {
                        _state.value = latest.copy(policies = policies, isLoadingPolicies = false)
                    }
                }
                .onFailure {
                    val latest = _state.value
                    if (latest is RegistrationState.EnterDetails) {
                        _state.value = latest.copy(isLoadingPolicies = false)
                    }
                }
        }
    }

    fun back() {
        _state.value = when (val current = _state.value) {
            is RegistrationState.VerifyPhoneOtp -> {
                if (current.isResending || current.isVerifying) {
                    current
                } else {
                    RegistrationState.EnterPhone(current.phoneNumber)
                }
            }
            is RegistrationState.EnterDetails -> RegistrationState.EnterPhone()
            else -> current
        }
    }

    private fun loadPoliciesAndShowDetails(registrationToken: String) {
        viewModelScope.launch {
            authRepository.getPolicies()
                .onSuccess { policies ->
                    _state.value = RegistrationState.EnterDetails(
                        registrationToken = registrationToken,
                        policies = policies,
                        isLoadingPolicies = false,
                    )
                }
                .onFailure {
                    _state.value = RegistrationState.EnterDetails(
                        registrationToken = registrationToken,
                        policies = null,
                        isLoadingPolicies = false,
                    )
                }
        }
    }

    private fun validateDetails(d: RegistrationState.EnterDetails): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        if (d.firstName.isBlank()) errors["firstName"] = "First name is required"
        if (d.lastName.isBlank()) errors["lastName"] = "Last name is required"
        if (d.dateOfBirth.isBlank()) {
            errors["dateOfBirth"] = "Date of birth is required"
        } else {
            val dateOfBirth = runCatching { LocalDate.parse(d.dateOfBirth) }.getOrNull()
            when {
                dateOfBirth == null -> errors["dateOfBirth"] = "Enter a valid date of birth"
                !dateOfBirth.isBefore(LocalDate.now(registrationZone)) -> {
                    errors["dateOfBirth"] = "Date of birth must be before today"
                }
            }
        }
        if (d.password.length < 12) errors["password"] = "Password must be at least 12 characters"
        if (d.password != d.passwordConfirmation) errors["passwordConfirmation"] = "Passwords do not match"
        if (!d.privacyAccepted) errors["privacy"] = "You must accept the Privacy Policy"
        if (!d.termsAccepted) errors["terms"] = "You must accept the Terms of Service"
        return errors
    }

    private fun registrationErrors(error: Throwable): Map<String, String> {
        val apiError = error as? ApiDomainError
        val fieldErrors = apiError?.fieldErrors.orEmpty()
            .mapNotNull { (field, messages) ->
                val uiField = registrationFieldKey(field)
                val message = messages.firstOrNull(String::isNotBlank)
                if (uiField == null || message == null) null else uiField to message
            }
            .toMap()

        if (fieldErrors.isNotEmpty()) return fieldErrors

        val message = if (apiError?.httpStatus == 429 || apiError?.code == "CONTACT_ALREADY_OWNED") {
            authErrorMessage(error, "Too many attempts. Please wait and try again.")
        } else {
            authErrorMessage(error, "We couldn't create your account. Please try again.")
        }
        return mapOf("_" to message)
    }

    private fun registrationFieldKey(field: String): String? = when (field) {
        "first_name", "firstName" -> "firstName"
        "middle_name", "middleName" -> "middleName"
        "last_name", "lastName" -> "lastName"
        "date_of_birth", "dateOfBirth" -> "dateOfBirth"
        "email" -> "email"
        "password" -> "password"
        "password_confirmation", "passwordConfirmation" -> "passwordConfirmation"
        "invitation_code", "invitationCode" -> "invitationCode"
        "privacy_policy_version", "privacy" -> "privacy"
        "terms_version", "terms" -> "terms"
        else -> null
    }
}
