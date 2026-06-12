package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.PrescriptionApiService
import com.eyecare.app.data.remote.dto.PrescriptionDtos
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.PrescriptionRepository
import javax.inject.Inject

class PrescriptionRepositoryImpl @Inject constructor(
    private val api: PrescriptionApiService,
) : PrescriptionRepository {

    override suspend fun getPrescriptions(): Result<List<Prescription>> = runCatching {
        api.getPrescriptions().data.map { it.toDomain() }
    }

    override suspend fun getPrescription(id: Int): Result<Prescription> = runCatching {
        api.getPrescription(id).data.toDomain()
    }

    private fun PrescriptionDtos.PrescriptionDto.toDomain() = Prescription(
        id = id, appointmentId = appointmentId,
        odSphere = odSphere, odCylinder = odCylinder, odAxis = odAxis, odAdd = odAdd,
        osSphere = osSphere, osCylinder = osCylinder, osAxis = osAxis, osAdd = osAdd,
        pd = pd, prescribedAt = prescribedAt, expiresAt = expiresAt, notes = notes,
    )
}
