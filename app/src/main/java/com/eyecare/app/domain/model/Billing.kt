package com.eyecare.app.domain.model

data class Billing(
    val id: Int,
    val orderId: Int,
    val status: BillingStatus,
    val totalAmount: String,
    val amountPaid: String,
    val balanceDue: String,
    val issuedAt: String?,
    val createdAt: String,
    val payments: List<Payment>,
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
    DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOIDED;

    companion object {
        fun from(value: String): BillingStatus = when (value.lowercase()) {
            "issued" -> ISSUED
            "partially_paid" -> PARTIALLY_PAID
            "paid" -> PAID
            "voided" -> VOIDED
            else -> DRAFT
        }
    }
}
