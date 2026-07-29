package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.EyewearApiService
import com.eyecare.app.data.remote.dto.EyewearDtos
import com.eyecare.app.domain.model.EyewearDetail
import com.eyecare.app.domain.model.EyewearDispensing
import com.eyecare.app.domain.model.EyewearEstimate
import com.eyecare.app.domain.model.EyewearItem
import com.eyecare.app.domain.model.EyewearPayment
import com.eyecare.app.domain.model.EyewearPaymentStatus
import com.eyecare.app.domain.model.EyewearPaymentSummary
import com.eyecare.app.domain.model.EyewearPreparation
import com.eyecare.app.domain.model.EyewearProgress
import com.eyecare.app.domain.model.EyewearSummary
import com.eyecare.app.domain.repository.EyewearRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class EyewearRepositoryImpl @Inject constructor(
    private val api: EyewearApiService,
) : EyewearRepository {

    override suspend fun getEyewear(filter: String, page: Int): Result<PaginatedResult<EyewearSummary>> = runCatching {
        val response = api.getEyewear(filter = filter, page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getEyewearDetail(key: String): Result<EyewearDetail> = runCatching {
        api.getEyewearDetail(key).data.toDomain()
    }

    private fun EyewearDtos.EyewearSummaryDto.toDomain() = EyewearSummary(
        key = key,
        description = description ?: "Eyewear transaction",
        consultationAt = consultationAt,
        createdAt = createdAt,
        progress = EyewearProgress.fromApi(progress),
        paymentStatus = paymentStatus?.let(EyewearPaymentStatus::fromApi),
        totalAmount = totalAmount,
        balanceDue = balanceDue,
        activityAt = activityAt,
    )

    private fun EyewearDtos.EyewearDetailDto.toDomain() = EyewearDetail(
        key = key,
        description = description,
        consultationAt = consultationAt,
        createdAt = createdAt,
        progress = EyewearProgress.fromApi(progress),
        paymentStatus = paymentStatus?.let(EyewearPaymentStatus::fromApi),
        totalAmount = totalAmount,
        balanceDue = balanceDue,
        activityAt = activityAt,
        estimate = estimate?.toDomain(),
        preparation = preparation?.toDomain(),
        dispensing = dispensing?.toDomain(),
        paymentSummary = paymentSummary?.toDomain(),
    )

    private fun EyewearDtos.EyewearEstimateDto.toDomain() = EyewearEstimate(
        quotationNumber = quotationNumber,
        status = status,
        validUntil = validUntil,
        subtotal = subtotal,
        discountAmount = discountAmount,
        total = total,
        items = items.map { it.toDomain() },
    )

    private fun EyewearDtos.EyewearPreparationDto.toDomain() = EyewearPreparation(
        jobOrderNumber = jobOrderNumber,
        status = status,
        totalAmount = totalAmount,
        startedAt = startedAt,
        readyAt = readyAt,
        items = items.map { it.toDomain() },
    )

    private fun EyewearDtos.EyewearDispensingDto.toDomain() = EyewearDispensing(
        status = status,
        readyAt = readyAt,
        dispensedAt = dispensedAt,
    )

    private fun EyewearDtos.EyewearPaymentSummaryDto.toDomain() = EyewearPaymentSummary(
        billingRecordNumber = billingRecordNumber,
        status = status,
        totalAmount = totalAmount,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        payments = payments.map { it.toDomain() },
    )

    private fun EyewearDtos.EyewearPaymentDto.toDomain() = EyewearPayment(
        id = id,
        amount = amount,
        paymentMethod = paymentMethod,
        referenceNumber = referenceNumber,
        recordedAt = recordedAt,
    )

    private fun EyewearDtos.EyewearItemDto.toDomain() = EyewearItem(
        id = id,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        productVariantId = productVariantId,
    )
}
