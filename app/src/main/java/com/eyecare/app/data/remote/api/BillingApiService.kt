package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.BillingDtos
import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Streaming

interface BillingApiService {
    @GET("billing/{id}")
    suspend fun getBilling(@Path("id") id: Int): BillingDtos.BillingResponse

    @Streaming
    @GET("billing/{id}/pdf")
    suspend fun downloadBillingPdf(@Path("id") id: Int): ResponseBody
}
