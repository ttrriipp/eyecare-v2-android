package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AppointmentV1Dtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentV1ApiService {

    @GET("appointment-availability")
    suspend fun getAppointmentAvailability(
        @Query("date") date: String,
        @Query("appointment_id") appointmentId: Int? = null,
    ): AppointmentV1Dtos.AppointmentAvailabilityResponse

    @GET("appointments")
    suspend fun getAppointments(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): AppointmentV1Dtos.AppointmentListResponse

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") id: Int): AppointmentV1Dtos.AppointmentResponse

    @POST("appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: Int): AppointmentV1Dtos.AppointmentResponse

    @POST("appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("id") id: Int,
        @Body request: AppointmentV1Dtos.RescheduleRequest,
    ): AppointmentV1Dtos.AppointmentResponse
}
