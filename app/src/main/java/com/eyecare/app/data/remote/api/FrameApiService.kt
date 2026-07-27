package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.FrameDtos
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface FrameApiService {

    @GET("frames")
    suspend fun getFrames(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
        @Query("search") search: String? = null,
        @Query("brand") brandId: Int? = null,
        @Query("category") categoryId: Int? = null,
        @Query("sort") sort: String? = null,
    ): FrameDtos.PaginatedFrameResponse

    @GET("frames/{id}")
    suspend fun getFrame(@Path("id") id: Int): FrameDtos.FrameResponse
}
