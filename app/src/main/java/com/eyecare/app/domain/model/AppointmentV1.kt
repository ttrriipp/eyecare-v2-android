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
    SCHEDULED,
    CHECKED_IN,
    FULFILLED,
    CANCELLED,
    NO_SHOW,
    UNKNOWN;

    val canCancel: Boolean
        get() = this == SCHEDULED || this == CHECKED_IN

    val canReschedule: Boolean
        get() = this == SCHEDULED

    val canLeaveFeedback: Boolean
        get() = this == FULFILLED

    val isActive: Boolean
        get() = this == SCHEDULED || this == CHECKED_IN

    val isTerminal: Boolean
        get() = this == FULFILLED || this == CANCELLED || this == NO_SHOW || this == UNKNOWN

    val patientLabel: String
        get() = when (this) {
            SCHEDULED -> "Scheduled"
            CHECKED_IN -> "Checked in"
            FULFILLED -> "Completed"
            CANCELLED -> "Cancelled"
            NO_SHOW -> "No show"
            UNKNOWN -> "Unknown"
        }

    companion object {
        fun from(value: String): AppointmentStatus = when (value.lowercase().trim()) {
            "scheduled" -> SCHEDULED
            "checked_in" -> CHECKED_IN
            "fulfilled" -> FULFILLED
            "cancelled" -> CANCELLED
            "no_show" -> NO_SHOW
            else -> UNKNOWN
        }
    }
}
