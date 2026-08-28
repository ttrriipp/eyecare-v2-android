package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AccountProfilePatch
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.model.RegistrationProof

interface AuthRepository {
    suspend fun getPolicies(): Result<PolicyMetadata>
    suspend fun requestRegistrationOtp(phone: String): Result<OtpChallenge>
    suspend fun verifyRegistrationOtp(challengeId: String, code: String): Result<RegistrationProof>
    suspend fun register(
        registrationToken: String,
        firstName: String,
        middleName: String?,
        lastName: String,
        dateOfBirth: String,
        email: String?,
        password: String,
        passwordConfirmation: String,
        privacyPolicyVersion: String,
        termsVersion: String,
        invitationCode: String?,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession>
    suspend fun beginLogin(phone: String, password: String, deviceName: String?, installationId: String?): Result<LoginOutcome>
    suspend fun verifyLogin(
        challengeId: String,
        code: String,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession>
    suspend fun requestPasswordRecoveryOtp(phone: String): Result<OtpChallenge>
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
    suspend fun updateAccountProfile(patch: AccountProfilePatch, stepUpToken: String? = null): Result<PatientAccount>
    suspend fun logoutCurrent(): Result<Unit>
    suspend fun logoutAll(): Result<Unit>
}
