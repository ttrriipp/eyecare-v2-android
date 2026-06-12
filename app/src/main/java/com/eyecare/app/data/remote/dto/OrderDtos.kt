package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object OrderDtos {

    @Serializable
    data class OrderDto(
        val id: Int,
        @SerialName("order_number") val orderNumber: String,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("is_non_prescription") val isNonPrescription: Boolean,
        val status: String,
        val subtotal: String,
        @SerialName("total_amount") val totalAmount: String,
        val items: List<OrderItemDto> = emptyList(),
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    data class OrderItemDto(
        val id: Int,
        @SerialName("product_variant_id") val productVariantId: Int,
        @SerialName("lens_type_id") val lensTypeId: Int,
        @SerialName("product_id") val productId: Int,
        @SerialName("product_name") val productName: String,
        @SerialName("variant_name") val variantName: String,
        @SerialName("variant_sku") val variantSku: String,
        @SerialName("lens_type_name") val lensTypeName: String,
        @SerialName("unit_price") val unitPrice: String,
        val quantity: Int,
        val subtotal: String,
    )

    @Serializable
    data class OrderListResponse(val data: List<OrderDto>)

    @Serializable
    data class OrderResponse(val data: OrderDto)

    @Serializable
    data class CreateOrderRequest(
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("is_non_prescription") val isNonPrescription: Boolean,
        val items: List<OrderItemRequest>,
    )

    @Serializable
    data class OrderItemRequest(
        @SerialName("product_variant_id") val productVariantId: Int,
        @SerialName("lens_type_id") val lensTypeId: Int,
        val quantity: Int,
    )

    @Serializable
    data class ValidationErrorBody(
        val message: String,
        val errors: Map<String, List<String>> = emptyMap(),
    )
}
