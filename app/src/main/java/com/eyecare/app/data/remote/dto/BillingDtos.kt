package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object BillingDtos {

    @Serializable
    data class BillingDto(
        val id: Int,
        @SerialName("billing_number") val billingNumber: String,
        val status: String,
        val subtotal: String,
        @SerialName("discount_amount") val discountAmount: String,
        @SerialName("total_amount") val totalAmount: String,
        @SerialName("amount_paid") val amountPaid: String,
        @SerialName("balance_due") val balanceDue: String,
        @SerialName("issued_at") val issuedAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val items: List<BillingItemDto> = emptyList(),
        val payments: List<PaymentDto> = emptyList(),
    )

    @Serializable
    data class BillingItemDto(
        val id: Int,
        val type: String,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price") val unitPrice: String,
        val amount: String,
    )

    @Serializable
    data class PaymentDto(
        val id: Int,
        val amount: String,
        val status: String,
        val method: String,
        @SerialName("reference_number") val referenceNumber: String? = null,
        @SerialName("paid_at") val paidAt: String? = null,
    )

    @Serializable
    data class BillingResponse(val data: BillingDto)
}
