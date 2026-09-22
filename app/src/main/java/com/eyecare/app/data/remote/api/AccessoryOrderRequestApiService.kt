package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AccessoryOrderRequestDtos
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AccessoryOrderRequestApiService {

    @GET("accessory-order-requests")
    suspend fun getRequests(
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): AccessoryOrderRequestDtos.OrderRequestListResponse

    @POST("accessory-order-requests")
    suspend fun submitRequest(
        @Body body: AccessoryOrderRequestDtos.SubmitOrderRequest,
    ): AccessoryOrderRequestDtos.OrderRequestResponse

    @GET("accessory-order-requests/{id}")
    suspend fun getRequest(@Path("id") id: Int): AccessoryOrderRequestDtos.OrderRequestResponse

    @POST("accessory-order-requests/{id}/cancel")
    suspend fun cancelRequest(@Path("id") id: Int): AccessoryOrderRequestDtos.OrderRequestResponse

    @Multipart
    @POST("accessory-order-requests/{id}/discount-proof")
    suspend fun uploadDiscountProof(
        @Path("id") requestId: Int,
        @Part proof: MultipartBody.Part,
    ): AccessoryOrderRequestDtos.DiscountProofResultResponse
}
