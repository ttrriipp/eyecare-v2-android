package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.OpticalOrderApiService
import com.eyecare.app.data.remote.dto.OpticalOrderDtos
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.domain.model.PaymentSummary
import com.eyecare.app.domain.model.QuotationReference
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.model.RatingSummary
import com.eyecare.app.domain.repository.OpticalOrderRepository
import com.eyecare.app.domain.repository.PaginatedResult
import javax.inject.Inject

class OpticalOrderRepositoryImpl @Inject constructor(
    private val api: OpticalOrderApiService,
) : OpticalOrderRepository {

    override suspend fun getOpticalOrders(filter: String?, page: Int): Result<PaginatedResult<OpticalOrder>> = runCatching {
        val response = api.getOpticalOrders(filter = filter, page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getOpticalOrder(id: Int): Result<OpticalOrder> = runCatching {
        api.getOpticalOrder(id).data.toDomain()
    }

    override suspend fun rateItem(itemId: Int, rating: Int, comment: String?): Result<RatingResult> = runCatching {
        val response = api.rateItem(itemId, OpticalOrderDtos.RatingRequest(rating = rating, comment = comment))
        RatingResult(
            id = response.id,
            itemId = response.itemId,
            rating = response.rating,
            comment = response.comment,
            revisionNumber = response.revisionNumber,
        )
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
        sourceQuotation = sourceQuotation?.let {
            QuotationReference(id = it.id, quotationNumber = it.quotationNumber)
        },
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
            RatingSummary(rating = it.rating, comment = it.comment, revisionNumber = it.revisionNumber, createdAt = it.createdAt)
        },
    )
}
