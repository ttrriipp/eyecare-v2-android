package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AccessoryDtos
import com.eyecare.app.data.remote.dto.ProductReviewDtos
import com.eyecare.app.domain.model.AccessoryQuery
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface AccessoryApiService {

    @GET("accessories")
    suspend fun getAccessories(
        @Query("search") search: String? = null,
        @Query("brand") brand: Int? = null,
        @Query("category") category: Int? = null,
        @Query("sort") sort: String? = null,
        @Query("minimum_rating") minimumRating: Int? = null,
        @Query("rated") rated: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): AccessoryDtos.AccessoryListResponse

    @GET("accessories/{id}")
    suspend fun getAccessory(@Path("id") id: Int): AccessoryDtos.AccessoryResponse

    @GET("accessories/{id}/reviews")
    suspend fun getAccessoryReviews(
        @Path("id") id: Int,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): ProductReviewDtos.ProductReviewListResponse
}

suspend fun AccessoryApiService.getAccessories(
    query: AccessoryQuery = AccessoryQuery(),
): AccessoryDtos.AccessoryListResponse = getAccessories(
    search = query.search?.takeIf { it.isNotBlank() },
    brand = query.brand,
    category = query.category,
    sort = query.sort,
    minimumRating = query.minimumRating,
    rated = query.rated,
    page = query.page,
    perPage = query.perPage,
)
