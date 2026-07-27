package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object PatientIntakeDtos {

    @Serializable
    data class PatientIntakeDto(
        val id: Int,
        @SerialName("patient_id") val patientId: Int,
        @SerialName("appointment_id") val appointmentId: Int,
        val status: String,
        @SerialName("appointment_type") val appointmentType: String? = null,
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("date_of_birth") val dateOfBirth: String? = null,
        val gender: String? = null,
        val occupation: String? = null,
        val address: String? = null,
        val phone: String? = null,
        val email: String? = null,
        @SerialName("chief_complaint") val chiefComplaint: String? = null,
        @SerialName("past_ocular_history") val pastOcularHistory: String? = null,
        @SerialName("past_surgical_history") val pastSurgicalHistory: String? = null,
        @SerialName("past_medical_history") val pastMedicalHistory: String? = null,
        val allergies: String? = null,
        val medications: String? = null,
        @SerialName("submitted_at") val submittedAt: String? = null,
        @SerialName("verified_at") val verifiedAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
    )

    @Serializable
    data class PatientIntakeResponse(val data: PatientIntakeDto? = null)

    @Serializable
    data class SaveIntakeRequest(
        @SerialName("full_name") val fullName: String? = null,
        @SerialName("date_of_birth") val dateOfBirth: String? = null,
        val gender: String? = null,
        val occupation: String? = null,
        val address: String? = null,
        val phone: String? = null,
        val email: String? = null,
        @SerialName("chief_complaint") val chiefComplaint: String? = null,
        @SerialName("past_ocular_history") val pastOcularHistory: String? = null,
        @SerialName("past_surgical_history") val pastSurgicalHistory: String? = null,
        @SerialName("past_medical_history") val pastMedicalHistory: String? = null,
        val allergies: String? = null,
        val medications: String? = null,
    )
}
