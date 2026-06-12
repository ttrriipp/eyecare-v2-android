package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.repository.ChatRepository
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ConversationApiService,
) : ChatRepository {

    override suspend fun getOrCreateConversation(): Result<Conversation> = runCatching {
        val existing = api.getConversations().data
        if (existing.isNotEmpty()) {
            existing.first().toDomain()
        } else {
            api.createConversation(MessageDtos.CreateConversationRequest(body = "Hello")).data.toDomain()
        }
    }

    override suspend fun getMessages(conversationId: Int): Result<List<Message>> = runCatching {
        api.getMessages(conversationId).data.map { it.toDomain() }
    }

    override suspend fun sendMessage(conversationId: Int, body: String): Result<Message> = runCatching {
        api.sendMessage(conversationId, MessageDtos.SendMessageRequest(body)).data.toDomain()
    }

    private fun MessageDtos.ConversationDto.toDomain() = Conversation(
        id = id, appointmentId = appointmentId, orderId = orderId,
        subject = subject, createdAt = createdAt,
    )

    private fun MessageDtos.MessageDto.toDomain() = Message(
        id = id, conversationId = conversationId, senderId = senderId,
        body = body, readAt = readAt, createdAt = createdAt,
        attachments = attachments.map { a ->
            MessageAttachment(a.id, a.originalName, a.mimeType, a.fileSize)
        },
    )
}
