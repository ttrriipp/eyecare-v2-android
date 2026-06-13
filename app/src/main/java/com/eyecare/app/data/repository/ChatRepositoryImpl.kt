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
    @ApplicationContext private val context: Context,
) : ChatRepository {

    override suspend fun getOrCreateConversation(): Result<Conversation> = runCatching {
        val existing = api.getConversations().data
        if (existing.isNotEmpty()) existing.first().toDomain()
        else api.createConversation(MessageDtos.CreateConversationRequest(body = "Hello")).data.toDomain()
    }

    override suspend fun getMessages(conversationId: Int): Result<List<Message>> = runCatching {
        api.getMessages(conversationId).data.map { it.toDomain() }
    }

    override suspend fun sendMessage(conversationId: Int, body: String): Result<Message> = runCatching {
        api.sendMessage(conversationId, MessageDtos.SendMessageRequest(body)).data.toDomain()
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
        tempFile.outputStream().use { out -> inputStream.use { it.copyTo(out) } }

        val bodyPart = "Attachment".toRequestBody("text/plain".toMediaType())
        val filePart = MultipartBody.Part.createFormData(
            "attachment", fileName,
            tempFile.asRequestBody(mimeType.toMediaType()),
        )
        api.sendFileMessage(conversationId, bodyPart, filePart).data.toDomain()
            .also { tempFile.delete() }
    }

    override suspend fun sendContextMessage(
        conversationId: Int,
        body: String,
        appointmentId: Int?,
        orderId: Int?,
    ): Result<Message> = runCatching {
        // Send as a regular message — the body carries the context reference text.
        // The conversation was optionally created with appointmentId/orderId already.
        api.sendMessage(conversationId, MessageDtos.SendMessageRequest(body)).data.toDomain()
    }

    private fun MessageDtos.ConversationDto.toDomain() = Conversation(
        id = id, appointmentId = appointmentId, orderId = orderId,
        subject = subject, createdAt = createdAt,
    )

    private fun MessageDtos.MessageDto.toDomain() = Message(
        id = id, conversationId = conversationId, senderId = senderId,
        body = body, readAt = readAt, createdAt = createdAt,
        attachments = attachments.map { a -> MessageAttachment(a.id, a.originalName, a.mimeType, a.fileSize) },
    )
}
