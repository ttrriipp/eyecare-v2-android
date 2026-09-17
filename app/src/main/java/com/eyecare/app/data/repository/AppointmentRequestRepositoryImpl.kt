package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.eyecare.app.data.remote.dto.AppointmentRequestIdentityDto
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import com.eyecare.app.data.remote.dto.CancellationReasonRequest
import com.eyecare.app.data.remote.dto.UpdateAppointmentRequestScheduleRequest
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AppointmentStatus
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
        api.getCurrentAppointmentJourney().data.toDomain()
    }

    private fun com.eyecare.app.data.remote.dto.CurrentAppointmentJourneyDto.toDomain(): CurrentAppointmentJourney {
        return when (kind) {
            "none" -> {
                require(request == null && appointment == null && originalRequest == null && pendingReschedule == null) {
                    "none journey must not contain variant data"
                }
                CurrentAppointmentJourney.None
            }

            "pending_request" -> {
                require(appointment == null && originalRequest == null && pendingReschedule == null) {
                    "pending_request journey contains appointment variant data"
                }
                val pendingRequest = requireNotNull(request) {
                    "pending_request kind missing required request field"
                }.toDomain()
                require(pendingRequest.requestType == AppointmentRequestType.NEW) {
                    "pending_request must contain a new appointment request"
                }
                require(pendingRequest.status == AppointmentRequestStatus.PENDING) {
                    "pending_request must contain a pending request"
                }
                require(pendingRequest.appointmentId == null) {
                    "pending_request must not reference an appointment"
                }
                CurrentAppointmentJourney.PendingRequest(request = pendingRequest)
            }

            "appointment" -> {
                require(request == null) {
                    "appointment journey must not contain a top-level request"
                }
                val confirmedAppointment = requireNotNull(appointment) {
                    "appointment kind missing required appointment field"
                }.toDomain()
                require(confirmedAppointment.status in setOf(AppointmentStatus.SCHEDULED, AppointmentStatus.CHECKED_IN)) {
                    "appointment journey must contain an active appointment"
                }

                val original = originalRequest?.toDomain()?.also {
                    require(it.requestType == AppointmentRequestType.NEW) {
                        "original_request must be a new appointment request"
                    }
                    require(it.status == AppointmentRequestStatus.ACCEPTED) {
                        "original_request must be accepted"
                    }
                    require(it.appointmentId == confirmedAppointment.id) {
                        "original_request must reference the confirmed appointment"
                    }
                }
                val pendingChange = pendingReschedule?.toDomain()?.also {
                    require(it.requestType == AppointmentRequestType.RESCHEDULE) {
                        "pending_reschedule must be a reschedule request"
                    }
                    require(it.status == AppointmentRequestStatus.PENDING) {
                        "pending_reschedule must be pending"
                    }
                    require(it.appointmentId == confirmedAppointment.id) {
                        "pending_reschedule must reference the confirmed appointment"
                    }
                }

                CurrentAppointmentJourney.Appointment(
                    appointment = confirmedAppointment,
                    originalRequest = original,
                    pendingReschedule = pendingChange,
                )
            }

            else -> throw IllegalStateException("Unknown current journey kind: $kind")
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
