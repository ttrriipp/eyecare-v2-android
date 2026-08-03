package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.OpticalOrderDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OpticalOrderApiService {
    @GET("optical-orders")
    suspend fun getOpticalOrders(
        @Query("filter") filter: String? = null,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): OpticalOrderDtos.OpticalOrderListResponse

    @GET("optical-orders/{id}")
    suspend fun getOpticalOrder(@Path("id") id: Int): OpticalOrderDtos.OpticalOrderResponse

    @POST("optical-order-items/{id}/rating")
    suspend fun rateItem(
        @Path("id") itemId: Int,
        @Body body: OpticalOrderDtos.RatingRequest,
    ): OpticalOrderDtos.RatingResultDto
}
