package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.PrescriptionDtos
import retrofit2.http.GET
import retrofit2.http.Path

interface PrescriptionApiService {
    @GET("prescriptions")
    suspend fun getPrescriptions(): PrescriptionDtos.PrescriptionListResponse

    @GET("prescriptions/{id}")
    suspend fun getPrescription(@Path("id") id: Int): PrescriptionDtos.PrescriptionResponse
}
