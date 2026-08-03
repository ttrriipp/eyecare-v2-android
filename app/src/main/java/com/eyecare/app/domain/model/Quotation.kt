package com.eyecare.app.domain.model

import java.math.BigDecimal

data class Quotation(
    val id: Int,
    val quotationNumber: String,
    val status: QuotationStatus,
    val validUntil: String?,
    val subtotal: BigDecimal,
    val discountAmount: BigDecimal,
    val total: BigDecimal,
    val notes: String?,
    val createdAt: String,
    val presentedAt: String?,
    val confirmedAt: String?,
    val opticalOrder: OpticalOrderReference?,
    val items: List<QuotationItem>,
)

data class QuotationItem(
    val id: Int,
    val itemType: QuotationItemType,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val amount: BigDecimal,
)

data class OpticalOrderReference(
    val id: Int,
    val orderNumber: String,
)

enum class QuotationStatus {
    PRESENTED, ACCEPTED, DECLINED, EXPIRED, UNKNOWN;

    companion object {
        fun from(value: String): QuotationStatus = when (value.lowercase()) {
            "presented" -> PRESENTED
            "accepted" -> ACCEPTED
            "declined" -> DECLINED
            "expired" -> EXPIRED
            else -> UNKNOWN
        }
    }
}

enum class QuotationItemType {
    PRODUCT, SERVICE, UNKNOWN;

    companion object {
        fun from(value: String): QuotationItemType = when (value.lowercase()) {
            "product" -> PRODUCT
            "service" -> SERVICE
            else -> UNKNOWN
        }
    }
}
