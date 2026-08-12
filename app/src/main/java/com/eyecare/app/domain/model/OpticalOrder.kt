package com.eyecare.app.domain.model

import java.math.BigDecimal

data class OpticalOrder(
    val id: Int,
    val orderNumber: String,
    val status: OpticalOrderStatus,
    val fulfillmentMode: FulfillmentMode,
    val totalAmount: BigDecimal,
    val startedAt: String?,
    val readyAt: String?,
    val dispensedAt: String?,
    val cancelledAt: String?,
    val createdAt: String,
    val items: List<OpticalOrderItem>,
    val paymentSummary: PaymentSummary?,
)

data class OpticalOrderItem(
    val id: Int,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val amount: BigDecimal,
    val productVariantId: Int?,
    val isRateable: Boolean,
    val rating: RatingSummary?,
)

data class RatingSummary(
    val rating: Int,
    val comment: String?,
    val createdAt: String?,
)

data class PaymentSummary(
    val status: PaymentStatus,
    val totalAmount: BigDecimal,
    val amountPaid: BigDecimal,
    val balanceDue: BigDecimal,
    val paymentDueDate: String?,
    val isOverdue: Boolean,
)

data class RatingResult(
    val id: Int,
    val itemId: Int?,
    val rating: Int,
    val comment: String?,
)

enum class OpticalOrderStatus {
    QUEUED, IN_PROGRESS, READY_FOR_DISPENSING, DISPENSED, CANCELLED, UNKNOWN;

    companion object {
        fun from(value: String): OpticalOrderStatus = when (value.lowercase()) {
            "queued" -> QUEUED
            "in_progress" -> IN_PROGRESS
            "ready_for_dispensing" -> READY_FOR_DISPENSING
            "dispensed" -> DISPENSED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class FulfillmentMode {
    IMMEDIATE, PREPARED, UNKNOWN;

    companion object {
        fun from(value: String): FulfillmentMode = when (value.lowercase()) {
            "immediate" -> IMMEDIATE
            "prepared" -> PREPARED
            else -> UNKNOWN
        }
    }
}

enum class PaymentStatus {
    UNPAID, PARTIALLY_PAID, PAID, VOIDED, UNKNOWN;

    companion object {
        fun from(value: String): PaymentStatus = when (value.lowercase()) {
            "unpaid" -> UNPAID
            "partially_paid" -> PARTIALLY_PAID
            "paid" -> PAID
            "voided" -> VOIDED
            else -> UNKNOWN
        }
    }
}
