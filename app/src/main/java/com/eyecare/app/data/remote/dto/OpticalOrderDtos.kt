package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

object OpticalOrderDtos {

    @Serializable
    data class RatingSummaryDto(
        val rating: Int,
        val comment: String? = null,
        @SerialName("revision_number") val revisionNumber: Int? = null,
        @SerialName("created_at") val createdAt: String? = null,
    )

    @Serializable
    data class OpticalOrderItemDto(
        val id: Int,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price")
        @Serializable(with = MoneyValueSerializer::class)
        val unitPrice: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("product_variant_id") val productVariantId: Int? = null,
        @SerialName("is_rateable") val isRateable: Boolean = false,
        val rating: RatingSummaryDto? = null,
    )

    @Serializable
    data class QuotationReferenceDto(
        val id: Int,
        @SerialName("quotation_number") val quotationNumber: String,
    )

    @Serializable
    data class PaymentSummaryDto(
        val status: String,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("amount_paid") val amountPaid: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("balance_due") val balanceDue: BigDecimal,
        @SerialName("payment_due_date") val paymentDueDate: String? = null,
        @SerialName("is_overdue") val isOverdue: Boolean = false,
    )

    @Serializable
    data class OpticalOrderDto(
        val id: Int,
        @SerialName("order_number") val orderNumber: String,
        val status: String,
        @SerialName("fulfillment_mode") val fulfillmentMode: String,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal,
        @SerialName("started_at") val startedAt: String? = null,
        @SerialName("ready_at") val readyAt: String? = null,
        @SerialName("dispensed_at") val dispensedAt: String? = null,
        @SerialName("cancelled_at") val cancelledAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        @SerialName("source_quotation") val sourceQuotation: QuotationReferenceDto? = null,
        val items: List<OpticalOrderItemDto> = emptyList(),
        @SerialName("payment_summary") val paymentSummary: PaymentSummaryDto? = null,
    )

    @Serializable
    data class OpticalOrderListResponse(
        val data: List<OpticalOrderDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class OpticalOrderResponse(val data: OpticalOrderDto)

    @Serializable
    data class RatingRequest(
        val rating: Int,
        val comment: String? = null,
    )

    @Serializable
    data class RatingResultDto(
        val id: Int,
        @SerialName("item_id") val itemId: Int? = null,
        @SerialName("product_variant_id") val productVariantId: Int? = null,
        val rating: Int,
        val comment: String? = null,
        @SerialName("revision_number") val revisionNumber: Int? = null,
        @SerialName("created_at") val createdAt: String? = null,
    )
}
