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
import com.eyecare.app.data.remote.dto.UpdateMeRequest
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
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.UpdateProfileRequest
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

    override suspend fun requestRegistrationOtp(contactType: String, contactValue: String): Result<OtpChallenge> =
        safeApiCall {
            val response = api.requestRegistrationOtp(
                RegistrationOtpRequest(contactType = contactType, contactValue = contactValue),
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
        contactValue: String,
        password: String,
        deviceName: String?,
        installationId: String?,
    ): Result<LoginOutcome> = safeApiCall {
        val response = api.login(
            LoginRequest(
                contactValue = contactValue,
                password = password,
                deviceName = deviceName ?: deviceIdentityProvider.deviceName(),
                installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
            ),
        )
        mapLoginResponse(response)
    }

    override suspend fun verifyLogin(challengeId: String, code: String, installationId: String?): Result<AuthenticatedSession> =
        safeApiCallWithToken {
            api.verifyLogin(
                LoginVerifyRequest(
                    challengeId = challengeId,
                    code = code,
                    installationId = installationId ?: deviceIdentityProvider.getOrCreateInstallationId(),
                ),
            )
        }

    override suspend fun requestPasswordRecoveryOtp(contactValue: String): Result<OtpChallenge> =
        safeApiCall {
            val response = api.requestPasswordRecoveryOtp(
                PasswordRecoveryOtpRequest(contactValue = contactValue),
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

    override suspend fun updateAccountName(firstName: String, lastName: String): Result<PatientAccount> =
        safeApiCall {
            api.updateMe(UpdateMeRequest(firstName = firstName, lastName = lastName)).data.toDomain()
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

    override suspend fun login(email: String, password: String): Result<User> = runCatching {
        val outcome = beginLogin(email, password, null, null).getOrThrow()
        when (outcome) {
            is LoginOutcome.Authenticated -> outcome.account.toLegacyUser()
            is LoginOutcome.OtpRequired -> throw IllegalStateException("OTP required")
        }
    }

    override suspend fun register(
        name: String,
        email: String,
        phone: String?,
        password: String,
        passwordConfirmation: String,
    ): Result<User> = runCatching { throw IllegalStateException("Use new two-stage registration") }

    override suspend fun logout(): Result<Unit> = logoutCurrent()

    override suspend fun getMeLegacy(): Result<User> = runCatching {
        getMe().getOrThrow().toLegacyUser()
    }

    override suspend fun updateMe(request: UpdateProfileRequest): Result<User> = runCatching {
        updateAccountName(request.name ?: "", "").getOrThrow().toLegacyUser()
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

    private fun PatientAccount.toLegacyUser() = User(
        id = id,
        name = name,
        email = email ?: "",
        phone = phone,
        role = role,
        patientNumber = linkedPatient?.patientNumber,
        fullName = linkedPatient?.fullName,
        dateOfBirth = dateOfBirth,
        occupation = linkedPatient?.occupation,
        address = linkedPatient?.address,
        gender = linkedPatient?.gender,
        contactEmail = linkedPatient?.contactEmail,
    )
}
