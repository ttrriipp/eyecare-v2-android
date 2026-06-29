package com.eyecare.app.domain.model

data class Order(
    val id: Int,
    val orderNumber: String,
    val appointmentId: Int?,
    val billingId: Int?,
    val isNonPrescription: Boolean,
    val status: OrderStatus,
    val subtotal: String,
    val totalAmount: String,
    val items: List<OrderItem>,
    val createdAt: String,
)

data class OrderItem(
    val id: Int,
    val productVariantId: Int,
    val lensTypeId: Int?,
    val productId: Int,
    val productName: String,
    val variantName: String,
    val variantSku: String,
    val lensTypeName: String?,
    val unitPrice: String,
    val quantity: Int,
    val subtotal: String,
    val imageUrl: String?,
)

enum class OrderStatus {
    REQUESTED, CONFIRMED, PROCESSING, READY_FOR_PICKUP, COMPLETED, CANCELLED;

    companion object {
        fun from(value: String): OrderStatus = when (value.lowercase()) {
            "confirmed" -> CONFIRMED
            "processing" -> PROCESSING
            "ready_for_pickup" -> READY_FOR_PICKUP
            "completed" -> COMPLETED
            "cancelled" -> CANCELLED
            else -> REQUESTED
        }
    }
}
