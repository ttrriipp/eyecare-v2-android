package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityResponse
import com.eyecare.app.data.remote.dto.AppointmentRequestListResponse
import com.eyecare.app.data.remote.dto.AppointmentRequestResponse
import com.eyecare.app.data.remote.dto.AppointmentTypeListResponse
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface AppointmentRequestApiService {

    @GET("appointment-types")
    suspend fun getAppointmentTypes(): AppointmentTypeListResponse

    @GET("appointment-request-availability")
    suspend fun getAvailability(
        @Query("date") date: String,
        @Query("appointment_type_id") appointmentTypeId: Int,
    ): AppointmentRequestAvailabilityResponse

    @GET("appointment-requests")
    suspend fun getRequests(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): AppointmentRequestListResponse

    @POST("appointment-requests")
    suspend fun createRequest(
        @Body request: CreateAppointmentRequest,
    ): AppointmentRequestResponse

    @GET("appointment-requests/{id}")
    suspend fun getRequest(
        @Path("id") id: Int,
    ): AppointmentRequestResponse

    @POST("appointment-requests/{id}/cancel")
    suspend fun cancelRequest(
        @Path("id") id: Int,
    ): AppointmentRequestResponse
}
