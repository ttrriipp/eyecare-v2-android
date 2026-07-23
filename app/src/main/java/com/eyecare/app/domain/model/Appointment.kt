package com.eyecare.app.domain.model

data class AssignedOptometrist(val id: Int, val name: String)

data class Appointment(
    val id: Int,
    val visitReason: String,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val contactNotes: String?,
    val staffNotes: String?,
    val lastRescheduleReason: String? = null,
    val appointmentNumber: String? = null,
    val source: String? = null,
    val assignedOptometrist: AssignedOptometrist? = null,
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
