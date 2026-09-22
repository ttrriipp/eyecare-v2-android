package com.eyecare.app.domain.model

import java.math.BigDecimal

data class AccessoryOrderRequest(
    val id: Int,
    val requestNumber: String,
    val status: OrderRequestStatus,
    val subtotalAmount: BigDecimal,
    val requestedDiscountType: DiscountType,
    val resolvedBy: Int?,
    val resolvedAt: String?,
    val items: List<AccessoryOrderRequestItem>,
    val rejectionReason: String?,
    val cancelledAt: String?,
    val createdAt: String,
    val order: AcceptedOrderSummary?,
)

data class AccessoryOrderRequestItem(
    val id: Int,
    val productVariantId: Int,
    val description: String,
    val quantity: Int,
    val unitPrice: BigDecimal,
    val amount: BigDecimal,
    val itemKind: String,
    val itemSnapshot: ItemSnapshot,
)

data class ItemSnapshot(
    val productName: String,
    val variantName: String,
    val attributes: Map<String, String>,
    val images: List<String> = emptyList(),
)

data class AcceptedOrderSummary(
    val id: Int,
    val orderNumber: String,
    val status: String,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paymentExpiresAt: String?,
)

enum class OrderRequestStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    UNKNOWN;

    companion object {
        fun from(value: String): OrderRequestStatus = when (value.lowercase()) {
            "pending" -> PENDING
            "accepted" -> ACCEPTED
            "rejected" -> REJECTED
            "cancelled" -> CANCELLED
            else -> UNKNOWN
        }
    }
}

enum class DiscountType {
    NONE,
    SENIOR_CITIZEN,
    PWD,
    UNKNOWN;

    companion object {
        fun from(value: String): DiscountType = when (value.lowercase()) {
            "none" -> NONE
            "senior_citizen" -> SENIOR_CITIZEN
            "pwd" -> PWD
            else -> UNKNOWN
        }
    }
}

enum class OrderRequestFilter(val apiValue: String, val label: String) {
    CURRENT("current", "Current"),
    HISTORY("history", "History"),
}
