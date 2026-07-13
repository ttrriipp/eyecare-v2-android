package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal val CLINIC_TIME_ZONE: ZoneId = ZoneId.of("Asia/Manila")

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
    val roundedMinutes = ((time.hour * 60 + time.minute + 14) / 15) * 15
    return LocalTime.of((roundedMinutes / 60).coerceAtMost(23), roundedMinutes % 60)
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
