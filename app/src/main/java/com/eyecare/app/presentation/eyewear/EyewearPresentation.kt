package com.eyecare.app.presentation.eyewear

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

fun formatPeso(amount: BigDecimal): String = pesoFormat.format(amount)

fun formatTimestamp(iso: String): String {
    return try {
        OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
    } catch (_: Exception) {
        iso.take(16)
    }
}

// Tracker step states
enum class TrackerStep { PREPARATION, READY, RELEASED }

data class TrackerState(
    val steps: List<Pair<TrackerStep, Boolean>>,
    val activeStep: TrackerStep?,
    val terminalMessage: String?,
)

// ── Order presentation ──────────────────────────────────────────────────

fun orderStatusLabel(status: OpticalOrderStatus): String = when (status) {
    OpticalOrderStatus.QUEUED -> "Preparing"
    OpticalOrderStatus.IN_PROGRESS -> "In preparation"
    OpticalOrderStatus.READY_FOR_DISPENSING -> "Ready for pickup"
    OpticalOrderStatus.DISPENSED -> "Released to you"
    OpticalOrderStatus.CANCELLED -> "Cancelled"
    OpticalOrderStatus.UNKNOWN -> "Status unavailable"
}

@Composable
fun orderStatusColor(status: OpticalOrderStatus): Color = when (status) {
    OpticalOrderStatus.QUEUED, OpticalOrderStatus.IN_PROGRESS -> EyecareColors.current.statusInfo
    OpticalOrderStatus.READY_FOR_DISPENSING -> EyecareColors.current.statusPending
    OpticalOrderStatus.DISPENSED -> MaterialTheme.colorScheme.tertiary
    OpticalOrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    // Neutral, not statusCancelled: an unrecognized status isn't a confirmed cancellation
    // and must not alarm the patient with the identical color (see orderStatusTextColor).
    OpticalOrderStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

// Contrast-safe label color for orderStatusColor's fill. The raw fill hues fail WCAG AA as
// text on their own 12%-tint background in light mode (see StatusPendingTextLight and
// friends in Color.kt), so pill/chip label text must route through the *Text tokens
// instead of reusing the fill color directly.
@Composable
fun orderStatusTextColor(status: OpticalOrderStatus): Color = when (status) {
    OpticalOrderStatus.QUEUED, OpticalOrderStatus.IN_PROGRESS -> EyecareColors.current.statusInfo
    OpticalOrderStatus.READY_FOR_DISPENSING -> EyecareColors.current.statusPendingText
    OpticalOrderStatus.DISPENSED -> EyecareColors.current.statusConfirmedText
    OpticalOrderStatus.CANCELLED -> EyecareColors.current.statusCancelledText
    OpticalOrderStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

fun paymentStatusLabel(status: PaymentStatus): String = when (status) {
    PaymentStatus.UNPAID -> "Payment due"
    PaymentStatus.PARTIALLY_PAID -> "Balance due"
    PaymentStatus.PAID -> "Paid"
    PaymentStatus.VOIDED -> "Payment voided"
    PaymentStatus.UNKNOWN -> "Payment status unavailable"
}

@Composable
fun paymentStatusColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.PAID -> MaterialTheme.colorScheme.tertiary
    PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.error
    PaymentStatus.VOIDED -> MaterialTheme.colorScheme.onSurfaceVariant
    // Amber, not error: an unrecognized payment status isn't a confirmed balance due and
    // must not collide with that alarm color (see paymentStatusTextColor).
    PaymentStatus.UNKNOWN -> EyecareColors.current.statusPending
}

@Composable
fun paymentStatusTextColor(status: PaymentStatus): Color = when (status) {
    PaymentStatus.PAID -> EyecareColors.current.statusConfirmedText
    PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID -> EyecareColors.current.statusCancelledText
    PaymentStatus.VOIDED -> MaterialTheme.colorScheme.onSurfaceVariant
    PaymentStatus.UNKNOWN -> EyecareColors.current.statusPendingText
}

fun orderCardTitle(order: OpticalOrder): String {
    val items = order.items
    if (items.isEmpty()) return "Eyewear order"
    val first = items[0].description
    return if (items.size == 1) first else "$first and ${items.size - 1} more"
}

fun orderDateLabel(order: OpticalOrder): Pair<String, String> {
    val ts = order.dispensedAt ?: order.readyAt ?: order.startedAt ?: order.createdAt
    val label = when {
        order.dispensedAt != null -> "Released"
        order.readyAt != null -> "Ready"
        order.startedAt != null -> "Started"
        else -> "Created"
    }
    return label to formatTimestamp(ts)
}

fun computeOrderTracker(status: OpticalOrderStatus): TrackerState {
    return when (status) {
        OpticalOrderStatus.QUEUED -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.PREPARATION,
            terminalMessage = null,
        )
        OpticalOrderStatus.IN_PROGRESS -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.PREPARATION,
            terminalMessage = null,
        )
        OpticalOrderStatus.READY_FOR_DISPENSING -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to true,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.READY,
            terminalMessage = null,
        )
        OpticalOrderStatus.DISPENSED -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to true,
                TrackerStep.RELEASED to true,
            ),
            activeStep = null,
            terminalMessage = "Released to you",
        )
        OpticalOrderStatus.CANCELLED -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Cancelled",
        )
        OpticalOrderStatus.UNKNOWN -> TrackerState(
            steps = listOf(
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Status unavailable",
        )
    }
}
