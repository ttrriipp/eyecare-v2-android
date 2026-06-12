package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.FeedbackDtos
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface FeedbackApiService {
    @POST("feedback")
    suspend fun submitFeedback(@Body request: FeedbackDtos.SubmitFeedbackRequest): FeedbackDtos.FeedbackResponse

    @GET("feedback")
    suspend fun getFeedbackHistory(): FeedbackDtos.FeedbackListResponse
}
