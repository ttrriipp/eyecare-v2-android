package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentV1

interface AppointmentV1Repository {
    suspend fun getAppointments(page: Int = 1): Result<PaginatedResult<AppointmentV1>>
    suspend fun getAppointment(id: Int): Result<AppointmentV1>
    suspend fun getAppointmentAvailability(
        date: String,
        appointmentId: Int? = null,
    ): Result<AppointmentAvailability>
    suspend fun cancelAppointment(id: Int): Result<AppointmentV1>
    suspend fun rescheduleAppointment(id: Int, scheduledAt: String): Result<AppointmentV1>
}

data class PaginatedResult<T>(
    val data: List<T>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int,
) {
    val hasMorePages: Boolean get() = currentPage < lastPage
}
