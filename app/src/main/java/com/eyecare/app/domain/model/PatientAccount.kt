package com.eyecare.app.domain.model

data class PatientAccount(
    val id: Int,
    val name: String,
    val firstName: String?,
    val middleName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val role: String,
    val dateOfBirth: String?,
    val linkStatus: PatientLinkStatus,
    val privacyPolicyVersion: String?,
    val privacyAcceptedAt: String?,
    val linkedPatient: LinkedPatient?,
)

data class LinkedPatient(
    val patientNumber: String,
    val fullName: String,
    val dateOfBirth: String?,
    val gender: String?,
    val occupation: String?,
    val address: String?,
    val phone: String?,
    val contactEmail: String?,
)

enum class PatientLinkStatus {
    LINKED,
    PENDING_REVIEW,
    UNLINKED,
    UNKNOWN;

    companion object {
        fun fromRaw(value: String?): PatientLinkStatus = when (value?.lowercase()) {
            "linked" -> LINKED
            "pending_review" -> PENDING_REVIEW
            "unlinked" -> UNLINKED
            else -> UNKNOWN
        }
    }
}

enum class ContactType {
    EMAIL,
    PHONE;

    companion object {
        fun fromRaw(value: String): ContactType = when (value.lowercase()) {
            "email" -> EMAIL
            "phone" -> PHONE
            else -> EMAIL
        }
    }
}

data class OtpChallenge(
    val challengeId: String,
    val expiresAt: String,
)

data class RegistrationProof(
    val token: String,
    val expiresAt: String,
    val contactType: ContactType,
)

sealed interface LoginOutcome {
    data class OtpRequired(val challengeId: String, val expiresAt: String) : LoginOutcome
    data class Authenticated(val token: String, val account: PatientAccount) : LoginOutcome
}

data class AuthenticatedSession(
    val token: String,
    val account: PatientAccount,
)

data class PolicyMetadata(
    val privacyPolicyVersion: String,
    val privacyPolicyUrl: String,
    val privacyPolicyEffectiveDate: String?,
    val termsVersion: String,
    val termsUrl: String,
    val termsEffectiveDate: String?,
)

data class AccountContact(
    val id: Int,
    val type: ContactType,
    val maskedValue: String,
    val isPrimary: Boolean,
    val verifiedAt: String?,
)

sealed interface LinkState {
    data object Linked : LinkState
    data object PendingReview : LinkState
    data object Unlinked : LinkState
    data object Unknown : LinkState

    companion object {
        fun fromStatus(status: PatientLinkStatus): LinkState = when (status) {
            PatientLinkStatus.LINKED -> Linked
            PatientLinkStatus.PENDING_REVIEW -> PendingReview
            PatientLinkStatus.UNLINKED -> Unlinked
            PatientLinkStatus.UNKNOWN -> Unknown
        }
    }
}

data class StepUpChallenge(
    val challengeId: String,
    val expiresAt: String,
    val contactType: ContactType,
    val maskedContact: String,
)

data class StepUpProof(
    val token: String,
    val expiresIn: Int,
)
