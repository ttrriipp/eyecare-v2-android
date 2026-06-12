package com.eyecare.app.domain.repository

import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Order

interface OrderRepository {
    suspend fun getOrders(): Result<List<Order>>
    suspend fun getOrder(id: Int): Result<Order>
    suspend fun createOrder(
        appointmentId: Int?,
        isNonPrescription: Boolean,
        items: List<OrderDtos.OrderItemRequest>,
    ): Result<Order>
}
