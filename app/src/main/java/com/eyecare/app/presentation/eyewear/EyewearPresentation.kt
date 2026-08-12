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
    OpticalOrderStatus.UNKNOWN -> EyecareColors.current.statusCancelled
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
    PaymentStatus.UNKNOWN -> EyecareColors.current.statusCancelled
}

fun isEdited(revisionNumber: Int?): Boolean = (revisionNumber ?: 1) > 1

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
