package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.Appointment

interface AppointmentRepository {
    suspend fun getAppointments(): Result<List<Appointment>>
    suspend fun getAppointment(id: Int): Result<Appointment>
    suspend fun createAppointment(
        visitReasonId: String,
        scheduledAt: String,
        contactNotes: String?,
    ): Result<Appointment>
}
