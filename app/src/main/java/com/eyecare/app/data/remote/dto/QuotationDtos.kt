package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal

object QuotationDtos {

    @Serializable
    data class QuotationItemDto(
        val description: String,
        val quantity: Int,
        @SerialName("unit_price") val unitPrice: Double,
        val amount: Double,
    )

    @Serializable
    data class QuotationRevisionDto(
        @SerialName("revision_number") val revisionNumber: Int,
        val subtotal: Double,
        @SerialName("discount_amount") val discountAmount: Double,
        val total: Double,
        val items: List<QuotationItemDto> = emptyList(),
    )

    @Serializable
    data class QuotationDto(
        val id: Int,
        @SerialName("quotation_number") val quotationNumber: String? = null,
        val status: String,
        @SerialName("valid_until") val validUntil: String? = null,
        val notes: String? = null,
        val revision: QuotationRevisionDto? = null,
        @SerialName("created_at") val createdAt: String? = null,
    )

    @Serializable
    data class QuotationListResponse(
        val data: List<QuotationDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class QuotationResponse(val data: QuotationDto)
}
