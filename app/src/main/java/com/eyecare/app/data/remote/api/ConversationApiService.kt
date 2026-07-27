package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.MessageDtos
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ConversationApiService {
    @GET("conversation")
    suspend fun getConversation(): MessageDtos.ConversationResponse

    @GET("conversation/messages")
    suspend fun getMessages(): MessageDtos.MessageListResponse

    @POST("conversation/messages")
    suspend fun sendMessage(@Body request: MessageDtos.SendMessageRequest): MessageDtos.MessageResponse

    @Multipart
    @POST("conversation/messages")
    suspend fun sendFileMessage(
        @Part("body") body: RequestBody,
        @Part file: MultipartBody.Part,
    ): MessageDtos.MessageResponse
}
