package com.eyecare.app.presentation.reservations

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val pesoFormat = DecimalFormat("#,##0.00")
private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

fun reservationChipLabel(isHeld: Boolean): String =
    if (isHeld) "Set aside" else "Requested"

/** Fill color for the status pill/banner background - pair with [reservationStatusTextColor]
 * for label/icon color, never this value directly (see EyewearPresentation.kt's identical
 * fill-vs-text split for orders, which exists for the same WCAG AA reason). */
@Composable
fun reservationStatusColor(isHeld: Boolean): Color =
    if (isHeld) EyecareColors.current.statusConfirmed else EyecareColors.current.statusPending

/** Contrast-safe label/icon color for [reservationStatusColor]'s fill - the raw fill hues fail
 * WCAG AA as text on their own 12% tint in light mode. */
@Composable
fun reservationStatusTextColor(isHeld: Boolean): Color =
    if (isHeld) EyecareColors.current.statusConfirmedText else EyecareColors.current.statusPendingText

fun reservationExplanation(isHeld: Boolean, expiresAt: String?): String =
    if (isHeld) {
        val formatted = expiresAt?.let { formatReservationDateTime(it) } ?: ""
        if (formatted.isNotBlank()) "Set aside for your visit until $formatted."
        else "Set aside for your visit."
    } else {
        "Request sent \u2014 the clinic will set these aside before your visit."
    }

fun formatReservationPrice(amount: BigDecimal): String = "₱${pesoFormat.format(amount)}"

fun formatReservationDate(iso: String): String = withOffsetDateTime(iso) { it.format(dateFormat) }

fun formatReservationDateTime(iso: String): String =
    withOffsetDateTime(iso) { "${it.format(dateFormat)} at ${it.format(timeFormat)}" }

fun formatReservationSchedule(scheduledAt: String, durationMinutes: Int): String =
    withOffsetDateTime(scheduledAt) {
        "${it.format(dateFormat)} at ${it.format(timeFormat)} · ${durationMinutes}min"
    }

private inline fun withOffsetDateTime(iso: String, transform: (OffsetDateTime) -> String): String =
    try {
        transform(OffsetDateTime.parse(iso))
    } catch (_: Exception) {
        iso.take(16)
    }
