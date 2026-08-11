package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * One home for every date and time string the request flow shows. Previously each screen kept
 * its own copy of these, which is how a mis-encoded en dash survived in one of them while the
 * neighbouring copy stayed correct.
 */
private val dayFormat = DateTimeFormatter.ofPattern("MMMM d, yyyy")
private val shortDayFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val weekdayFormat = DateTimeFormatter.ofPattern("EEEE")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

/** En dash, spaced, used between every start and end time in the flow. */
private const val TIME_RANGE_SEPARATOR = " – "
private const val DOT_SEPARATOR = " · "

fun formatRequestDate(dateStr: String): String = runCatching {
    LocalDate.parse(dateStr).format(dayFormat)
}.getOrDefault(dateStr)

fun formatRequestWeekday(dateStr: String): String = runCatching {
    LocalDate.parse(dateStr).format(weekdayFormat)
}.getOrDefault("")

fun formatTimeRange(startsAt: String, endsAt: String): String = runCatching {
    val start = Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE)
    val end = Instant.parse(endsAt).atZone(CLINIC_TIME_ZONE)
    start.format(timeFormat) + TIME_RANGE_SEPARATOR + end.format(timeFormat)
}.getOrDefault(startsAt + TIME_RANGE_SEPARATOR + endsAt)

/** Start time only — enough to identify a slot once the day is already established on screen. */
fun formatSlotStart(startsAt: String): String = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).format(timeFormat)
}.getOrDefault(startsAt)

fun formatSlotDuration(startsAt: String, endsAt: String): String = runCatching {
    val start = Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE)
    val end = Instant.parse(endsAt).atZone(CLINIC_TIME_ZONE)
    "${Duration.between(start, end).toMinutes()} min"
}.getOrDefault("")

/** Full day-and-time label, for summaries where the day is not otherwise visible. */
fun formatSlotWithDay(slot: AvailabilitySlot): String = runCatching {
    val start = Instant.parse(slot.startsAt).atZone(CLINIC_TIME_ZONE)
    start.format(shortDayFormat) + DOT_SEPARATOR + formatTimeRange(slot.startsAt, slot.endsAt)
}.getOrDefault(formatTimeRange(slot.startsAt, slot.endsAt))

fun parseSlotTime(startsAt: String): LocalTime? = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).toLocalTime()
}.getOrNull()

fun String.toDatePickerMillis(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDay(CLINIC_TIME_ZONE).toInstant().toEpochMilli()
}.getOrNull()
