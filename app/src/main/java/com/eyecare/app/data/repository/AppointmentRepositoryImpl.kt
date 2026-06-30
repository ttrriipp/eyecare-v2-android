package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.AppointmentDao
import com.eyecare.app.data.local.dao.VisitReasonDao
import com.eyecare.app.data.local.entity.AppointmentEntity
import com.eyecare.app.data.local.entity.VisitReasonEntity
import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.data.remote.dto.AppointmentDtos
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AssignedStaff
import com.eyecare.app.domain.model.VisitReason
import com.eyecare.app.domain.repository.AppointmentRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class AppointmentRepositoryImpl @Inject constructor(
    private val api: AppointmentApiService,
    private val json: Json,
    private val appointmentDao: AppointmentDao,
    private val visitReasonDao: VisitReasonDao,
) : AppointmentRepository {

    override suspend fun getAppointments(): Result<List<Appointment>> {
        return try {
            val dtos = api.getAppointments().data
            val entities = dtos.map { it.toEntity() }
            appointmentDao.clearAll()
            appointmentDao.insertAll(entities)
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            val cached = appointmentDao.getAll()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getAppointment(id: Int): Result<Appointment> {
        return try {
            val dto = api.getAppointment(id).data
            appointmentDao.insertAll(listOf(dto.toEntity()))
            Result.success(dto.toDomain())
        } catch (e: Exception) {
            val cached = appointmentDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomain())
            } else {
                Result.failure(e)
            }
        }
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

    override suspend fun getVisitReasons(): Result<List<VisitReason>> {
        return try {
            val dtos = api.getVisitReasons().data
            val entities = dtos.map { it.toEntity() }
            visitReasonDao.clearAll()
            visitReasonDao.insertAll(entities)
            Result.success(dtos.map { VisitReason(it.id, it.name, it.durationMinutes) })
        } catch (e: Exception) {
            val cached = visitReasonDao.getAll()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toDomain() })
            } else {
                Result.failure(e)
            }
        }
    }

    // ── DTO → Domain ──

    private fun AppointmentDtos.AppointmentDto.toDomain() = Appointment(
        id = id,
        visitReason = visitReason,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        staffNotes = staffNotes,
        assignedStaff = assignedStaff?.let { AssignedStaff(it.id, it.name) },
    )

    // ── DTO → Entity ──

    private fun AppointmentDtos.AppointmentDto.toEntity() = AppointmentEntity(
        id = id,
        visitReason = visitReason,
        status = status,
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        staffNotes = staffNotes,
        assignedStaffJson = assignedStaff?.let {
            json.encodeToString(AppointmentDtos.AssignedStaffDto.serializer(), it)
        },
    )

    private fun AppointmentDtos.VisitReasonDto.toEntity() = VisitReasonEntity(
        id = id,
        name = name,
        durationMinutes = durationMinutes,
    )

    // ── Entity → Domain ──

    private fun AppointmentEntity.toDomain() = Appointment(
        id = id,
        visitReason = visitReason,
        status = AppointmentStatus.from(status),
        scheduledAt = scheduledAt,
        contactNotes = contactNotes,
        staffNotes = staffNotes,
        assignedStaff = assignedStaffJson?.let {
            val dto = json.decodeFromString<AppointmentDtos.AssignedStaffDto>(it)
            AssignedStaff(dto.id, dto.name)
        },
    )

    private fun VisitReasonEntity.toDomain() = VisitReason(
        id = id,
        name = name,
        durationMinutes = durationMinutes,
    )
}
