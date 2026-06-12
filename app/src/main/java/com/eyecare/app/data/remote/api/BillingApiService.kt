package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.BillingDtos
import retrofit2.http.GET
import retrofit2.http.Path

interface BillingApiService {
    @GET("billing/{id}")
    suspend fun getBilling(@Path("id") id: Int): BillingDtos.BillingResponse
}
