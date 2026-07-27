package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.FrameReservationDtos
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface FrameReservationApiService {

    @GET("frame-reservations")
    suspend fun getReservations(): FrameReservationDtos.ReservationListResponse

    @POST("frame-reservations")
    suspend fun createReservation(request: FrameReservationDtos.CreateReservationRequest): FrameReservationDtos.ReservationResponse

    @POST("frame-reservations/{id}/cancel")
    suspend fun cancelReservation(@Path("id") reservationId: Int): FrameReservationDtos.ReservationResponse
}
