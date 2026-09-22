package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.OpticalOrderApiService
import com.eyecare.app.data.remote.dto.OpticalOrderDtos
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentInstructions
import com.eyecare.app.domain.model.PaymentMethodInstructions
import com.eyecare.app.domain.model.PaymentProofResult
import com.eyecare.app.domain.model.PaymentProofStatus
import com.eyecare.app.domain.model.PaymentProofSummary
import com.eyecare.app.domain.model.PaymentProofUpload
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.domain.model.PaymentSummary
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.model.RatingSummary
import com.eyecare.app.domain.repository.OpticalOrderRepository
import com.eyecare.app.domain.repository.PaginatedResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject

class OpticalOrderRepositoryImpl @Inject constructor(
    private val api: OpticalOrderApiService,
) : OpticalOrderRepository {

    override suspend fun getOpticalOrders(filter: String?, page: Int): Result<PaginatedResult<OpticalOrder>> = safeApiCall {
        val response = api.getOpticalOrders(filter = filter, page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getOpticalOrder(id: Int): Result<OpticalOrder> = safeApiCall {
        api.getOpticalOrder(id).data.toDomain()
    }

    override suspend fun rateItem(itemId: Int, rating: Int, comment: String?): Result<RatingResult> = safeApiCall {
        val response = api.rateItem(itemId, OpticalOrderDtos.RatingRequest(rating = rating, comment = comment))
        val result = response.data
        RatingResult(
            id = result.id,
            itemId = result.itemId,
            rating = result.rating,
            comment = result.comment,
            productVariantId = result.productVariantId,
            createdAt = result.createdAt,
        )
    }

    override suspend fun uploadPaymentProof(orderId: Int, proof: PaymentProofUpload): Result<PaymentProofResult> = safeApiCall {
        var uploadSucceeded = false
        try {
            val imagePart = MultipartBody.Part.createFormData(
                name = "proof",
                filename = proof.imageFile.name,
                body = proof.imageFile.asRequestBody(proof.mimeType.toMediaType()),
            )
            val senderNamePart = proof.senderName.trim().toRequestBody("text/plain".toMediaType())
            val referencePart = proof.referenceNumber.trim().toRequestBody("text/plain".toMediaType())
            val paymentMethodPart = proof.paymentMethod.trim().ifBlank { "gcash" }
                .toRequestBody("text/plain".toMediaType())

            val response = api.uploadPaymentProof(
                orderId,
                imagePart,
                senderNamePart,
                referencePart,
                paymentMethodPart,
            )
            val result = response.data
            val paymentProofResult = PaymentProofResult(
                id = result.id,
                status = PaymentProofStatus.from(result.status),
                paymentMethod = result.paymentMethod,
                senderName = result.senderName,
                referenceNumber = result.referenceNumber,
                createdAt = result.createdAt,
            )
            uploadSucceeded = true
            paymentProofResult
        } finally {
            if (proof.deleteAfterUpload && uploadSucceeded) {
                proof.imageFile.delete()
            }
        }
    }

    private fun OpticalOrderDtos.OpticalOrderDto.toDomain() = OpticalOrder(
        id = id,
        orderNumber = orderNumber,
        status = OpticalOrderStatus.from(status),
        fulfillmentMode = FulfillmentMode.from(fulfillmentMode),
        totalAmount = totalAmount,
        startedAt = startedAt,
        readyAt = readyAt,
        dispensedAt = dispensedAt,
        cancelledAt = cancelledAt,
        createdAt = createdAt,
        items = items.map { it.toDomain() },
        paymentSummary = paymentSummary?.let {
            PaymentSummary(
                status = PaymentStatus.from(it.status),
                totalAmount = it.totalAmount,
                amountPaid = it.amountPaid,
                balanceDue = it.balanceDue,
                paymentDueDate = it.paymentDueDate,
                isOverdue = it.isOverdue,
            )
        },
        paymentExpiresAt = paymentExpiresAt,
        paymentInstructions = paymentInstructions?.let {
            PaymentInstructions(
                method = it.method,
                label = it.label,
                clinicAccountName = it.clinicAccountName,
                clinicAccountNumber = it.clinicAccountNumber,
                bankName = it.bankName,
                amount = it.amount,
                orderReference = it.orderReference,
                paymentExpiresAt = it.paymentExpiresAt,
                qrImageUrl = it.qrImageUrl,
                availableMethods = it.availableMethods.map { method -> method.toDomain() }
                    .ifEmpty { listOf(it.toDomain()) },
            )
        },
        paymentProof = paymentProof?.let {
            PaymentProofSummary(
                id = it.id,
                status = PaymentProofStatus.from(it.status),
                paymentMethod = it.paymentMethod,
                senderName = it.senderName,
                referenceNumber = it.referenceNumber,
                rejectionReason = it.rejectionReason,
                createdAt = it.createdAt,
            )
        },
        paymentProofStatus = PaymentProofStatus.from(paymentProofStatus ?: paymentProof?.status ?: "not_submitted"),
        paymentProofMethod = paymentProofMethod ?: paymentProof?.paymentMethod,
        paymentProofRejectionReason = paymentProofRejectionReason ?: paymentProof?.rejectionReason,
    )

    private fun OpticalOrderDtos.PaymentInstructionsDto.toDomain() = PaymentMethodInstructions(
        method = method,
        label = label,
        clinicAccountName = clinicAccountName,
        clinicAccountNumber = clinicAccountNumber,
        bankName = bankName,
        amount = amount,
        orderReference = orderReference,
        paymentExpiresAt = paymentExpiresAt,
        qrImageUrl = qrImageUrl,
    )

    private fun OpticalOrderDtos.PaymentMethodInstructionsDto.toDomain() = PaymentMethodInstructions(
        method = method,
        label = label,
        clinicAccountName = clinicAccountName,
        clinicAccountNumber = clinicAccountNumber,
        bankName = bankName,
        amount = amount,
        orderReference = orderReference,
        paymentExpiresAt = paymentExpiresAt,
        qrImageUrl = qrImageUrl,
    )

    private fun OpticalOrderDtos.OpticalOrderItemDto.toDomain() = OpticalOrderItem(
        id = id,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        productVariantId = productVariantId,
        isRateable = isRateable,
        rating = rating?.let {
            RatingSummary(rating = it.rating, comment = it.comment, createdAt = it.createdAt)
        },
        imagePath = imageUrl,
    )
}
