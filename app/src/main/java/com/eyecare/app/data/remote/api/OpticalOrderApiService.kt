package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.OpticalOrderDtos
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface OpticalOrderApiService {
    @GET("optical-orders")
    suspend fun getOpticalOrders(
        @Query("filter") filter: String? = null,
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): OpticalOrderDtos.OpticalOrderListResponse

    @GET("optical-orders/{id}")
    suspend fun getOpticalOrder(@Path("id") id: Int): OpticalOrderDtos.OpticalOrderResponse

    @POST("optical-order-items/{id}/rating")
    suspend fun rateItem(
        @Path("id") itemId: Int,
        @Body body: OpticalOrderDtos.RatingRequest,
    ): OpticalOrderDtos.RatingResultResponse

    @Multipart
    @POST("optical-order-items/{id}/rating")
    suspend fun rateItemWithAttachment(
        @Path("id") itemId: Int,
        @Part attachment: MultipartBody.Part,
        @Part("rating") rating: RequestBody,
        @Part("comment") comment: RequestBody?,
        @Part("public_display_consent") publicDisplayConsent: RequestBody,
        @Part("public_attachment_consent") publicAttachmentConsent: RequestBody,
    ): OpticalOrderDtos.RatingResultResponse

    @Multipart
    @POST("optical-orders/{id}/payment-proof")
    suspend fun uploadPaymentProof(
        @Path("id") orderId: Int,
        @Part proof: MultipartBody.Part,
        @Part("sender_name") senderName: RequestBody,
        @Part("reference_number") referenceNumber: RequestBody,
        @Part("payment_method") paymentMethod: RequestBody? = null,
    ): OpticalOrderDtos.PaymentProofResultResponse
}
