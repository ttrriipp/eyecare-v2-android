package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.QuotationApiService
import com.eyecare.app.data.remote.dto.QuotationDtos
import com.eyecare.app.domain.model.OpticalOrderReference
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.model.QuotationItem
import com.eyecare.app.domain.model.QuotationItemType
import com.eyecare.app.domain.model.QuotationStatus
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.QuotationRepository
import java.math.BigDecimal
import javax.inject.Inject

class QuotationRepositoryImpl @Inject constructor(
    private val api: QuotationApiService,
) : QuotationRepository {

    override suspend fun getQuotations(filter: String?, page: Int): Result<PaginatedResult<Quotation>> = runCatching {
        val response = api.getQuotations(filter = filter, page = page)
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

    private fun QuotationDtos.QuotationDto.toDomain(): Quotation {
        val resolvedSubtotal = subtotal ?: items.fold(BigDecimal.ZERO) { sum, item -> sum + item.amount }
        val resolvedDiscountAmount = discountAmount ?: BigDecimal.ZERO
        val resolvedTotal = total ?: resolvedSubtotal - resolvedDiscountAmount

        return Quotation(
            id = id,
            quotationNumber = quotationNumber,
            status = QuotationStatus.from(status),
            validUntil = validUntil,
            subtotal = resolvedSubtotal,
            discountAmount = resolvedDiscountAmount,
            total = resolvedTotal,
            notes = notes,
            createdAt = createdAt,
            presentedAt = presentedAt,
            confirmedAt = confirmedAt,
            opticalOrder = opticalOrder?.let {
                OpticalOrderReference(id = it.id, orderNumber = it.orderNumber)
            },
            items = items.map { it.toDomain() },
        )
    }

    private fun QuotationDtos.QuotationItemDto.toDomain() = QuotationItem(
        id = id,
        itemType = QuotationItemType.from(itemType),
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        productVariantId = productVariantId,
        lensCategoryId = lensCategoryId,
        serviceId = serviceId,
    )
}
