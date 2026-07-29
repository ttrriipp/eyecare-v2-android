package com.eyecare.app.domain.model

import java.math.BigDecimal

// ── Enums ─────────────────────────────────────────────────────────────

enum class EyewearProgress {
    ESTIMATE_AVAILABLE,
    IN_PREPARATION,
    READY_FOR_PICKUP,
    DISPENSED,
    ESTIMATE_DECLINED,
    ESTIMATE_EXPIRED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun fromApi(value: String): EyewearProgress = when (value.lowercase()) {
            "estimate_available" -> ESTIMATE_AVAILABLE
            "in_preparation" -> IN_PREPARATION
            "ready_for_pickup" -> READY_FOR_PICKUP
            "dispensed" -> DISPENSED
            "estimate_declined" -> ESTIMATE_DECLINED
            "estimate_expired" -> ESTIMATE_EXPIRED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }

    val isCurrent: Boolean
        get() = this == ESTIMATE_AVAILABLE || this == IN_PREPARATION || this == READY_FOR_PICKUP

    val patientLabel: String
        get() = when (this) {
            ESTIMATE_AVAILABLE -> "Estimate Available"
            IN_PREPARATION -> "In Preparation"
            READY_FOR_PICKUP -> "Ready for Pickup"
            DISPENSED -> "Dispensed"
            ESTIMATE_DECLINED -> "Estimate Declined"
            ESTIMATE_EXPIRED -> "Estimate Expired"
            CANCELLED -> "Cancelled"
            UNKNOWN -> "Status Unavailable"
        }
}

enum class EyewearPaymentStatus {
    BALANCE_DUE,
    PAID,
    UNKNOWN;

    companion object {
        fun fromApi(value: String): EyewearPaymentStatus = when (value.lowercase()) {
            "balance_due" -> BALANCE_DUE
            "paid" -> PAID
            else -> UNKNOWN
        }
    }

    val patientLabel: String
        get() = when (this) {
            BALANCE_DUE -> "Balance Due"
            PAID -> "Paid"
            UNKNOWN -> "Payment Status Unavailable"
        }
}

// ── Summary ───────────────────────────────────────────────────────────

data class EyewearSummary(
    val key: String,
    val description: String,
    val consultationAt: String?,
    val createdAt: String,
    val progress: EyewearProgress,
    val paymentStatus: EyewearPaymentStatus?,
    val totalAmount: BigDecimal,
    val balanceDue: BigDecimal?,
    val activityAt: String,
)

// ── Detail sections ───────────────────────────────────────────────────

data class EyewearItem(
    val id: Int?,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val amount: BigDecimal,
    val productVariantId: Int?,
)

data class EyewearEstimate(
    val quotationNumber: String?,
    val status: String?,
    val validUntil: String?,
    val subtotal: BigDecimal?,
    val discountAmount: BigDecimal?,
    val total: BigDecimal?,
    val items: List<EyewearItem>,
)

data class EyewearPreparation(
    val jobOrderNumber: String?,
    val status: String?,
    val totalAmount: BigDecimal?,
    val startedAt: String?,
    val readyAt: String?,
    val items: List<EyewearItem>,
)

data class EyewearDispensing(
    val status: String?,
    val readyAt: String?,
    val dispensedAt: String?,
)

data class EyewearPayment(
    val id: Int,
    val amount: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val recordedAt: String?,
)

data class EyewearPaymentSummary(
    val billingRecordNumber: String?,
    val status: String?,
    val totalAmount: BigDecimal?,
    val amountPaid: BigDecimal?,
    val balanceDue: BigDecimal?,
    val payments: List<EyewearPayment>,
)

data class EyewearDetail(
    val key: String,
    val description: String?,
    val consultationAt: String?,
    val createdAt: String,
    val progress: EyewearProgress,
    val paymentStatus: EyewearPaymentStatus?,
    val totalAmount: BigDecimal,
    val balanceDue: BigDecimal?,
    val activityAt: String,
    val estimate: EyewearEstimate?,
    val preparation: EyewearPreparation?,
    val dispensing: EyewearDispensing?,
    val paymentSummary: EyewearPaymentSummary?,
)
