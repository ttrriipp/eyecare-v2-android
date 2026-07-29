package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.EyewearDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EyewearApiService {
    @GET("eyewear")
    suspend fun getEyewear(
        @Query("filter") filter: String = "current",
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): EyewearDtos.EyewearListResponse

    @GET("eyewear/{key}")
    suspend fun getEyewearDetail(@Path("key") key: String): EyewearDtos.EyewearDetailResponse
}
