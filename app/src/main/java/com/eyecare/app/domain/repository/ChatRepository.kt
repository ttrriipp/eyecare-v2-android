package com.eyecare.app.domain.repository

import android.net.Uri
import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message

interface ChatRepository {
    suspend fun getConversation(): Result<Conversation>
    suspend fun getMessages(conversationId: Int): Result<List<Message>>
    suspend fun sendMessage(
        conversationId: Int,
        body: String,
        contexts: List<MessageDtos.ContextLinkDto>? = null,
    ): Result<Message>
    suspend fun sendFileMessage(conversationId: Int, uri: Uri, mimeType: String, fileName: String): Result<Message>
    suspend fun markMessagesRead(conversationId: Int): Result<Unit>
}
