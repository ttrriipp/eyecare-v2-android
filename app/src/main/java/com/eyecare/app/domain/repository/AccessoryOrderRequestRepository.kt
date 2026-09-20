package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.OrderRequestFilter

interface AccessoryOrderRequestRepository {
    suspend fun getRequests(filter: OrderRequestFilter, page: Int = 1): Result<PaginatedResult<AccessoryOrderRequest>>
    suspend fun getRequest(id: Int): Result<AccessoryOrderRequest>
    suspend fun submitRequest(discountType: String, items: List<Pair<Int, Int>>): Result<AccessoryOrderRequest>
    suspend fun cancelRequest(id: Int): Result<AccessoryOrderRequest>
}