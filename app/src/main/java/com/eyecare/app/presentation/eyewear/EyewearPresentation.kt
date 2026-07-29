package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.EyewearPaymentStatus
import com.eyecare.app.domain.model.EyewearProgress
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

fun formatPeso(amount: BigDecimal): String = pesoFormat.format(amount)

fun formatDateLabel(consultationAt: String?, createdAt: String): Pair<String, String> {
    return if (!consultationAt.isNullOrBlank()) {
        "Consultation" to formatTimestamp(consultationAt)
    } else {
        "Created" to formatTimestamp(createdAt)
    }
}

fun formatTimestamp(iso: String): String {
    return try {
        OffsetDateTime.parse(iso).format(DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
    } catch (_: Exception) {
        iso.take(16)
    }
}

fun progressLabel(progress: EyewearProgress): String = progress.patientLabel

fun paymentLabel(status: EyewearPaymentStatus?): String? {
    if (status == null) return null
    return status.patientLabel
}

fun shouldShowBalance(paymentStatus: EyewearPaymentStatus?, balanceDue: BigDecimal?): Boolean {
    return paymentStatus == EyewearPaymentStatus.BALANCE_DUE && balanceDue != null && balanceDue > BigDecimal.ZERO
}

fun isRatingEligible(progress: EyewearProgress, jobOrderItemId: Int?, productVariantId: Int?): Boolean {
    return progress == EyewearProgress.DISPENSED && jobOrderItemId != null && productVariantId != null
}

fun humanizePaymentMethod(method: String): String {
    return when (method.lowercase().trim()) {
        "cash" -> "Cash"
        "gcash" -> "GCash"
        "bank_transfer" -> "Bank Transfer"
        "credit_card", "card" -> "Credit Card"
        else -> method.replace("_", " ").replaceFirstChar { it.uppercase() }
    }
}

// Tracker step states
enum class TrackerStep { ESTIMATE, PREPARATION, READY, RELEASED }

data class TrackerState(
    val steps: List<Pair<TrackerStep, Boolean>>, // step to completed
    val activeStep: TrackerStep?,
    val terminalMessage: String?,
)

fun computeTracker(progress: EyewearProgress): TrackerState {
    return when (progress) {
        EyewearProgress.ESTIMATE_AVAILABLE -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.ESTIMATE,
            terminalMessage = null,
        )
        EyewearProgress.IN_PREPARATION -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.PREPARATION,
            terminalMessage = null,
        )
        EyewearProgress.READY_FOR_PICKUP -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to true,
                TrackerStep.RELEASED to false,
            ),
            activeStep = TrackerStep.READY,
            terminalMessage = null,
        )
        EyewearProgress.DISPENSED -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to true,
                TrackerStep.READY to true,
                TrackerStep.RELEASED to true,
            ),
            activeStep = null,
            terminalMessage = "Released to You",
        )
        EyewearProgress.ESTIMATE_DECLINED -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Estimate Declined",
        )
        EyewearProgress.ESTIMATE_EXPIRED -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to true,
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Estimate Expired",
        )
        EyewearProgress.CANCELLED -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to false,
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Cancelled",
        )
        EyewearProgress.UNKNOWN -> TrackerState(
            steps = listOf(
                TrackerStep.ESTIMATE to false,
                TrackerStep.PREPARATION to false,
                TrackerStep.READY to false,
                TrackerStep.RELEASED to false,
            ),
            activeStep = null,
            terminalMessage = "Status Unavailable",
        )
    }
}
