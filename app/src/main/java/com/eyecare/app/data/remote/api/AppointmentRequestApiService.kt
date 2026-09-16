package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AppointmentRequestAvailabilityResponse
import com.eyecare.app.data.remote.dto.AppointmentRequestListResponse
import com.eyecare.app.data.remote.dto.AppointmentRequestResponse
import com.eyecare.app.data.remote.dto.AppointmentTypeListResponse
import com.eyecare.app.data.remote.dto.CreateAppointmentRequest
import com.eyecare.app.data.remote.dto.CancellationReasonRequest
import com.eyecare.app.data.remote.dto.CurrentAppointmentJourneyResponse
import com.eyecare.app.data.remote.dto.UpdateAppointmentRequestScheduleRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
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

    @GET("appointment-requests/current")
    suspend fun getCurrentAppointmentJourney(): CurrentAppointmentJourneyResponse

    @POST("appointment-requests")
    suspend fun createRequest(
        @Body request: CreateAppointmentRequest,
    ): AppointmentRequestResponse

    @GET("appointment-requests/{id}")
    suspend fun getRequest(
        @Path("id") id: Int,
    ): AppointmentRequestResponse

    @PATCH("appointment-requests/{id}")
    suspend fun updateRequestSchedule(
        @Path("id") id: Int,
        @Body request: UpdateAppointmentRequestScheduleRequest,
    ): AppointmentRequestResponse

    @POST("appointment-requests/{id}/cancel")
    suspend fun cancelRequest(
        @Path("id") id: Int,
        @Body request: CancellationReasonRequest,
    ): AppointmentRequestResponse
}
