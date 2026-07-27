package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.InvoiceDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface InvoiceApiService {
    @GET("invoices")
    suspend fun getInvoices(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): InvoiceDtos.InvoiceListResponse

    @GET("invoices/{id}")
    suspend fun getInvoice(@Path("id") id: Int): InvoiceDtos.InvoiceResponse
}
