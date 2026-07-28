package com.eyecare.app.domain.model

import java.math.BigDecimal

data class BillingRecord(
    val id: Int,
    val billingRecordNumber: String,
    val jobOrderId: Int,
    val status: BillingRecordStatus,
    val totalAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val balanceDue: BigDecimal,
    val recordedAt: String?,
    val payments: List<BillingPayment>,
)

data class BillingPayment(
    val id: Int,
    val amount: BigDecimal,
    val paymentMethod: String,
    val referenceNumber: String?,
    val status: BillingPaymentStatus,
    val recordedAt: String?,
)

enum class BillingRecordStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID,
    VOIDED,
    UNKNOWN;

    companion object {
        fun from(value: String): BillingRecordStatus = when (value.lowercase()) {
            "unpaid" -> UNPAID
            "partially_paid" -> PARTIALLY_PAID
            "paid" -> PAID
            "voided" -> VOIDED
            else -> UNKNOWN
        }
    }

    val displayLabel: String
        get() = when (this) {
            UNPAID -> "Unpaid"
            PARTIALLY_PAID -> "Partially paid"
            PAID -> "Paid"
            VOIDED -> "Voided"
            UNKNOWN -> "Unknown"
        }
}

enum class BillingPaymentStatus {
    POSTED,
    UNKNOWN;

    companion object {
        fun from(value: String): BillingPaymentStatus = when (value.lowercase()) {
            "posted" -> POSTED
            else -> UNKNOWN
        }
    }
}
