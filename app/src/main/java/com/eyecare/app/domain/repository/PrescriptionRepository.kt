package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Prescription

interface PrescriptionRepository {
    suspend fun getPrescriptions(page: Int = 1): Result<PaginatedResult<Prescription>>
    suspend fun getPrescription(id: Int): Result<Prescription>
}
