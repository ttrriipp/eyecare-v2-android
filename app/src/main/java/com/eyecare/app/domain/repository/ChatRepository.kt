package com.eyecare.app.domain.repository

import android.net.Uri
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message

interface ChatRepository {
    suspend fun getOrCreateConversation(): Result<Conversation>
    suspend fun getMessages(conversationId: Int): Result<List<Message>>
    suspend fun sendMessage(conversationId: Int, body: String): Result<Message>
    suspend fun sendFileMessage(conversationId: Int, uri: Uri, mimeType: String, fileName: String): Result<Message>
    suspend fun sendContextMessage(conversationId: Int, body: String, appointmentId: Int? = null, orderId: Int? = null): Result<Message>
}
