package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability

interface AppointmentRequestRepository {
    suspend fun getAvailability(date: String): Result<AppointmentRequestAvailability>
    suspend fun getRequests(page: Int = 1, perPage: Int = 15): Result<PaginatedResult<AppointmentRequest>>
    suspend fun createRequest(scheduledAt: String, reasonForVisit: String): Result<AppointmentRequest>
    suspend fun getRequest(id: Int): Result<AppointmentRequest>
    suspend fun cancelRequest(id: Int): Result<AppointmentRequest>
}
