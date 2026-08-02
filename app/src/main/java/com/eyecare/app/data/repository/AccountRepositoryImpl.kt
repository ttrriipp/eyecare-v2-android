package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.ApiErrorDecoder
import com.eyecare.app.data.remote.api.AccountApiService
import com.eyecare.app.data.remote.dto.ContactOtpRequest
import com.eyecare.app.data.remote.dto.ContactVerifyRequest
import com.eyecare.app.data.remote.dto.InvitationAcceptRequest
import com.eyecare.app.data.remote.dto.InvitationOtpRequest
import com.eyecare.app.data.remote.dto.PasswordChangeRequest
import com.eyecare.app.data.remote.dto.StepUpOtpRequest
import com.eyecare.app.data.remote.dto.StepUpVerifyRequest
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.domain.model.PatientLinkRequestStatus
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.model.StepUpProof
import com.eyecare.app.domain.repository.AccountRepository
import retrofit2.HttpException
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val api: AccountApiService,
) : AccountRepository {

    override suspend fun getContacts(): Result<List<AccountContact>> = safeApiCall {
        api.getContacts().data.map { dto ->
            AccountContact(
                id = dto.id,
                type = ContactType.fromRaw(dto.type),
                maskedValue = dto.maskedValue,
                isPrimary = dto.isPrimary,
                verifiedAt = dto.verifiedAt,
            )
        }
    }

    override suspend fun requestStepUpOtp(): Result<StepUpChallenge> = safeApiCall {
        val response = api.requestStepUpOtp(StepUpOtpRequest())
        StepUpChallenge(
            challengeId = response.data.challengeId,
            expiresAt = response.data.expiresAt,
            contactType = ContactType.fromRaw(response.data.contactType),
            maskedContact = response.data.maskedContact,
        )
    }

    override suspend fun verifyStepUpOtp(challengeId: String, code: String): Result<StepUpProof> = safeApiCall {
        val response = api.verifyStepUpOtp(StepUpVerifyRequest(challengeId = challengeId, code = code))
        StepUpProof(token = response.data.stepUpToken, expiresIn = response.data.expiresIn)
    }

    override suspend fun requestContactOtp(stepUpToken: String, contactType: String, contactValue: String): Result<OtpChallenge> =
        safeApiCall {
            val response = api.requestContactOtp(
                ContactOtpRequest(contactType = contactType, contactValue = contactValue),
                stepUpToken,
            )
            OtpChallenge(challengeId = response.data.challengeId, expiresAt = response.data.expiresAt)
        }

    override suspend fun verifyContactOtp(challengeId: String, code: String): Result<AccountContact> = safeApiCall {
        val response = api.verifyContactOtp(ContactVerifyRequest(challengeId = challengeId, code = code))
        val dto = response.data
        AccountContact(
            id = dto.id,
            type = ContactType.fromRaw(dto.type),
            maskedValue = dto.maskedValue,
            isPrimary = dto.isPrimary,
            verifiedAt = dto.verifiedAt,
        )
    }

    override suspend fun makeContactPrimary(stepUpToken: String, contactId: Int): Result<List<AccountContact>> =
        safeApiCall {
            api.makeContactPrimary(contactId, stepUpToken).data.map { dto ->
                AccountContact(
                    id = dto.id,
                    type = ContactType.fromRaw(dto.type),
                    maskedValue = dto.maskedValue,
                    isPrimary = dto.isPrimary,
                    verifiedAt = dto.verifiedAt,
                )
            }
        }

    override suspend fun removeContact(stepUpToken: String, contactId: Int): Result<Unit> = safeApiCall {
        api.removeContact(contactId, stepUpToken)
    }

    override suspend fun changePassword(
        stepUpToken: String,
        currentPassword: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String> = safeApiCall {
        val response = api.changePassword(
            PasswordChangeRequest(
                currentPassword = currentPassword,
                password = password,
                passwordConfirmation = passwordConfirmation,
            ),
            stepUpToken,
        )
        response.data.message ?: "Password changed successfully."
    }

    override suspend fun getLinkState(): Result<LinkState> = safeApiCall {
        val response = api.getLinkState()
        when (response.data.status) {
            "linked" -> LinkState.Linked
            "pending_review" -> LinkState.PendingReview
            "unlinked" -> LinkState.Unlinked
            else -> LinkState.Unknown
        }
    }

    override suspend fun submitPatientLinkRequest(): Result<PatientLinkRequest> = safeApiCall {
        api.submitPatientLinkRequest().data.toDomain()
    }

    override suspend fun getCurrentPatientLinkRequest(): Result<PatientLinkRequest?> = safeApiCall {
        val response = api.getCurrentPatientLinkRequest()
        if (!response.isSuccessful) throw HttpException(response)
        response.body()?.data?.toDomain()
    }

    override suspend fun requestInvitationOtp(invitationCode: String): Result<OtpChallenge> = safeApiCall {
        val response = api.requestInvitationOtp(InvitationOtpRequest(invitationCode = invitationCode.trim()))
        OtpChallenge(challengeId = response.data.challengeId, expiresAt = response.data.expiresAt)
    }

    override suspend fun acceptInvitation(invitationCode: String, challengeId: String, code: String): Result<LinkState> =
        safeApiCall {
            val response = api.acceptInvitation(
                InvitationAcceptRequest(
                    invitationCode = invitationCode.trim(),
                    challengeId = challengeId,
                    code = code,
                ),
            )
            when (response.data.status) {
                "linked" -> LinkState.Linked
                else -> LinkState.Unknown
            }
        }

    private suspend fun <T> safeApiCall(block: suspend () -> T): Result<T> = runCatching {
        block()
    }.recoverCatching { throwable ->
        throw mapError(throwable)
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

    private fun com.eyecare.app.data.remote.dto.PatientLinkRequestData.toDomain() = PatientLinkRequest(
        requestNumber = requestNumber,
        status = PatientLinkRequestStatus.fromRaw(status),
        submittedAt = submittedAt,
        reviewedAt = reviewedAt,
    )
}
