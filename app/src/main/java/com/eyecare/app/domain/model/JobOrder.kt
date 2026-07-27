package com.eyecare.app.domain.model

data class JobOrder(
    val id: Int,
    val jobOrderNumber: String?,
    val patientId: Int?,
    val encounterId: Int?,
    val prescriptionId: Int?,
    val quotationRevisionId: Int?,
    val status: JobOrderStatus,
    val totalAmount: Double?,
    val notes: String?,
    val startedAt: String?,
    val readyAt: String?,
    val dispensedAt: String?,
    val cancelledAt: String?,
    val items: List<JobOrderItem>,
)

data class JobOrderItem(
    val id: Int,
    val description: String,
    val quantity: Int,
    val unitPrice: Double,
    val amount: Double,
    val productVariantId: Int?,
)

enum class JobOrderStatus {
    QUEUED, IN_PROGRESS, READY_FOR_DISPENSING, DISPENSED, CANCELLED;

    companion object {
        fun from(value: String): JobOrderStatus = when (value.lowercase()) {
            "queued" -> QUEUED
            "in_progress" -> IN_PROGRESS
            "ready_for_dispensing" -> READY_FOR_DISPENSING
            "dispensed" -> DISPENSED
            "cancelled" -> CANCELLED
            else -> QUEUED
        }
    }
}
