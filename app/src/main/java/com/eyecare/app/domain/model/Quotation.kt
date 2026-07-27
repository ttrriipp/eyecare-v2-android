package com.eyecare.app.domain.model

data class Quotation(
    val id: Int,
    val quotationNumber: String?,
    val status: QuotationStatus,
    val validUntil: String?,
    val notes: String?,
    val revision: QuotationRevision?,
    val createdAt: String?,
)

data class QuotationRevision(
    val revisionNumber: Int,
    val subtotal: Double,
    val discountAmount: Double,
    val total: Double,
    val items: List<QuotationItem>,
)

data class QuotationItem(
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double,
)

enum class QuotationStatus {
    DRAFT, PRESENTED, ACCEPTED, DECLINED, EXPIRED;

    companion object {
        fun from(value: String): QuotationStatus = when (value.lowercase()) {
            "draft" -> DRAFT
            "presented" -> PRESENTED
            "accepted" -> ACCEPTED
            "declined" -> DECLINED
            "expired" -> EXPIRED
            else -> DRAFT
        }
    }
}
