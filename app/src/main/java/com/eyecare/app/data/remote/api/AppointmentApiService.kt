package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AppointmentDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface AppointmentApiService {
    @GET("appointments")
    suspend fun getAppointments(): AppointmentDtos.AppointmentListResponse

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") id: Int): AppointmentDtos.AppointmentResponse

    @POST("appointments")
    suspend fun createAppointment(@Body request: AppointmentDtos.CreateAppointmentRequest): AppointmentDtos.AppointmentResponse

    @POST("appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: Int): AppointmentDtos.AppointmentResponse

    @GET("visit-reasons")
    suspend fun getVisitReasons(): AppointmentDtos.VisitReasonListResponse
}
