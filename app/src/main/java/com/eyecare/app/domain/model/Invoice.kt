package com.eyecare.app.domain.model

data class Invoice(
    val id: Int,
    val invoiceNumber: String?,
    val officialNumber: String?,
    val patientId: Int?,
    val jobOrderId: Int?,
    val encounterId: Int?,
    val status: InvoiceStatus,
    val saleType: String?,
    val soldToName: String?,
    val subtotal: Double?,
    val discountAmount: Double?,
    val taxAmount: Double?,
    val total: Double?,
    val amountPaid: Double?,
    val balanceDue: Double?,
    val notes: String?,
    val issuedAt: String?,
    val items: List<InvoiceItem>,
    val payments: List<InvoicePayment>,
)

data class InvoiceItem(
    val id: Int,
    val type: String?,
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double,
)

data class InvoicePayment(
    val id: Int,
    val amount: Double,
    val paymentMethod: String,
    val referenceNumber: String?,
    val status: String,
)

enum class InvoiceStatus {
    DRAFT, ISSUED, PARTIALLY_PAID, PAID, VOIDED;

    companion object {
        fun from(value: String): InvoiceStatus = when (value.lowercase()) {
            "draft" -> DRAFT
            "issued" -> ISSUED
            "partially_paid" -> PARTIALLY_PAID
            "paid" -> PAID
            "voided" -> VOIDED
            else -> DRAFT
        }
    }
}
