package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus

internal const val maxActiveAppointmentRequests = 1

internal fun activeAppointmentRequestCount(requests: List<AppointmentRequest>): Int =
    requests.count { it.status == AppointmentRequestStatus.PENDING }

internal fun hasReachedActiveAppointmentRequestLimit(requests: List<AppointmentRequest>): Boolean =
    activeAppointmentRequestCount(requests) >= maxActiveAppointmentRequests
