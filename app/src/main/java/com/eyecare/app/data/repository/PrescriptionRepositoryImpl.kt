package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.PrescriptionDao
import com.eyecare.app.data.local.entity.PrescriptionEntity
import com.eyecare.app.data.remote.api.PrescriptionApiService
import com.eyecare.app.data.remote.dto.PrescriptionDtos
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.PrescriptionRepository
import javax.inject.Inject

class PrescriptionRepositoryImpl @Inject constructor(
    private val api: PrescriptionApiService,
    private val prescriptionDao: PrescriptionDao,
) : PrescriptionRepository {

    override suspend fun getPrescriptions(): Result<List<Prescription>> {
        return try {
            val dtos = api.getPrescriptions().data
            val entities = dtos.map { it.toEntity() }
            prescriptionDao.clearAll()
            prescriptionDao.insertAll(entities)
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            val cached = prescriptionDao.getAll()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPrescription(id: Int): Result<Prescription> {
        return try {
            val dto = api.getPrescription(id).data
            prescriptionDao.insertAll(listOf(dto.toEntity()))
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            val cached = prescriptionDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    // ── DTO → Domain ──

    private fun PrescriptionDtos.PrescriptionDto.toDomain() = Prescription(
        id = id, appointmentId = appointmentId,
        odSphere = odSphere, odCylinder = odCylinder, odAxis = odAxis, odAdd = odAdd,
        osSphere = osSphere, osCylinder = osCylinder, osAxis = osAxis, osAdd = osAdd,
        pd = pd, prescribedAt = prescribedAt, expiresAt = expiresAt, notes = notes,
    )

    // ── DTO → Entity ──

    private fun PrescriptionDtos.PrescriptionDto.toEntity() = PrescriptionEntity(
        id = id, appointmentId = appointmentId,
        odSphere = odSphere, odCylinder = odCylinder, odAxis = odAxis, odAdd = odAdd,
        osSphere = osSphere, osCylinder = osCylinder, osAxis = osAxis, osAdd = osAdd,
        pd = pd, prescribedAt = prescribedAt, expiresAt = expiresAt, notes = notes,
    )

    // ── Entity → Domain ──

    private fun PrescriptionEntity.toDomain() = Prescription(
        id = id, appointmentId = appointmentId,
        odSphere = odSphere, odCylinder = odCylinder, odAxis = odAxis, odAdd = odAdd,
        osSphere = osSphere, osCylinder = osCylinder, osAxis = osAxis, osAdd = osAdd,
        pd = pd, prescribedAt = prescribedAt, expiresAt = expiresAt, notes = notes,
    )
}
