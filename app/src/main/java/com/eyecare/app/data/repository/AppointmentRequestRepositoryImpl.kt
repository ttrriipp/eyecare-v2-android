package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.eyecare.app.data.remote.dto.AppointmentRequestDto
import com.eyecare.app.data.remote.dto.AppointmentRequestIdentityDto
import com.eyecare.app.data.remote.dto.AppointmentRequestTypeSummaryDto
import com.eyecare.app.data.remote.dto.AvailabilitySlotDto
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityData
import com.eyecare.app.data.remote.dto.AppointmentTypeDto
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestTypeSummary
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class AppointmentRequestRepositoryImpl @Inject constructor(
    private val api: AppointmentRequestApiService,
) : AppointmentRequestRepository {

    override suspend fun getAppointmentTypes(): Result<List<AppointmentType>> = safeApiCall {
        api.getAppointmentTypes().data.map { it.toDomain() }
    }

    override suspend fun getAvailability(date: String, appointmentTypeId: Int): Result<AppointmentRequestAvailability> = safeApiCall {
        api.getAvailability(date, appointmentTypeId).data.toDomain()
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
        appointmentTypeId: Int,
        scheduledAt: String,
        reasonForVisit: String,
        alternativeScheduledTimes: List<String>?,
        referringSource: String?,
        identity: AppointmentRequestIdentity?,
    ): Result<AppointmentRequest> = safeApiCall {
        api.createRequest(
            CreateAppointmentRequest(
                appointmentTypeId = appointmentTypeId,
                scheduledAt = scheduledAt,
                alternativeScheduledTimes = alternativeScheduledTimes,
                reasonForVisit = reasonForVisit,
                referringSource = referringSource,
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

    private fun AppointmentTypeDto.toDomain() = AppointmentType(
        id = id,
        name = name,
        description = description,
        durationMinutes = durationMinutes,
        requiresReferral = requiresReferral,
    )

    private fun AppointmentRequestDto.toDomain() = AppointmentRequest(
        id = id,
        requestNumber = requestNumber,
        status = AppointmentRequestStatus.fromRaw(status),
        patientId = patientId,
        appointmentType = appointmentType?.toDomain(),
        scheduledAt = scheduledAt,
        alternativeScheduledTimes = alternativeScheduledTimes ?: emptyList(),
        provisionalDurationMinutes = provisionalDurationMinutes,
        reasonForVisit = reasonForVisit,
        referringSource = referringSource,
        timePreferencesAreReserved = timePreferencesAreReserved,
        expiresAt = expiresAt,
        cancelledAt = cancelledAt,
        rejectionReason = rejectionReason,
        createdAt = createdAt,
        appointmentId = appointment?.id,
    )

    private fun AppointmentRequestTypeSummaryDto.toDomain() = AppointmentRequestTypeSummary(
        id = id,
        name = name,
        durationMinutes = durationMinutes,
    )

    private fun AppointmentRequestAvailabilityData.toDomain() = AppointmentRequestAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        slotDurationMinutes = slotDurationMinutes,
        visitDurationMinutes = visitDurationMinutes,
        appointmentTypeId = appointmentTypeId,
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
