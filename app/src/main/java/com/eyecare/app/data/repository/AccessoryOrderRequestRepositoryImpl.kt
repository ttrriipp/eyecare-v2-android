package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AccessoryOrderRequestApiService
import com.eyecare.app.data.remote.dto.AccessoryOrderRequestDtos
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.AccessoryOrderRequestItem
import com.eyecare.app.domain.model.AcceptedOrderSummary
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.ItemSnapshot
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject

class AccessoryOrderRequestRepositoryImpl @Inject constructor(
    private val api: AccessoryOrderRequestApiService,
) : AccessoryOrderRequestRepository {

    override suspend fun getRequests(filter: OrderRequestFilter, page: Int): Result<PaginatedResult<AccessoryOrderRequest>> = safeApiCall {
        val response = api.getRequests(filter = filter.apiValue, page = page)
        PaginatedResult(
            data = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getRequest(id: Int): Result<AccessoryOrderRequest> = safeApiCall {
        api.getRequest(id).data.toDomain()
    }

    override suspend fun submitRequest(discountType: String, items: List<Pair<Int, Int>>): Result<AccessoryOrderRequest> = safeApiCall {
        val body = AccessoryOrderRequestDtos.SubmitOrderRequest(
            requestedDiscountType = discountType,
            items = items.map { (variantId, quantity) ->
                AccessoryOrderRequestDtos.SubmitOrderItem(productVariantId = variantId, quantity = quantity)
            },
        )
        api.submitRequest(body).data.toDomain()
    }

    override suspend fun cancelRequest(id: Int): Result<AccessoryOrderRequest> = safeApiCall {
        api.cancelRequest(id).data.toDomain()
    }

    private fun AccessoryOrderRequestDtos.OrderRequestDto.toDomain() = AccessoryOrderRequest(
        id = id,
        requestNumber = requestNumber,
        status = OrderRequestStatus.from(status),
        subtotalAmount = subtotalAmount,
        requestedDiscountType = DiscountType.from(requestedDiscountType),
        resolvedBy = resolvedBy,
        resolvedAt = resolvedAt,
        items = items.map { it.toDomain() },
        rejectionReason = rejectionReason,
        cancelledAt = cancelledAt,
        createdAt = createdAt,
        order = order?.let {
            AcceptedOrderSummary(
                id = it.id,
                orderNumber = it.orderNumber,
                status = it.status,
                discountAmount = it.discountAmount,
                totalAmount = it.totalAmount,
                paymentExpiresAt = it.paymentExpiresAt,
            )
        },
    )

    private fun AccessoryOrderRequestDtos.OrderRequestItemDto.toDomain() = AccessoryOrderRequestItem(
        id = id,
        productVariantId = productVariantId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        amount = amount,
        itemKind = itemKind,
        itemSnapshot = itemSnapshot.let {
            ItemSnapshot(
                productName = it.productName,
                variantName = it.variantName,
                attributes = it.attributes.entries.associate { (key, value) ->
                    key to when (value) {
                        is JsonPrimitive -> value.content
                        else -> value.toString()
                    }
                },
                images = (it.images + listOfNotNull(imageUrl)).distinct(),
            )
        },
    )
}
