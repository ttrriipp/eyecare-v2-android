package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.data.remote.dto.AppointmentDtos
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AssignedStaff
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
            throw AppointmentError.ValidationError(parsed.errors)
        }
        throw throwable
    }

    override suspend fun cancelAppointment(id: Int): Result<Appointment> = runCatching {
        api.cancelAppointment(id).data.toDomain()
    }

    private fun AppointmentDtos.AppointmentDto.toDomain() = Appointment(
        id = id,
        visitReason = visitReason,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        staffNotes = staffNotes,
        assignedStaff = assignedStaff?.let { AssignedStaff(it.id, it.name) },
    )
}
