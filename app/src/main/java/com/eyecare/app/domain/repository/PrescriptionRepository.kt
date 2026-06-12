package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Prescription

interface PrescriptionRepository {
    suspend fun getPrescriptions(): Result<List<Prescription>>
    suspend fun getPrescription(id: Int): Result<Prescription>
}
