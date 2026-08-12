package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

object QuotationDtos {

    @Serializable
    data class QuotationItemDto(
        val id: Int,
        @SerialName("item_type") val itemType: String,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price")
        @Serializable(with = MoneyValueSerializer::class)
        val unitPrice: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("product_variant_id") val productVariantId: Int? = null,
        @SerialName("lens_category_id") val lensCategoryId: Int? = null,
        @SerialName("service_id") val serviceId: Int? = null,
    )

    @Serializable
    data class OpticalOrderReferenceDto(
        val id: Int,
        @SerialName("order_number") val orderNumber: String,
    )

    @Serializable
    data class QuotationDto(
        val id: Int,
        @SerialName("quotation_number") val quotationNumber: String,
        val status: String,
        @SerialName("valid_until") val validUntil: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val subtotal: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("discount_amount") val discountAmount: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val total: BigDecimal? = null,
        val notes: String? = null,
        @SerialName("created_at") val createdAt: String,
        @SerialName("presented_at") val presentedAt: String? = null,
        @SerialName("confirmed_at") val confirmedAt: String? = null,
        @SerialName("optical_order") val opticalOrder: OpticalOrderReferenceDto? = null,
        val items: List<QuotationItemDto> = emptyList(),
    )

    @Serializable
    data class QuotationListResponse(
        val data: List<QuotationDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class QuotationResponse(val data: QuotationDto)
}
