package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object JobOrderDtos {

    @Serializable
    data class JobOrderItemDto(
        val id: Int,
        @SerialName("job_order_id") val jobOrderId: Int? = null,
        val description: String,
        val quantity: Int,
        @SerialName("unit_price") val unitPrice: Double,
        val amount: Double,
        @SerialName("product_variant_id") val productVariantId: Int? = null,
        @SerialName("lens_category_id") val lensCategoryId: Int? = null,
    )

    @Serializable
    data class JobOrderDto(
        val id: Int,
        @SerialName("job_order_number") val jobOrderNumber: String? = null,
        @SerialName("patient_id") val patientId: Int? = null,
        @SerialName("encounter_id") val encounterId: Int? = null,
        @SerialName("prescription_id") val prescriptionId: Int? = null,
        @SerialName("quotation_revision_id") val quotationRevisionId: Int? = null,
        val status: String,
        @SerialName("total_amount") val totalAmount: Double? = null,
        val notes: String? = null,
        @SerialName("started_at") val startedAt: String? = null,
        @SerialName("ready_at") val readyAt: String? = null,
        @SerialName("dispensed_at") val dispensedAt: String? = null,
        @SerialName("cancelled_at") val cancelledAt: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("updated_at") val updatedAt: String? = null,
        val items: List<JobOrderItemDto> = emptyList(),
    )

    @Serializable
    data class JobOrderListResponse(
        val data: List<JobOrderDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class JobOrderResponse(val data: JobOrderDto)
}
