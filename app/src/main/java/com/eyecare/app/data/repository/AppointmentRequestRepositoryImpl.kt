package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.eyecare.app.data.remote.dto.AppointmentRequestIdentityDto
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import com.eyecare.app.data.remote.dto.CancellationReasonRequest
import com.eyecare.app.data.remote.dto.UpdateAppointmentRequestScheduleRequest
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.CurrentAppointmentJourney
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
            bookingEligibility = response.meta?.bookingEligibility?.toDomain(),
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

    override suspend fun createRebookingRequest(
        appointmentId: Int,
        scheduledAt: String,
        alternativeScheduledTimes: List<String>?,
        reasonForVisit: String?,
    ): Result<AppointmentRequest> = safeApiCall {
        api.createRequest(
            CreateAppointmentRequest(
                appointmentId = appointmentId,
                scheduledAt = scheduledAt,
                alternativeScheduledTimes = alternativeScheduledTimes,
                reasonForVisit = reasonForVisit,
            ),
        ).data.toDomain()
    }

    override suspend fun getRequest(id: Int): Result<AppointmentRequest> = safeApiCall {
        api.getRequest(id).data.toDomain()
    }

    override suspend fun updateRequestSchedule(
        id: Int,
        scheduledAt: String,
        alternativeScheduledTimes: List<String>?,
    ): Result<AppointmentRequest> = safeApiCall {
        api.updateRequestSchedule(
            id = id,
            request = UpdateAppointmentRequestScheduleRequest(
                scheduledAt = scheduledAt,
                alternativeScheduledTimes = alternativeScheduledTimes,
            ),
        ).data.toDomain()
    }

    override suspend fun cancelRequest(id: Int, reasonDetails: String): Result<AppointmentRequest> = safeApiCall {
        api.cancelRequest(id, CancellationReasonRequest(reasonDetails)).data.toDomain()
    }

    override suspend fun getCurrentAppointmentJourney(): Result<CurrentAppointmentJourney> = safeApiCall {
        val dto = api.getCurrentAppointmentJourney().data
        when (dto.kind) {
            "none" -> CurrentAppointmentJourney.None
            "pending_request" -> {
                val request = dto.request
                    ?: throw IllegalStateException("pending_request kind missing required request field")
                CurrentAppointmentJourney.PendingRequest(request = request.toDomain())
            }
            "appointment" -> {
                val appointment = dto.appointment
                    ?: throw IllegalStateException("appointment kind missing required appointment field")
                CurrentAppointmentJourney.Appointment(
                    appointment = appointment.toDomain(),
                    originalRequest = dto.originalRequest?.toDomain(),
                    pendingReschedule = dto.pendingReschedule?.toDomain(),
                )
            }
            else -> throw IllegalStateException("Unknown current journey kind: ${dto.kind}")
        }
    }

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
