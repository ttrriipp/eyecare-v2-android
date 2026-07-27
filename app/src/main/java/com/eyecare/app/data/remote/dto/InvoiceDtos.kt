package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object InvoiceDtos {

    @Serializable
    data class InvoiceItemDto(
        val id: Int,
        @SerialName("invoice_id") val invoiceId: Int? = null,
        val type: String? = null,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price") val unitPrice: Double,
        val amount: Double,
        @SerialName("job_order_item_id") val jobOrderItemId: Int? = null,
    )

    @Serializable
    data class InvoicePaymentDto(
        val id: Int,
        @SerialName("invoice_id") val invoiceId: Int? = null,
        val amount: Double,
        @SerialName("payment_method") val paymentMethod: String,
        @SerialName("reference_number") val referenceNumber: String? = null,
        @SerialName("recorded_by") val recordedBy: Int? = null,
        @SerialName("recorded_at") val recordedAt: String? = null,
        val notes: String? = null,
        val status: String,
    )

    @Serializable
    data class InvoiceDto(
        val id: Int,
        @SerialName("invoice_number") val invoiceNumber: String? = null,
        @SerialName("official_number") val officialNumber: String? = null,
        @SerialName("patient_id") val patientId: Int? = null,
        @SerialName("job_order_id") val jobOrderId: Int? = null,
        @SerialName("encounter_id") val encounterId: Int? = null,
        val status: String,
        @SerialName("sale_type") val saleType: String? = null,
        @SerialName("sold_to_name") val soldToName: String? = null,
        val subtotal: Double? = null,
        @SerialName("discount_amount") val discountAmount: Double? = null,
        @SerialName("tax_amount") val taxAmount: Double? = null,
        val total: Double? = null,
        @SerialName("amount_paid") val amountPaid: Double? = null,
        @SerialName("balance_due") val balanceDue: Double? = null,
        val notes: String? = null,
        @SerialName("recorded_by") val recordedBy: Int? = null,
        @SerialName("issued_at") val issuedAt: String? = null,
        val items: List<InvoiceItemDto> = emptyList(),
        val payments: List<InvoicePaymentDto> = emptyList(),
    )

    @Serializable
    data class InvoiceListResponse(
        val data: List<InvoiceDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class InvoiceResponse(val data: InvoiceDto)
}
