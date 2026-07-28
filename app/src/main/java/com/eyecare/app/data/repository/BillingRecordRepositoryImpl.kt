package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.BillingRecordApiService
import com.eyecare.app.data.remote.dto.BillingRecordDtos
import com.eyecare.app.domain.model.BillingPayment
import com.eyecare.app.domain.model.BillingPaymentStatus
import com.eyecare.app.domain.model.BillingRecord
import com.eyecare.app.domain.model.BillingRecordStatus
import com.eyecare.app.domain.repository.BillingRecordRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class BillingRecordRepositoryImpl @Inject constructor(
    private val api: BillingRecordApiService,
) : BillingRecordRepository {

    override suspend fun getBillingRecords(page: Int): Result<PaginatedResult<BillingRecord>> = runCatching {
        val response = api.getBillingRecords(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getBillingRecord(id: Int): Result<BillingRecord> = runCatching {
        api.getBillingRecord(id).data.toDomain()
    }

    private fun BillingRecordDtos.BillingRecordDto.toDomain() = BillingRecord(
        id = id,
        billingRecordNumber = billingRecordNumber,
        jobOrderId = jobOrderId,
        status = BillingRecordStatus.from(status),
        totalAmount = totalAmount,
        amountPaid = amountPaid,
        balanceDue = balanceDue,
        recordedAt = recordedAt,
        payments = payments.map { it.toDomain() },
    )

    private fun BillingRecordDtos.BillingPaymentDto.toDomain() = BillingPayment(
        id = id,
        amount = amount,
        paymentMethod = paymentMethod,
        referenceNumber = referenceNumber,
        status = BillingPaymentStatus.from(status),
        recordedAt = recordedAt,
    )
}
