package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.PatientIntake

interface PatientIntakeRepository {
    suspend fun getIntake(appointmentId: Int): Result<PatientIntake?>
    suspend fun saveIntake(appointmentId: Int, request: SaveIntakeRequest): Result<PatientIntake>
    suspend fun submitIntake(appointmentId: Int): Result<PatientIntake>
}

data class SaveIntakeRequest(
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val occupation: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val chiefComplaint: String? = null,
    val pastOcularHistory: String? = null,
    val pastSurgicalHistory: String? = null,
    val pastMedicalHistory: String? = null,
    val allergies: String? = null,
    val medications: String? = null,
)
