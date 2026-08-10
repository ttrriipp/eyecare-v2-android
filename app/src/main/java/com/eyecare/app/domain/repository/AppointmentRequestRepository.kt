package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentType

interface AppointmentRequestRepository {
    suspend fun getAppointmentTypes(): Result<List<AppointmentType>>
    suspend fun getAvailability(date: String, appointmentTypeId: Int): Result<AppointmentRequestAvailability>
    suspend fun getRequests(page: Int = 1, perPage: Int = 15): Result<PaginatedResult<AppointmentRequest>>
    suspend fun createRequest(
        appointmentTypeId: Int,
        scheduledAt: String,
        reasonForVisit: String,
        alternativeScheduledTimes: List<String>? = null,
        referringSource: String? = null,
        identity: AppointmentRequestIdentity? = null,
    ): Result<AppointmentRequest>
    suspend fun getRequest(id: Int): Result<AppointmentRequest>
    suspend fun cancelRequest(id: Int): Result<AppointmentRequest>
}
