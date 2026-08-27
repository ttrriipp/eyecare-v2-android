package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.SavedFrameDtos
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface SavedFrameApiService {

    @GET("saved-frames")
    suspend fun getSavedFrames(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 15,
    ): SavedFrameDtos.SavedFramePageResponse

    @PUT("saved-frames/{productVariantId}")
    suspend fun saveFrame(
        @Path("productVariantId") productVariantId: Int,
    ): SavedFrameDtos.SavedFrameSaveResponse

    @DELETE("saved-frames/{productVariantId}")
    suspend fun removeFrame(
        @Path("productVariantId") productVariantId: Int,
    ): Response<Unit>
}
