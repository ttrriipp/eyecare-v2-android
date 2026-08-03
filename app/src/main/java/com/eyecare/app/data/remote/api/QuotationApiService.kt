package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.QuotationDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface QuotationApiService {
    @GET("quotations")
    suspend fun getQuotations(
        @Query("filter") filter: String? = null,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): QuotationDtos.QuotationListResponse

    @GET("quotations/{id}")
    suspend fun getQuotation(@Path("id") id: Int): QuotationDtos.QuotationResponse
}
