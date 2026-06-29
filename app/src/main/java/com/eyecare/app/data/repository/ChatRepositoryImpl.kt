package com.eyecare.app.data.repository

import android.content.Context
import android.net.Uri
import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.repository.ChatRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val api: ConversationApiService,
    @param:ApplicationContext private val context: Context,
) : ChatRepository {

    override suspend fun getConversation(): Result<Conversation> = runCatching {
        api.getConversations().data.toDomain()
    }

    override suspend fun getMessages(conversationId: Int): Result<List<Message>> = runCatching {
        api.getMessages(conversationId).data.map { it.toDomain() }
    }

    override suspend fun sendMessage(
        conversationId: Int,
        body: String,
        contexts: List<MessageDtos.ContextLinkDto>?,
    ): Result<Message> = runCatching {
        api.sendMessage(conversationId, MessageDtos.SendMessageRequest(body, contexts)).data.toDomain()
    }

    override suspend fun sendFileMessage(
        conversationId: Int,
        uri: Uri,
        mimeType: String,
        fileName: String,
    ): Result<Message> = runCatching {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open file")
        val tempFile = File.createTempFile("upload_", null, context.cacheDir)
        try {
            tempFile.outputStream().use { out -> inputStream.use { it.copyTo(out) } }
            val bodyPart = "Attachment".toRequestBody("text/plain".toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                "attachment", fileName,
                tempFile.asRequestBody(mimeType.toMediaType()),
            )
            api.sendFileMessage(conversationId, bodyPart, filePart).data.toDomain()
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun markMessagesRead(conversationId: Int): Result<Unit> = runCatching {
        api.markMessagesRead(conversationId)
    }

    private fun MessageDtos.ConversationDto.toDomain() = Conversation(
        id = id, customerId = customerId, unreadCount = unreadCount, createdAt = createdAt,
    )

    private fun MessageDtos.MessageDto.toDomain() = Message(
        id = id, conversationId = conversationId, senderId = senderId,
        body = body, readAt = readAt, createdAt = createdAt,
        attachments = attachments.map { a -> MessageAttachment(a.id, a.originalName, a.mimeType, a.fileSize) },
    )
}
