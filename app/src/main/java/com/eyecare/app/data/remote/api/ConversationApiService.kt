package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.MessageDtos
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ConversationApiService {
    @GET("conversations")
    suspend fun getConversations(): MessageDtos.ConversationResponse

    @GET("conversations/{id}/messages")
    suspend fun getMessages(@Path("id") id: Int): MessageDtos.MessageListResponse

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: Int, @Body request: MessageDtos.SendMessageRequest): MessageDtos.MessageResponse

    @POST("conversations/{id}/messages/read")
    suspend fun markMessagesRead(@Path("id") id: Int)

    @Multipart
    @POST("conversations/{id}/messages")
    suspend fun sendFileMessage(
        @Path("id") id: Int,
        @Part("body") body: RequestBody,
        @Part file: MultipartBody.Part,
    ): MessageDtos.MessageResponse
}
