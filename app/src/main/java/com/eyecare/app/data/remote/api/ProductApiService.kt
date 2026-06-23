package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.ProductDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): ProductDtos.PaginatedProductResponse

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDtos.ProductResponse
}
