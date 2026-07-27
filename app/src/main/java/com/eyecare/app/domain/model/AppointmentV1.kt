package com.eyecare.app.domain.model

data class AppointmentV1(
    val id: Int,
    val appointmentNumber: String?,
    val appointmentType: String,
    val durationMinutes: Int,
    val referringSource: String?,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val contactNotes: String?,
    val lastRescheduleReason: String?,
    val source: String?,
    val assignedOptometrist: AssignedOptometrist?,
)
