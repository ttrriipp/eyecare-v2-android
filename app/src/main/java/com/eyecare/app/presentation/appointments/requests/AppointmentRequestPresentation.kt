package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AppointmentRequestStatus

data class RequestStatusPresentation(
    val label: String,
    val description: String,
    val showCancel: Boolean = false,
    val showViewConfirmed: Boolean = false,
)

fun requestStatusPresentation(status: AppointmentRequestStatus): RequestStatusPresentation = when (status) {
    AppointmentRequestStatus.PENDING -> RequestStatusPresentation(
        label = "Awaiting clinic review",
        description = "The requested time is held while staff review it.",
        showCancel = true,
    )
    AppointmentRequestStatus.ACCEPTED -> RequestStatusPresentation(
        label = "Confirmed",
        description = "Staff accepted the request and created an appointment.",
        showViewConfirmed = true,
    )
    AppointmentRequestStatus.REJECTED -> RequestStatusPresentation(
        label = "Not approved",
        description = "The clinic could not approve this request.",
    )
    AppointmentRequestStatus.CANCELLED -> RequestStatusPresentation(
        label = "Cancelled",
        description = "The patient cancelled the request.",
    )
    AppointmentRequestStatus.EXPIRED -> RequestStatusPresentation(
        label = "Expired",
        description = "The request was not resolved before its hold expired.",
    )
    AppointmentRequestStatus.UNKNOWN -> RequestStatusPresentation(
        label = "Status unavailable",
        description = "The request status could not be determined.",
    )
}
