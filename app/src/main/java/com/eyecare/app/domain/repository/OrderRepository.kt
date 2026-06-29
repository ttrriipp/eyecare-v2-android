package com.eyecare.app.domain.repository

import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Order

interface OrderRepository {
    suspend fun getOrders(page: Int = 1): Result<List<Order>>
    suspend fun hasMorePages(page: Int): Boolean
    suspend fun getOrder(id: Int): Result<Order>
    suspend fun createOrder(
        appointmentId: Int?,
        isNonPrescription: Boolean,
        items: List<OrderDtos.OrderItemRequest>,
    ): Result<Order>
    suspend fun cancelOrder(id: Int): Result<Order>
}
