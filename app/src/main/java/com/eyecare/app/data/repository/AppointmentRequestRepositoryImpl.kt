package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.eyecare.app.data.remote.dto.AppointmentRequestDto
import com.eyecare.app.data.remote.dto.AppointmentRequestIdentityDto
import com.eyecare.app.data.remote.dto.AvailabilitySlotDto
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class AppointmentRequestRepositoryImpl @Inject constructor(
    private val api: AppointmentRequestApiService,
) : AppointmentRequestRepository {

    override suspend fun getAvailability(date: String): Result<AppointmentRequestAvailability> = safeApiCall {
        api.getAvailability(date).data.toDomain()
    }

    override suspend fun getRequests(page: Int, perPage: Int): Result<PaginatedResult<AppointmentRequest>> = safeApiCall {
        val response = api.getRequests(page, perPage)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: page,
            lastPage = response.meta?.lastPage ?: page,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun createRequest(
        scheduledAt: String,
        reasonForVisit: String,
        identity: AppointmentRequestIdentity?,
    ): Result<AppointmentRequest> = safeApiCall {
        api.createRequest(
            CreateAppointmentRequest(
                scheduledAt = scheduledAt,
                reasonForVisit = reasonForVisit,
                identity = identity?.toDto(),
            ),
        ).data.toDomain()
    }

    override suspend fun getRequest(id: Int): Result<AppointmentRequest> = safeApiCall {
        api.getRequest(id).data.toDomain()
    }

    override suspend fun cancelRequest(id: Int): Result<AppointmentRequest> = safeApiCall {
        api.cancelRequest(id).data.toDomain()
    }

    private fun AppointmentRequestDto.toDomain() = AppointmentRequest(
        id = id,
        requestNumber = requestNumber,
        status = AppointmentRequestStatus.fromRaw(status),
        patientId = patientId,
        scheduledAt = scheduledAt,
        reasonForVisit = reasonForVisit,
        expiresAt = expiresAt,
        cancelledAt = cancelledAt,
        createdAt = createdAt,
        appointmentId = appointment?.id,
    )

    private fun com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityData.toDomain() = AppointmentRequestAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        slotDurationMinutes = slotDurationMinutes,
        dayStatus = dayStatus,
        generatedAt = generatedAt,
        slots = slots.map { it.toDomain() },
    )

    private fun AvailabilitySlotDto.toDomain() = AvailabilitySlot(
        startsAt = startsAt,
        endsAt = endsAt,
        available = available,
        reason = reason,
    )

    private fun AppointmentRequestIdentity.toDto() = AppointmentRequestIdentityDto(
        phone = phone,
        email = email,
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        dateOfBirth = dateOfBirth,
        gender = gender?.apiValue,
        occupation = occupation,
        address = address,
    )
}
