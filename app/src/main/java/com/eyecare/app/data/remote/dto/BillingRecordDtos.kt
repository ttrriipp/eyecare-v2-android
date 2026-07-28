package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal

object BillingRecordDtos {

    @Serializable
    data class BillingPaymentDto(
        val id: Int,
        @SerialName("billing_record_id") val billingRecordId: Int? = null,
        @Serializable(with = MoneyValueSerializer::class)
        val amount: BigDecimal,
        @SerialName("payment_method") val paymentMethod: String,
        @SerialName("reference_number") val referenceNumber: String? = null,
        @SerialName("recorded_by") val recordedBy: Int? = null,
        @SerialName("recorded_at") val recordedAt: String? = null,
        val notes: String? = null,
        val status: String,
    )

    @Serializable
    data class BillingRecordDto(
        val id: Int,
        @SerialName("billing_record_number") val billingRecordNumber: String,
        @SerialName("patient_id") val patientId: Int? = null,
        @SerialName("job_order_id") val jobOrderId: Int,
        @SerialName("encounter_id") val encounterId: Int? = null,
        val status: String,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("total_amount") val totalAmount: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("amount_paid") val amountPaid: BigDecimal,
        @Serializable(with = MoneyValueSerializer::class)
        @SerialName("balance_due") val balanceDue: BigDecimal,
        @SerialName("recorded_by") val recordedBy: Int? = null,
        @SerialName("recorded_at") val recordedAt: String? = null,
        val payments: List<BillingPaymentDto> = emptyList(),
    )

    @Serializable
    data class BillingRecordListResponse(
        val data: List<BillingRecordDto>,
        val links: JsonElement? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class BillingRecordResponse(val data: BillingRecordDto)
}
