package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

object EyewearDtos {

    // ── List summary ──────────────────────────────────────────────────

    @Serializable
    data class EyewearSummaryDto(
        val key: String,
        val description: String? = null,
        @SerialName("consultation_at") val consultationAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val progress: String,
        @SerialName("payment_status") val paymentStatus: String? = null,
        @SerialName("total_amount")
        @Serializable(with = MoneyValueSerializer::class)
        val totalAmount: BigDecimal,
        @SerialName("balance_due")
        @Serializable(with = MoneyValueSerializer::class)
        val balanceDue: BigDecimal? = null,
        @SerialName("activity_at") val activityAt: String,
    )

    @Serializable
    data class EyewearListResponse(
        val data: List<EyewearSummaryDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    // ── Detail sections ───────────────────────────────────────────────

    @Serializable
    data class EyewearItemDto(
        val id: Int? = null,
        val description: String,
        val quantity: Int,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("unit_price") val unitPrice: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("product_variant_id") val productVariantId: Int? = null,
    )

    @Serializable
    data class EyewearEstimateDto(
        @SerialName("quotation_number") val quotationNumber: String? = null,
        val status: String? = null,
        @SerialName("valid_until") val validUntil: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val subtotal: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("discount_amount") val discountAmount: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val total: BigDecimal? = null,
        val items: List<EyewearItemDto> = emptyList(),
    )

    @Serializable
    data class EyewearPreparationDto(
        @SerialName("job_order_number") val jobOrderNumber: String? = null,
        val status: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal? = null,
        @SerialName("started_at") val startedAt: String? = null,
        @SerialName("ready_at") val readyAt: String? = null,
        val items: List<EyewearItemDto> = emptyList(),
    )

    @Serializable
    data class EyewearDispensingDto(
        val status: String? = null,
        @SerialName("ready_at") val readyAt: String? = null,
        @SerialName("dispensed_at") val dispensedAt: String? = null,
    )

    @Serializable
    data class EyewearPaymentDto(
        val id: Int,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("payment_method") val paymentMethod: String,
        @SerialName("reference_number") val referenceNumber: String? = null,
        @SerialName("recorded_at") val recordedAt: String? = null,
    )

    @Serializable
    data class EyewearPaymentSummaryDto(
        @SerialName("billing_record_number") val billingRecordNumber: String? = null,
        val status: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("amount_paid") val amountPaid: BigDecimal? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("balance_due") val balanceDue: BigDecimal? = null,
        val payments: List<EyewearPaymentDto> = emptyList(),
    )

    // ── Detail response ───────────────────────────────────────────────

    @Serializable
    data class EyewearDetailDto(
        val key: String,
        val description: String? = null,
        @SerialName("consultation_at") val consultationAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val progress: String,
        @SerialName("payment_status") val paymentStatus: String? = null,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("balance_due") val balanceDue: BigDecimal? = null,
        @SerialName("activity_at") val activityAt: String,
        val estimate: EyewearEstimateDto? = null,
        val preparation: EyewearPreparationDto? = null,
        val dispensing: EyewearDispensingDto? = null,
        @SerialName("payment_summary") val paymentSummary: EyewearPaymentSummaryDto? = null,
    )

    @Serializable
    data class EyewearDetailResponse(val data: EyewearDetailDto)
}
