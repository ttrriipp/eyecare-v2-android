package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- Contacts ---

@Serializable
data class ContactDto(
    val id: Int,
    val type: String,
    @SerialName("masked_value") val maskedValue: String,
    @SerialName("is_primary") val isPrimary: Boolean = false,
    @SerialName("verified_at") val verifiedAt: String? = null,
)

@Serializable
data class ContactsResponse(val data: List<ContactDto>)

@Serializable
data class ContactResponse(val data: ContactDto)

// --- Contact OTP ---

@Serializable
data class ContactOtpRequest(
    @SerialName("contact_type") val contactType: String,
    @SerialName("contact_value") val contactValue: String,
)

// --- Contact Verify ---

@Serializable
data class ContactVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
)

// --- Step-up ---

@Serializable
data class StepUpOtpRequest(
    val purpose: String,
)

@Serializable
data class StepUpOtpResponse(val data: StepUpOtpData)

@Serializable
data class StepUpOtpData(
    @SerialName("challenge_id") val challengeId: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("contact_type") val contactType: String,
    @SerialName("masked_contact") val maskedContact: String,
)

@Serializable
data class StepUpVerifyRequest(
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
)

@Serializable
data class StepUpVerifyResponse(val data: StepUpVerifyData)

@Serializable
data class StepUpVerifyData(
    @SerialName("step_up_token") val stepUpToken: String,
    @SerialName("expires_in") val expiresIn: Int,
)

// --- Password Change ---

@Serializable
data class PasswordChangeRequest(
    @SerialName("current_password") val currentPassword: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class PasswordChangeResponse(val data: PasswordChangeData)

@Serializable
data class PasswordChangeData(val message: String? = null)

// --- Link State ---

@Serializable
data class LinkStateResponse(val data: LinkStateData)

@Serializable
data class LinkStateData(
    val status: String,
    @SerialName("linked_at") val linkedAt: String? = null,
    @SerialName("request_submitted_at") val requestSubmittedAt: String? = null,
)

@Serializable
data class PatientLinkRequestResponse(val data: PatientLinkRequestData)

@Serializable
data class PatientLinkRequestData(
    @SerialName("request_number") val requestNumber: String,
    val status: String,
    @SerialName("submitted_at") val submittedAt: String,
    @SerialName("reviewed_at") val reviewedAt: String? = null,
)

// --- Invitation ---

@Serializable
data class InvitationOtpRequest(
    @SerialName("invitation_code") val invitationCode: String,
)

@Serializable
data class InvitationAcceptRequest(
    @SerialName("invitation_code") val invitationCode: String,
    @SerialName("challenge_id") val challengeId: String,
    val code: String,
)

@Serializable
data class InvitationAcceptResponse(val data: InvitationAcceptData)

@Serializable
data class InvitationAcceptData(
    val status: String,
    @SerialName("linked_at") val linkedAt: String? = null,
)
