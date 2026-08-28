package com.eyecare.app.data.repository

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.ApiErrorDecoder
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.dto.AuthSessionResponse
import com.eyecare.app.data.remote.dto.LinkedPatientDto
import com.eyecare.app.data.remote.dto.LoginRequest
import com.eyecare.app.data.remote.dto.LoginResponse
import com.eyecare.app.data.remote.dto.LoginVerifyRequest
import com.eyecare.app.data.remote.dto.PasswordRecoveryOtpRequest
import com.eyecare.app.data.remote.dto.PasswordRecoveryVerifyRequest
import com.eyecare.app.data.remote.dto.PatientAccountDto
import com.eyecare.app.data.remote.dto.RegisterRequest
import com.eyecare.app.data.remote.dto.RegistrationOtpRequest
import com.eyecare.app.data.remote.dto.RegistrationVerifyRequest
import com.eyecare.app.domain.model.AccountProfilePatch
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.LinkedPatient
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.model.RegistrationProof
import com.eyecare.app.domain.model.toPhilippineE164
import com.eyecare.app.domain.repository.AuthRepository
import retrofit2.HttpException
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApiService,
    private val tokenManager: TokenManager,
    private val deviceIdentityProvider: DeviceIdentityProvider,
) : AuthRepository {

    override suspend fun getPolicies(): Result<PolicyMetadata> = safeApiCall {
        val response = api.getPolicies()
        PolicyMetadata(
            privacyPolicyVersion = response.data.privacyPolicy.version,
            privacyPolicyUrl = response.data.privacyPolicy.url,
            privacyPolicyEffectiveDate = response.data.privacyPolicy.effectiveDate,
            termsVersion = response.data.termsOfService.version,
            termsUrl = response.data.termsOfService.url,
            termsEffectiveDate = response.data.termsOfService.effectiveDate,
        )
    }

    override suspend fun requestRegistrationOtp(phone: String): Result<OtpChallenge> =
        safeApiCall {
            val response = api.requestRegistrationOtp(
                RegistrationOtpRequest(contactType = "phone", contactValue = toPhilippineE164(phone)),
            )
            OtpChallenge(challengeId = response.data.challengeId, expiresAt = response.data.expiresAt)
        }

    override suspend fun verifyRegistrationOtp(challengeId: String, code: String): Result<RegistrationProof> =
        safeApiCall {
            val response = api.verifyRegistrationOtp(
                RegistrationVerifyRequest(challengeId = challengeId, code = code),
            )
            RegistrationProof(
                token = response.data.registrationToken,
                expiresAt = response.data.expiresAt,
                contactType = ContactType.fromRaw(response.data.contactType),
            )
        }

    override suspend fun register(
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
    ): Result<AuthenticatedSession> = safeApiCallWithToken {
        api.register(
            RegisterRequest(
                registrationToken = registrationToken,
                firstName = firstName,
                middleName = middleName,
                lastName = lastName,
                dateOfBirth = dateOfBirth,
                email = email?.trim()?.ifBlank { null },
                password = password,
                passwordConfirmation = passwordConfirmation,
                privacyPolicyVersion = privacyPolicyVersion,
                termsVersion = termsVersion,
                invitationCode = invitationCode?.trim()?.ifBlank { null },
                deviceName = deviceName ?: deviceIdentityProvider.deviceName(),
                installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
            ),
        )
    }

    override suspend fun beginLogin(
        phone: String,
        password: String,
        deviceName: String?,
        installationId: String?,
    ): Result<LoginOutcome> = safeApiCall {
        val response = api.login(
            LoginRequest(
                contactValue = toPhilippineE164(phone),
                password = password,
                deviceName = deviceName ?: deviceIdentityProvider.deviceName(),
                installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
            ),
        )
        val outcome = mapLoginResponse(response)
        if (outcome is LoginOutcome.Authenticated) {
            tokenManager.saveToken(outcome.token)
        }
        outcome
    }

    override suspend fun verifyLogin(
        challengeId: String,
        code: String,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession> =
        safeApiCallWithToken {
            api.verifyLogin(
                LoginVerifyRequest(
                    challengeId = challengeId,
                    code = code,
                    deviceName = deviceName ?: deviceIdentityProvider.deviceName(),
                    installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
                ),
            )
        }

    override suspend fun requestPasswordRecoveryOtp(phone: String): Result<OtpChallenge> =
        safeApiCall {
            val response = api.requestPasswordRecoveryOtp(
                PasswordRecoveryOtpRequest(contactValue = toPhilippineE164(phone)),
            )
            OtpChallenge(challengeId = response.data.challengeId, expiresAt = response.data.expiresAt)
        }

    override suspend fun recoverPassword(
        challengeId: String,
        code: String,
        password: String,
        passwordConfirmation: String,
        deviceName: String?,
        installationId: String?,
    ): Result<AuthenticatedSession> = safeApiCallWithToken {
        api.verifyPasswordRecovery(
            PasswordRecoveryVerifyRequest(
                challengeId = challengeId,
                code = code,
                password = password,
                passwordConfirmation = passwordConfirmation,
                deviceName = deviceName ?: deviceIdentityProvider.deviceName(),
                installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
            ),
        )
    }

    override suspend fun getMe(): Result<PatientAccount> = safeApiCall {
        api.getMe().data.toDomain()
    }

    override suspend fun updateAccountProfile(patch: AccountProfilePatch, stepUpToken: String?): Result<PatientAccount> =
        safeApiCall {
            api.updateProfile(patch, stepUpToken).data.toDomain()
        }

    override suspend fun logoutCurrent(): Result<Unit> = runCatching {
        try {
            api.logout()
        } finally {
            tokenManager.clearToken()
        }
    }

    override suspend fun logoutAll(): Result<Unit> = runCatching {
        try {
            api.logoutAll()
        } finally {
            tokenManager.clearToken()
        }
    }

    private fun mapLoginResponse(response: LoginResponse): LoginOutcome {
        val data = response.data
        return if (data.stepUpRequired) {
            LoginOutcome.OtpRequired(
                challengeId = requireNotNull(data.challengeId),
                expiresAt = requireNotNull(data.expiresAt),
            )
        } else {
            LoginOutcome.Authenticated(
                token = requireNotNull(data.token),
                account = requireNotNull(data.user).toDomain(),
            )
        }
    }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = runCatching {
        block()
    }.recoverCatching { throwable ->
        throw mapError(throwable)
    }

    private suspend fun safeApiCallWithToken(block: suspend () -> AuthSessionResponse): Result<AuthenticatedSession> =
        safeApiCall {
            val response = block()
            tokenManager.saveToken(response.data.token)
            AuthenticatedSession(
                token = response.data.token,
                account = response.data.user.toDomain(),
            )
        }

    private fun mapError(throwable: Throwable): Throwable {
        if (throwable !is HttpException) return throwable
        val body = throwable.response()?.errorBody()?.string()
        val error = ApiErrorDecoder.decode(throwable.code(), body)
        return ApiDomainError(
            httpStatus = error.httpStatus,
            code = error.code,
            message = error.message,
            fieldErrors = error.fieldErrors,
        )
    }

    private fun PatientAccountDto.toDomain() = PatientAccount(
        id = id,
        name = name,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        email = email,
        phone = phone,
        role = role,
        dateOfBirth = dateOfBirth,
        linkStatus = PatientLinkStatus.fromRaw(linkStatus),
        privacyPolicyVersion = privacyPolicyVersion,
        privacyAcceptedAt = privacyAcceptedAt,
        linkedPatient = linkedPatient?.toDomain(),
    )

    private fun LinkedPatientDto.toDomain() = LinkedPatient(
        patientNumber = patientNumber,
        fullName = fullName,
        dateOfBirth = dateOfBirth,
        gender = gender,
        occupation = occupation,
        address = address,
        phone = phone,
        contactEmail = contactEmail,
    )
}
