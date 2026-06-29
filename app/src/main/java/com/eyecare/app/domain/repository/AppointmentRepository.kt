package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.VisitReason

interface AppointmentRepository {
    suspend fun getAppointments(): Result<List<Appointment>>
    suspend fun getAppointment(id: Int): Result<Appointment>
    suspend fun createAppointment(
        visitReasonId: Int,
        scheduledAt: String,
        contactNotes: String?,
    ): Result<Appointment>
    suspend fun cancelAppointment(id: Int): Result<Appointment>
    suspend fun getVisitReasons(): Result<List<VisitReason>>
}
