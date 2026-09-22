package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.math.BigDecimal

object AccessoryOrderRequestDtos {

    @Serializable
    data class ItemSnapshotDto(
        @SerialName("product_variant_id") val productVariantId: Int? = null,
        val sku: String? = null,
        @SerialName("product_name") val productName: String? = null,
        @SerialName("variant_name") val variantName: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val price: BigDecimal? = null,
        val attributes: JsonObject? = null,
        val images: List<String> = emptyList(),
    )

    @Serializable
    data class OrderRequestItemDto(
        val id: Int,
        @SerialName("product_variant_id") val productVariantId: Int,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price")
        @Serializable(with = MoneyValueSerializer::class)
        val unitPrice: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("item_kind") val itemKind: String,
        @SerialName("item_snapshot") val itemSnapshot: ItemSnapshotDto? = null,
        @SerialName("image_url") val imageUrl: String? = null,
    )

    @Serializable
    data class AcceptedOrderSummaryDto(
        val id: Int,
        @SerialName("order_number") val orderNumber: String,
        val status: String,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("discount_amount") val discountAmount: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal,
        @SerialName("payment_expires_at") val paymentExpiresAt: String? = null,
    )

    @Serializable
    data class OrderRequestDto(
        val id: Int,
        @SerialName("request_number") val requestNumber: String,
        val status: String,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("subtotal_amount") val subtotalAmount: BigDecimal,
        @SerialName("requested_discount_type") val requestedDiscountType: String,
        @SerialName("discount_proof_status") val discountProofStatus: String? = null,
        @SerialName("discount_proof_rejection_reason") val discountProofRejectionReason: String? = null,
        @SerialName("resolved_by") val resolvedBy: Int? = null,
        @SerialName("resolved_at") val resolvedAt: String? = null,
        val items: List<OrderRequestItemDto> = emptyList(),
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("cancelled_at") val cancelledAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val order: AcceptedOrderSummaryDto? = null,
    )

    @Serializable
    data class OrderRequestResponse(val data: OrderRequestDto)

    @Serializable
    data class DiscountProofResponse(
        val id: Int,
        val status: String,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    data class DiscountProofResultResponse(val data: DiscountProofResponse)

    @Serializable
    data class OrderRequestListResponse(
        val data: List<OrderRequestDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class SubmitOrderRequest(
        @SerialName("requested_discount_type") val requestedDiscountType: String,
        val items: List<SubmitOrderItem>,
    )

    @Serializable
    data class SubmitOrderItem(
        @SerialName("product_variant_id") val productVariantId: Int,
        val quantity: Int,
    )
}
