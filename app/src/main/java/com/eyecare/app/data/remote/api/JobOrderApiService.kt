package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.JobOrderDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface JobOrderApiService {
    @GET("job-orders")
    suspend fun getJobOrders(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): JobOrderDtos.JobOrderListResponse

    @GET("job-orders/{id}")
    suspend fun getJobOrder(@Path("id") id: Int): JobOrderDtos.JobOrderResponse
}
