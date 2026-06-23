package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.OrderDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApiService {
    @GET("orders")
    suspend fun getOrders(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): OrderDtos.PaginatedOrderResponse

    @GET("orders/{id}")
    suspend fun getOrder(@Path("id") id: Int): OrderDtos.OrderResponse

    @POST("orders")
    suspend fun createOrder(@Body request: OrderDtos.CreateOrderRequest): OrderDtos.OrderResponse
}
