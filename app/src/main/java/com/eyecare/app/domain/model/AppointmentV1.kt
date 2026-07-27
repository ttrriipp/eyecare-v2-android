package com.eyecare.app.domain.model

data class AssignedOptometrist(val name: String)

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

enum class AppointmentStatus {
    PENDING, CONFIRMED, ARRIVED, COMPLETED, NO_SHOW, CANCELLED;

    companion object {
        fun from(value: String): AppointmentStatus = when (value.lowercase()) {
            "confirmed" -> CONFIRMED
            "arrived" -> ARRIVED
            "cancelled" -> CANCELLED
            "completed" -> COMPLETED
            "no_show" -> NO_SHOW
            else -> PENDING
        }
    }
}
