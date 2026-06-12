package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.ProductDtos
import retrofit2.http.GET
import retrofit2.http.Path

interface ProductApiService {
    @GET("products")
    suspend fun getProducts(): ProductDtos.ProductListResponse

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDtos.ProductResponse
}
