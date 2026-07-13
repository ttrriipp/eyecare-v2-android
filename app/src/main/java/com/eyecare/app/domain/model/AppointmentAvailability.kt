package com.eyecare.app.domain.model

data class AppointmentAvailability(
    val date: String,
    val timezone: String,
    val intervalMinutes: Int,
    val visitReasonId: Int,
    val visitDurationMinutes: Int,
    val optometristId: Int?,
    val appointmentId: Int?,
    val dayStatus: String,
    val generatedAt: String,
    val slots: List<AppointmentSlot>,
)

data class AppointmentSlot(
    val startsAt: String,
    val endsAt: String,
    val available: Boolean,
    val reason: String?,
)
