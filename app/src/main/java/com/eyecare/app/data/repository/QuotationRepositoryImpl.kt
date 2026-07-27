package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.QuotationApiService
import com.eyecare.app.data.remote.dto.QuotationDtos
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.model.QuotationItem
import com.eyecare.app.domain.model.QuotationRevision
import com.eyecare.app.domain.model.QuotationStatus
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.QuotationRepository
import javax.inject.Inject

class QuotationRepositoryImpl @Inject constructor(
    private val api: QuotationApiService,
) : QuotationRepository {

    override suspend fun getQuotations(page: Int): Result<PaginatedResult<Quotation>> = runCatching {
        val response = api.getQuotations(page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getQuotation(id: Int): Result<Quotation> = runCatching {
        api.getQuotation(id).data.toDomain()
    }

    private fun QuotationDtos.QuotationDto.toDomain() = Quotation(
        id = id,
        quotationNumber = quotationNumber,
        status = QuotationStatus.from(status),
        validUntil = validUntil,
        notes = notes,
        revision = revision?.toDomain(),
        createdAt = createdAt,
    )

    private fun QuotationDtos.QuotationRevisionDto.toDomain() = QuotationRevision(
        revisionNumber = revisionNumber,
        subtotal = subtotal,
        discountAmount = discountAmount,
        total = total,
        items = items.map { it.toDomain() },
    )

    private fun QuotationDtos.QuotationItemDto.toDomain() = QuotationItem(
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
    )
}
