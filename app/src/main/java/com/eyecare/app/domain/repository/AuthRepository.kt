package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.model.RegistrationProof
import com.eyecare.app.domain.model.User

interface AuthRepository {
    suspend fun getPolicies(): Result<PolicyMetadata>
    suspend fun requestRegistrationOtp(contactType: String, contactValue: String): Result<OtpChallenge>
    suspend fun verifyRegistrationOtp(challengeId: String, code: String): Result<RegistrationProof>
    suspend fun register(
        registrationToken: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        dateOfBirth: String,
        password: String,
        passwordConfirmation: String,
        privacyPolicyVersion: String,
        termsVersion: String,
        invitationCode: String?,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession>
    suspend fun beginLogin(contactValue: String, password: String, deviceName: String?, installationId: String?): Result<LoginOutcome>
    suspend fun verifyLogin(challengeId: String, code: String, installationId: String?): Result<AuthenticatedSession>
    suspend fun requestPasswordRecoveryOtp(contactValue: String): Result<OtpChallenge>
    suspend fun recoverPassword(
        challengeId: String,
        code: String,
        password: String,
        passwordConfirmation: String,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession>
    suspend fun getMe(): Result<PatientAccount>
    suspend fun updateAccountName(firstName: String, lastName: String): Result<PatientAccount>
    suspend fun logoutCurrent(): Result<Unit>
    suspend fun logoutAll(): Result<Unit>

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        passwordConfirmation: String,
    ): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun getMeLegacy(): Result<User>
    suspend fun updateMe(request: UpdateProfileRequest): Result<User>
}

data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val address: String? = null,
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val occupation: String? = null,
    val gender: String? = null,
    val contactEmail: String? = null,
)
