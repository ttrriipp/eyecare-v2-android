package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.data.remote.dto.AppointmentDtos
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AssignedOptometrist
import com.eyecare.app.domain.model.VisitReason
import com.eyecare.app.domain.repository.AppointmentRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class AppointmentRepositoryImpl @Inject constructor(
    private val api: AppointmentApiService,
    private val json: Json,
) : AppointmentRepository {

    override suspend fun getAppointments(): Result<List<Appointment>> = runCatching {
        api.getAppointments().data.map { it.toDomain() }
    }

    override suspend fun getAppointment(id: Int): Result<Appointment> = runCatching {
        api.getAppointment(id).data.toDomain()
    }

    override suspend fun getAppointmentAvailability(
        date: String,
        visitReasonId: Int,
        appointmentId: Int?,
    ): Result<AppointmentAvailability> = runCatching {
        api.getAppointmentAvailability(date, visitReasonId, appointmentId).data.toDomain()
    }

    override suspend fun createAppointment(
        visitReasonId: Int,
        scheduledAt: String,
        contactNotes: String?,
    ): Result<Appointment> = runCatching {
        api.createAppointment(
            AppointmentDtos.CreateAppointmentRequest(visitReasonId, scheduledAt, contactNotes)
        ).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<AppointmentDtos.ValidationErrorBody>(body)
            throw AppointmentError.ValidationError(parsed.errors, parsed.code)
        }
        throw throwable
    }

    override suspend fun cancelAppointment(id: Int): Result<Appointment> = runCatching {
        api.cancelAppointment(id).data.toDomain()
    }

    override suspend fun rescheduleAppointment(id: Int, scheduledAt: String): Result<Appointment> = runCatching {
        api.rescheduleAppointment(id, AppointmentDtos.RescheduleRequest(scheduledAt)).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<AppointmentDtos.ValidationErrorBody>(body)
            throw AppointmentError.ValidationError(parsed.errors, parsed.code)
        }
        throw throwable
    }

    override suspend fun getVisitReasons(): Result<List<VisitReason>> = runCatching {
        api.getVisitReasons().data.map { VisitReason(it.id, it.name, it.durationMinutes) }
    }

    private fun AppointmentDtos.AppointmentDto.toDomain() = Appointment(
        id = id,
        visitReason = visitReason,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        staffNotes = staffNotes,
        appointmentNumber = appointmentNumber,
        source = source,
        assignedOptometrist = (assignedOptometrist ?: legacyAssignedStaff)?.let {
            AssignedOptometrist(it.id, it.name)
        },
    )

    private fun AppointmentDtos.AppointmentAvailabilityDto.toDomain() = AppointmentAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        visitReasonId = visitReasonId,
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
