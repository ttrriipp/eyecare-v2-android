package com.eyecare.app.presentation.reservations

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.DecimalFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

private val pesoFormat = DecimalFormat("#,##0.00")
private val dateFormat = DateTimeFormatter.ofPattern("MMM d, yyyy")
private val timeFormat = DateTimeFormatter.ofPattern("h:mm a")

fun reservationStatusLabel(status: ReservationStatus): String = when (status) {
    ReservationStatus.REQUESTED -> "Requested"
    ReservationStatus.PREPARED -> "Prepared"
    ReservationStatus.TRIED_ON -> "Tried on"
    ReservationStatus.CONVERTED -> "Converted"
    ReservationStatus.RELEASED -> "Released"
    ReservationStatus.CANCELLED -> "Cancelled"
    ReservationStatus.UNKNOWN -> "Unknown"
}

/**
 * Patient-facing explanation of what the clinic is doing with the reservation right now.
 */
fun reservationStatusExplanation(status: ReservationStatus): String = when (status) {
    ReservationStatus.REQUESTED -> "The clinic has your request and will pull these frames before your visit."
    ReservationStatus.PREPARED -> "Your frames are set aside and waiting for you at the clinic."
    ReservationStatus.TRIED_ON -> "You've tried these frames on. The clinic can prepare an estimate whenever you're ready."
    ReservationStatus.CONVERTED -> "These frames moved into an eyewear order. Track it under My Eyewear."
    ReservationStatus.RELEASED -> "The hold ended and these frames went back to the display."
    ReservationStatus.CANCELLED -> "This reservation was cancelled. The frames are no longer held for you."
    ReservationStatus.UNKNOWN -> "Ask the clinic for the latest status of this reservation."
}

@Composable
fun reservationStatusColor(status: ReservationStatus): Color {
    val colors = EyecareColors.current
    return when (status) {
        ReservationStatus.REQUESTED -> colors.statusPending
        ReservationStatus.PREPARED -> colors.statusConfirmed
        ReservationStatus.TRIED_ON -> colors.statusInfo
        ReservationStatus.CONVERTED -> colors.statusConfirmed
        ReservationStatus.RELEASED -> MaterialTheme.colorScheme.onSurfaceVariant
        ReservationStatus.CANCELLED -> colors.statusCancelled
        ReservationStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
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
