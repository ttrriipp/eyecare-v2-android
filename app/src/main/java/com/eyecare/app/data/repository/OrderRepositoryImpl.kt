package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.OrderApiService
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderError
import com.eyecare.app.domain.model.OrderItem
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.repository.OrderRepository
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import javax.inject.Inject

class OrderRepositoryImpl @Inject constructor(
    private val api: OrderApiService,
    private val json: Json,
) : OrderRepository {

    private var lastMeta: OrderDtos.PaginationMeta? = null

    override suspend fun getOrders(page: Int): Result<List<Order>> = runCatching {
        val response = api.getOrders(page = page)
        lastMeta = response.meta
        response.data.map { it.toDomain() }
    }

    override suspend fun hasMorePages(page: Int): Boolean {
        val meta = lastMeta ?: return false
        return page < meta.lastPage
    }

    override suspend fun getOrder(id: Int): Result<Order> = runCatching {
        api.getOrder(id).data.toDomain()
    }

    override suspend fun createOrder(
        appointmentId: Int?,
        isNonPrescription: Boolean,
        items: List<OrderDtos.OrderItemRequest>,
    ): Result<Order> = runCatching {
        api.createOrder(OrderDtos.CreateOrderRequest(appointmentId, isNonPrescription, items)).data.toDomain()
    }.recoverCatching { throwable ->
        if (throwable is HttpException && throwable.code() == 422) {
            val body = throwable.response()?.errorBody()?.use { it.string() } ?: ""
            val parsed = json.decodeFromString<OrderDtos.ValidationErrorBody>(body)
            throw OrderError.ValidationError(parsed.errors)
        }
        throw throwable
    }

    override suspend fun cancelOrder(id: Int): Result<Order> = runCatching {
        api.cancelOrder(id).data.toDomain()
    }

    private fun OrderDtos.OrderDto.toDomain() = Order(
        id = id, orderNumber = orderNumber, appointmentId = appointmentId,
        isNonPrescription = isNonPrescription, status = OrderStatus.from(status),
        subtotal = subtotal, totalAmount = totalAmount,
        items = items.map { it.toDomain() }, createdAt = createdAt,
    )

    private fun OrderDtos.OrderItemDto.toDomain() = OrderItem(
        id = id, productVariantId = productVariantId, lensTypeId = lensTypeId,
        productId = productId, productName = productName, variantName = variantName,
        variantSku = variantSku, lensTypeName = lensTypeName,
        unitPrice = unitPrice, quantity = quantity, subtotal = subtotal,
        imageUrl = (variantImages.firstOrNull() ?: productImages.firstOrNull()),
    )
}
