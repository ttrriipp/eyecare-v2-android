package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Registration ---

@Serializable
data class RegistrationOtpRequest(
    @SerialName("contact_type") val contactType: String,
    @SerialName("contact_value") val contactValue: String,
)

@Serializable
data class RegistrationOtpResponse(val data: RegistrationOtpData)

@Serializable
data class RegistrationOtpData(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("expires_at") val expiresAt: String,
)

@Serializable
data class RegistrationVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
)

@Serializable
data class RegistrationVerifyResponse(val data: RegistrationVerifyData)

@Serializable
data class RegistrationVerifyData(
    @SerialName("registration_token") val registrationToken: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("contact_type") val contactType: String,
)

@Serializable
data class RegisterRequest(
    @SerialName("registration_token") val registrationToken: String,
    @SerialName("first_name") val firstName: String,
    @SerialName("middle_name") val middleName: String? = null,
    @SerialName("last_name") val lastName: String,
    @SerialName("date_of_birth") val dateOfBirth: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("privacy_policy_version") val privacyPolicyVersion: String,
    @SerialName("terms_version") val termsVersion: String,
    @SerialName("invitation_code") val invitationCode: String? = null,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("installation_id") val installationId: String? = null,
)

// --- Login ---

@Serializable
data class LoginRequest(
    @SerialName("contact_value") val contactValue: String,
    val password: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("installation_id") val installationId: String? = null,
)

@Serializable
data class LoginResponse(val data: LoginData)

@Serializable
data class LoginData(
    @SerialName("step_up_required") val stepUpRequired: Boolean = false,
    @SerialName("challenge_id") val challengeId: String? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
    val token: String? = null,
    val user: PatientAccountDto? = null,
)

@Serializable
data class LoginVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
    @SerialName("installation_id") val installationId: String? = null,
)

// --- Password Recovery ---

@Serializable
data class PasswordRecoveryOtpRequest(
    @SerialName("contact_value") val contactValue: String,
)

@Serializable
data class PasswordRecoveryVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("device_name") val deviceName: String? = null,
    @SerialName("installation_id") val installationId: String? = null,
)

// --- Policies ---

@Serializable
data class PoliciesResponse(val data: PoliciesData)

@Serializable
data class PoliciesData(
    @SerialName("privacy_policy") val privacyPolicy: PolicyEntry,
    @SerialName("terms_of_service") val termsOfService: PolicyEntry,
)

@Serializable
data class PolicyEntry(
    val version: String,
    val url: String,
    @SerialName("effective_date") val effectiveDate: String? = null,
)

// --- Shared Auth Response (register, login-verify, recovery-verify) ---

@Serializable
data class AuthSessionResponse(val data: AuthSessionData)

@Serializable
data class AuthSessionData(
    val token: String,
    val user: PatientAccountDto,
)

// --- PatientAccount ---

@Serializable
data class PatientAccountDto(
    val id: Int,
    val name: String,
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("middle_name") val middleName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val role: String = "patient",
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    @SerialName("link_status") val linkStatus: String = "unlinked",
    @SerialName("privacy_policy_version") val privacyPolicyVersion: String? = null,
    @SerialName("privacy_accepted_at") val privacyAcceptedAt: String? = null,
    @SerialName("linked_patient") val linkedPatient: LinkedPatientDto? = null,
)

@Serializable
data class LinkedPatientDto(
    @SerialName("patient_number") val patientNumber: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val address: String? = null,
    val phone: String? = null,
    @SerialName("contact_email") val contactEmail: String? = null,
)

// --- Me / Update ---

@Serializable
data class MeResponse(val data: PatientAccountDto)

@Serializable
data class UpdateMeRequest(
    @SerialName("first_name") val firstName: String? = null,
    @SerialName("last_name") val lastName: String? = null,
)

// --- Logout ---

@Serializable
data class LogoutAllResponse(val data: LogoutAllData)

@Serializable
data class LogoutAllData(val message: String? = null)
