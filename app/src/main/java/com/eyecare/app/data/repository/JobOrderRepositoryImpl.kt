package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.JobOrderApiService
import com.eyecare.app.data.remote.dto.JobOrderDtos
import com.eyecare.app.domain.model.JobOrder
import com.eyecare.app.domain.model.JobOrderItem
import com.eyecare.app.domain.model.JobOrderStatus
import com.eyecare.app.domain.repository.JobOrderRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class JobOrderRepositoryImpl @Inject constructor(
    private val api: JobOrderApiService,
) : JobOrderRepository {

    override suspend fun getJobOrders(page: Int): Result<PaginatedResult<JobOrder>> = runCatching {
        val response = api.getJobOrders(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getJobOrder(id: Int): Result<JobOrder> = runCatching {
        api.getJobOrder(id).data.toDomain()
    }

    private fun JobOrderDtos.JobOrderDto.toDomain() = JobOrder(
        id = id,
        jobOrderNumber = jobOrderNumber,
        patientId = patientId,
        encounterId = encounterId,
        prescriptionId = prescriptionId,
        quotationRevisionId = quotationRevisionId,
        status = JobOrderStatus.from(status),
        totalAmount = totalAmount,
        notes = notes,
        startedAt = startedAt,
        readyAt = readyAt,
        dispensedAt = dispensedAt,
        cancelledAt = cancelledAt,
        items = items.map { it.toDomain() },
    )

    private fun JobOrderDtos.JobOrderItemDto.toDomain() = JobOrderItem(
        id = id,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        productVariantId = productVariantId,
    )
}
