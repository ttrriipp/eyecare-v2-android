package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.PrescriptionApiService
import com.eyecare.app.data.remote.dto.PrescriptionDtos
import com.eyecare.app.domain.model.EyeMeasurement
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.PrescriptionMeasurementGroup
import com.eyecare.app.domain.model.PrescriptionMeasurements
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.PrescriptionRepository
import javax.inject.Inject

class PrescriptionRepositoryImpl @Inject constructor(
    private val api: PrescriptionApiService,
) : PrescriptionRepository {

    override suspend fun getPrescriptions(page: Int): Result<PaginatedResult<Prescription>> = runCatching {
        val response = api.getPrescriptions(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getPrescription(id: Int): Result<Prescription> = runCatching {
        api.getPrescription(id).data.toDomain()
    }

    private fun PrescriptionDtos.PrescriptionDto.toDomain() = Prescription(
        id = id,
        appointmentId = appointmentId,
        previousPrescriptionId = previousPrescriptionId,
        isCurrent = isCurrent,
        date = date,
        measurements = measurements.toDomain(),
        remarks = remarks,
    )

    private fun PrescriptionDtos.MeasurementsDto.toDomain() = PrescriptionMeasurements(
        main = main.toDomain(),
        add = add.toDomain(),
    )

    private fun PrescriptionDtos.MeasurementGroupDto.toDomain() = PrescriptionMeasurementGroup(
        od = od.toDomain(),
        os = os.toDomain(),
    )

    private fun PrescriptionDtos.EyeMeasurementDto.toDomain() = EyeMeasurement(
        value = value,
        sphere = sphere,
        cylinder = cylinder,
    )
}
