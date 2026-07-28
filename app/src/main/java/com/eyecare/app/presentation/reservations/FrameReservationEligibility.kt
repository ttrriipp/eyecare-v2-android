package com.eyecare.app.presentation.reservations

import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

/**
 * Pure, testable eligibility policy for reservation-linked appointments.
 * An eligible appointment must:
 * - have status SCHEDULED
 * - have a parseable scheduled_at as an offset timestamp
 * - have non-negative duration
 * - have an end instant (scheduled_at + duration) that is at or after [now]
 */
internal fun isReservationEligible(
    appointment: AppointmentV1,
    now: Instant,
): Boolean {
    if (appointment.status != AppointmentStatus.SCHEDULED) return false
    if (appointment.durationMinutes < 0) return false

    val scheduledInstant = try {
        OffsetDateTime.parse(appointment.scheduledAt).toInstant()
    } catch (_: DateTimeParseException) {
        return false
    }

    val endInstant = scheduledInstant.plusSeconds(appointment.durationMinutes * 60L)
    return !endInstant.isBefore(now)
}
