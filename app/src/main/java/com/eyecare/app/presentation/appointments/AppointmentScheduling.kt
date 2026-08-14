package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val CLINIC_TIME_ZONE: ZoneId = ZoneId.of("Asia/Manila")

/** Days shown at once in a week-strip date picker (reschedule sheet, appointment requests). */
internal const val availabilityWeekLength = 7

/**
 * How a single date in a week strip looks before the patient commits to it. The clinic's
 * availability endpoint answers one date per call, so a week strip fetches its whole week in
 * parallel and caches the verdict per date.
 */
enum class DayAvailability { UNKNOWN, LOADING, OPEN, CLOSED, FULL }

internal fun formatClinicScheduledAt(date: String, time: String): String =
    LocalDate.parse(date)
        .atTime(LocalTime.parse(time))
        .atZone(CLINIC_TIME_ZONE)
        .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

internal fun parseClinicDateTime(value: String): LocalDateTime? =
    runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(CLINIC_TIME_ZONE).toLocalDateTime()
    }.getOrNull() ?: runCatching { LocalDateTime.parse(value) }.getOrNull()

internal fun nextClinicSlot(time: LocalTime): LocalTime {
    val extraMinute = if (time.second > 0 || time.nano > 0) 1 else 0
    val roundedMinutes = ((time.hour * 60 + time.minute + extraMinute + 14) / 15) * 15
    return LocalTime.of((roundedMinutes / 60).coerceAtMost(23), roundedMinutes % 60)
}

internal fun isBookableAppointmentTime(
    candidate: LocalDateTime,
    durationMinutes: Int,
    now: LocalDateTime,
): Boolean = candidate.isAfter(now) &&
    candidate.dayOfWeek != java.time.DayOfWeek.SUNDAY &&
    !candidate.toLocalTime().isBefore(LocalTime.of(9, 0)) &&
    !candidate.plusMinutes(durationMinutes.toLong()).isAfter(candidate.toLocalDate().atTime(17, 0))

internal fun earliestBookingTime(
    date: LocalDate,
    durationMinutes: Int,
    now: LocalDateTime,
): LocalTime? {
    if (date.isBefore(now.toLocalDate()) || date.dayOfWeek == java.time.DayOfWeek.SUNDAY) return null
    val candidateTime = if (date == now.toLocalDate()) {
        maxOf(LocalTime.of(9, 0), nextClinicSlot(now.toLocalTime()))
    } else {
        LocalTime.of(9, 0)
    }
    val candidate = date.atTime(candidateTime)
    return candidateTime.takeIf { isBookableAppointmentTime(candidate, durationMinutes, now) }
}

internal enum class RescheduleSelectionError { PAST, UNCHANGED }

internal fun validateRescheduleSelection(
    candidate: LocalDateTime,
    current: LocalDateTime,
    now: LocalDateTime,
): RescheduleSelectionError? = when {
    !candidate.isAfter(now) -> RescheduleSelectionError.PAST
    candidate == current -> RescheduleSelectionError.UNCHANGED
    else -> null
}
