package com.eyecare.app.domain.model

data class PatientIntake(
    val id: Int,
    val patientId: Int,
    val appointmentId: Int,
    val status: IntakeStatus,
    val appointmentType: String?,
    val fullName: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val occupation: String?,
    val address: String?,
    val phone: String?,
    val email: String?,
    val chiefComplaint: String?,
    val pastOcularHistory: String?,
    val pastSurgicalHistory: String?,
    val pastMedicalHistory: String?,
    val allergies: String?,
    val medications: String?,
    val submittedAt: String?,
    val verifiedAt: String?,
)

enum class IntakeStatus {
    DRAFT, SUBMITTED, VERIFIED;

    companion object {
        fun from(value: String): IntakeStatus = when (value.lowercase()) {
            "draft" -> DRAFT
            "submitted" -> SUBMITTED
            "verified" -> VERIFIED
            else -> DRAFT
        }
    }
}
