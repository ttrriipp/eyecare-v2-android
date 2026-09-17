package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatAppointmentTitle(visitReason: String): String = visitReason
    .replace("_", " ")
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word ->
        word.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
    }
    .ifBlank { "Appointment" }

internal fun formatAppointmentDate(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    return parsed?.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        ?: runCatching {
            LocalDate.parse(scheduledAt.take(10))
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        }.getOrDefault("Date TBD")
}

internal fun formatAppointmentTime(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    val fallback = scheduledAt.drop(11).take(5)
    return parsed?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        ?: fallback.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "Time TBD"
}

private fun parseAppointmentDateTime(value: String): LocalDateTime? =
    runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(CLINIC_TIME_ZONE).toLocalDateTime()
    }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value.replace(" ", "T").removeSuffix("Z")) }.getOrNull()
