package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.OrderDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface OrderApiService {
    @GET("orders")
    suspend fun getOrders(): OrderDtos.OrderListResponse

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): OrderDtos.OrderResponse

    @POST("orders")
    suspend fun createOrder(@Body request: OrderDtos.CreateOrderRequest): OrderDtos.OrderResponse
}
