package com.eyecare.app.domain.model

data class AssignedStaff(val id: Int, val name: String)

data class Appointment(
    val id: Int,
    val visitReason: String,
    val status: AppointmentStatus,
    val scheduledAt: String,
    val contactNotes: String?,
    val staffNotes: String?,
    val assignedStaff: AssignedStaff? = null,
)

enum class AppointmentStatus {
    PENDING, CONFIRMED, RESCHEDULED, CANCELLED, COMPLETED;

    companion object {
        fun from(value: String): AppointmentStatus = when (value.lowercase()) {
            "confirmed" -> CONFIRMED
            "rescheduled" -> RESCHEDULED
            "cancelled" -> CANCELLED
            "completed" -> COMPLETED
            else -> PENDING
        }
    }
}
