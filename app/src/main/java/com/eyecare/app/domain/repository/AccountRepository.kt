package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.model.StepUpProof

interface AccountRepository {
    suspend fun getContacts(): Result<List<AccountContact>>
    suspend fun requestStepUpOtp(): Result<StepUpChallenge>
    suspend fun verifyStepUpOtp(challengeId: String, code: String): Result<StepUpProof>
    suspend fun requestContactOtp(stepUpToken: String, contactType: String, contactValue: String): Result<OtpChallenge>
    suspend fun verifyContactOtp(challengeId: String, code: String): Result<AccountContact>
    suspend fun makeContactPrimary(stepUpToken: String, contactId: Int): Result<List<AccountContact>>
    suspend fun removeContact(stepUpToken: String, contactId: Int): Result<Unit>
    suspend fun changePassword(
        stepUpToken: String,
        currentPassword: String,
        password: String,
        passwordConfirmation: String,
    ): Result<String>
    suspend fun getLinkState(): Result<LinkState>
    suspend fun submitPatientLinkRequest(): Result<PatientLinkRequest>
    suspend fun getCurrentPatientLinkRequest(): Result<PatientLinkRequest?>
    suspend fun requestInvitationOtp(invitationCode: String): Result<OtpChallenge>
    suspend fun acceptInvitation(invitationCode: String, challengeId: String, code: String): Result<LinkState>
}
