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
        @SerialName("image_url") val imageUrl: String? = null,
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
    data class PaymentMethodInstructionsDto(
        val method: String,
        val label: String = "Payment",
        @SerialName("clinic_account_name") val clinicAccountName: String,
        @SerialName("clinic_account_number") val clinicAccountNumber: String,
        @SerialName("bank_name") val bankName: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("order_reference") val orderReference: String,
        @SerialName("payment_expires_at") val paymentExpiresAt: String? = null,
        @SerialName("qr_image_url") val qrImageUrl: String? = null,
    )

    @Serializable
    data class PaymentInstructionsDto(
        val method: String,
        val label: String = "Payment",
        @SerialName("clinic_account_name") val clinicAccountName: String,
        @SerialName("clinic_account_number") val clinicAccountNumber: String,
        @SerialName("bank_name") val bankName: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("order_reference") val orderReference: String,
        @SerialName("payment_expires_at") val paymentExpiresAt: String? = null,
        @SerialName("qr_image_url") val qrImageUrl: String? = null,
        @SerialName("available_methods") val availableMethods: List<PaymentMethodInstructionsDto> = emptyList(),
    )

    @Serializable
    data class PaymentProofSummaryDto(
        val id: Int,
        val status: String,
        @SerialName("payment_method") val paymentMethod: String? = null,
        @SerialName("sender_name") val senderName: String,
        @SerialName("reference_number") val referenceNumber: String,
        @SerialName("rejection_reason") val rejectionReason: String? = null,
        @SerialName("created_at") val createdAt: String,
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
        val items: List<OpticalOrderItemDto> = emptyList(),
        @SerialName("payment_summary") val paymentSummary: PaymentSummaryDto? = null,
        @SerialName("payment_expires_at") val paymentExpiresAt: String? = null,
        @SerialName("payment_instructions") val paymentInstructions: PaymentInstructionsDto? = null,
        @SerialName("payment_proof") val paymentProof: PaymentProofSummaryDto? = null,
        @SerialName("payment_proof_status") val paymentProofStatus: String? = null,
        @SerialName("payment_proof_method") val paymentProofMethod: String? = null,
        @SerialName("payment_proof_rejection_reason") val paymentProofRejectionReason: String? = null,
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
        @SerialName("created_at") val createdAt: String? = null,
    )

    @Serializable
    data class PaymentProofResponse(
        val id: Int,
        val status: String,
        @SerialName("payment_method") val paymentMethod: String? = null,
        @SerialName("sender_name") val senderName: String,
        @SerialName("reference_number") val referenceNumber: String,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    data class PaymentProofResultResponse(val data: PaymentProofResponse)

    @Serializable
    data class RatingResultResponse(
        val data: RatingResultDto,
    )
}
