package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AppointmentDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PATCH
import retrofit2.http.Query

interface AppointmentApiService {
    @GET("appointments")
    suspend fun getAppointments(): AppointmentDtos.AppointmentListResponse

    @GET("appointments/{id}")
    suspend fun getAppointment(@Path("id") id: Int): AppointmentDtos.AppointmentResponse

    @GET("appointments/availability")
    suspend fun getAppointmentAvailability(
        @Query("date") date: String,
        @Query("visit_reason_id") visitReasonId: Int,
        @Query("appointment_id") appointmentId: Int? = null,
    ): AppointmentDtos.AppointmentAvailabilityResponse

    @POST("appointments")
    suspend fun createAppointment(@Body request: AppointmentDtos.CreateAppointmentRequest): AppointmentDtos.AppointmentResponse

    @POST("appointments/{id}/cancel")
    suspend fun cancelAppointment(@Path("id") id: Int): AppointmentDtos.AppointmentResponse

    @POST("appointments/{id}/reschedule")
    suspend fun rescheduleAppointment(
        @Path("id") id: Int,
        @Body request: AppointmentDtos.RescheduleRequest,
    ): AppointmentDtos.AppointmentResponse

    @PATCH("appointments/{id}/contact-note")
    suspend fun updateAppointmentContactNote(
        @Path("id") id: Int,
        @Body request: AppointmentDtos.UpdateContactNoteRequest,
    ): AppointmentDtos.AppointmentResponse

    @GET("visit-reasons")
    suspend fun getVisitReasons(): AppointmentDtos.VisitReasonListResponse
}
