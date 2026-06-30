package com.eyecare.app.domain.model

data class Billing(
    val id: Int,
    val billingNumber: String,
    val orNumber: String?,
    val status: BillingStatus,
    val subtotal: String,
    val discountAmount: String,
    val totalAmount: String,
    val amountPaid: String,
    val balanceDue: String,
    val issuedAt: String?,
    val createdAt: String,
    val items: List<BillingItem>,
    val payments: List<Payment>,
)

data class BillingItem(
    val id: Int,
    val type: String,
    val description: String,
    val quantity: Int,
    val unitPrice: String,
    val amount: String,
)

data class Payment(
    val id: Int,
    val amount: String,
    val status: String,
    val method: String,
    val referenceNumber: String?,
    val paidAt: String?,
)

enum class BillingStatus {
    ISSUED, PARTIALLY_PAID, PAID, VOIDED;

    companion object {
        fun from(value: String): BillingStatus = when (value.lowercase()) {
            "partially_paid" -> PARTIALLY_PAID
            "paid" -> PAID
            "voided" -> VOIDED
            else -> ISSUED
        }
    }
}
