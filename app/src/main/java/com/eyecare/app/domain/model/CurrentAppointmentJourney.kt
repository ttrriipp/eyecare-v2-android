package com.eyecare.app.domain.model

sealed interface CurrentAppointmentJourney {
    data object None : CurrentAppointmentJourney

    data class PendingRequest(
        val request: AppointmentRequest,
    ) : CurrentAppointmentJourney

    data class Appointment(
        val appointment: AppointmentV1,
        val originalRequest: AppointmentRequest?,
        val pendingReschedule: AppointmentRequest?,
    ) : CurrentAppointmentJourney
}
