package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.BillingRecordDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface BillingRecordApiService {
    @GET("billing-records")
    suspend fun getBillingRecords(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): BillingRecordDtos.BillingRecordListResponse

    @GET("billing-records/{id}")
    suspend fun getBillingRecord(@Path("id") id: Int): BillingRecordDtos.BillingRecordResponse
}
