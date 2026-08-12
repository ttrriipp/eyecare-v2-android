package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.FrameReservationDtos
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FrameReservationApiService {

    @GET("frame-reservations")
    suspend fun getReservations(): FrameReservationDtos.ReservationListResponse

    @POST("frame-reservations")
    suspend fun createReservation(@Body request: FrameReservationDtos.CreateReservationRequest): FrameReservationDtos.ReservationResponse

    @DELETE("frame-reservations/{id}")
    suspend fun deleteReservation(@Path("id") reservationId: Int): Response<Unit>

    @POST("frame-reservations/{id}/items")
    suspend fun addItem(
        @Path("id") reservationId: Int,
        @Body request: FrameReservationDtos.AddItemRequest,
    ): FrameReservationDtos.ReservationResponse

    @DELETE("frame-reservations/{id}/items/{itemId}")
    suspend fun removeItem(
        @Path("id") reservationId: Int,
        @Path("itemId") itemId: Int,
    ): Response<FrameReservationDtos.ReservationResponse>
}
