package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.PrescriptionDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PrescriptionApiService {
    @GET("prescriptions")
    suspend fun getPrescriptions(
        @Query("per_page") perPage: Int = 15,
        @Query("page") page: Int = 1,
    ): PrescriptionDtos.PrescriptionListResponse

    @GET("prescriptions/{id}")
    suspend fun getPrescription(@Path("id") id: Int): PrescriptionDtos.PrescriptionResponse
}
