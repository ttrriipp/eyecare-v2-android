package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.ProductDtos
import com.eyecare.app.data.remote.dto.ProductFilterDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {
    @GET("products")
    suspend fun getProducts(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
        @Query("search") search: String? = null,
        @Query("brand") brandId: Int? = null,
        @Query("category") categoryId: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("in_stock") inStock: Boolean? = null,
    ): ProductDtos.PaginatedProductResponse

    @GET("products/{id}")
    suspend fun getProduct(@Path("id") id: Int): ProductDtos.ProductResponse

    @GET("brands")
    suspend fun getBrands(): ProductFilterDtos.BrandListResponse

    @GET("categories")
    suspend fun getCategories(): ProductFilterDtos.CategoryListResponse
}
