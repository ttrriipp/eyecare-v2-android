package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentV1ApiService
import com.eyecare.app.data.remote.dto.ApiErrorBody
import com.eyecare.app.data.remote.dto.AppointmentV1Dtos
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AssignedOptometrist
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class AppointmentV1RepositoryImpl @Inject constructor(
    private val api: AppointmentV1ApiService,
    private val json: Json,
) : AppointmentV1Repository {

    override suspend fun getAppointments(page: Int): Result<PaginatedResult<AppointmentV1>> = runCatching {
        val response = api.getAppointments(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getAppointment(id: Int): Result<AppointmentV1> = runCatching {
        api.getAppointment(id).data.toDomain()
    }

    override suspend fun getAppointmentAvailability(
        date: String,
        appointmentId: Int?,
    ): Result<AppointmentAvailability> = runCatching {
        api.getAppointmentAvailability(date, appointmentId).data.toDomain()
    }

    override suspend fun cancelAppointment(id: Int): Result<AppointmentV1> = runCatching {
        api.cancelAppointment(id).data.toDomain()
    }

    override suspend fun rescheduleAppointment(id: Int, scheduledAt: String): Result<AppointmentV1> = runCatching {
        api.rescheduleAppointment(id, AppointmentV1Dtos.RescheduleRequest(scheduledAt)).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<ApiErrorBody>(body)
            throw AppointmentError.ValidationError(parsed.errors ?: emptyMap())
        }
        throw throwable
    }

    override suspend fun rateAppointment(id: Int, rating: Int, comment: String?): Result<VisitRating> = runCatching {
        val response = api.rateAppointment(id, AppointmentV1Dtos.VisitRatingRequest(rating = rating, comment = comment))
        response.data.toDomain()
    }.recoverCatching { throwable ->
        when {
            throwable is HttpException && throwable.code() == 404 ->
                throw AppointmentError.NotFound
            throwable is HttpException && throwable.code() == 422 -> {
                val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
                val parsed = json.decodeFromString<ApiErrorBody>(body)
                throw AppointmentError.ValidationError(parsed.errors ?: emptyMap())
            }
            else -> throw throwable
        }
    }

    private fun AppointmentV1Dtos.AppointmentDto.toDomain() = AppointmentV1(
        id = id,
        appointmentNumber = appointmentNumber,
        appointmentType = appointmentType,
        durationMinutes = durationMinutes,
        referringSource = referringSource,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        reasonForVisit = reasonForVisit,
        lastRescheduleReason = lastRescheduleReason,
        source = source,
        assignedOptometrist = assignedOptometrist?.let { AssignedOptometrist(name = it.name) },
        isRateable = isRateable,
        visitRating = rating?.toDomain(),
    )

    private fun AppointmentV1Dtos.VisitRatingDto.toDomain() = VisitRating(
        rating = rating,
        comment = comment,
        revisionNumber = revisionNumber,
        createdAt = createdAt,
    )

    private fun AppointmentV1Dtos.AppointmentAvailabilityDto.toDomain() = AppointmentAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        visitReasonId = appointmentTypeId,
        visitDurationMinutes = visitDurationMinutes,
        optometristId = optometristId,
        appointmentId = appointmentId,
        dayStatus = dayStatus,
        generatedAt = generatedAt,
        slots = slots.map { slot ->
            AppointmentSlot(
                startsAt = slot.startsAt,
                endsAt = slot.endsAt,
                available = slot.available,
                reason = slot.reason,
            )
        },
    )
}
