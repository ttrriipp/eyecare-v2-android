package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.PatientIntakeDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface PatientIntakeApiService {
    @GET("appointments/{id}/intake")
    suspend fun getIntake(@Path("id") appointmentId: Int): PatientIntakeDtos.PatientIntakeResponse

    @PUT("appointments/{id}/intake")
    suspend fun saveIntake(
        @Path("id") appointmentId: Int,
        @Body request: PatientIntakeDtos.SaveIntakeRequest,
    ): PatientIntakeDtos.PatientIntakeResponse

    @POST("appointments/{id}/intake/submit")
    suspend fun submitIntake(@Path("id") appointmentId: Int): PatientIntakeDtos.PatientIntakeResponse
}
