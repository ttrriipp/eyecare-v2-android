package com.eyecare.app.data.repository

import android.content.Context
import android.net.Uri
import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.model.MessageContext
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
        api.getConversation().data.toDomain()
    }

    override suspend fun getMessages(): Result<List<Message>> = runCatching {
        api.getMessages().data.map { it.toDomain() }
    }

    override suspend fun sendMessage(
        body: String,
        contexts: List<MessageDtos.ContextLinkDto>?,
    ): Result<Message> = runCatching {
        api.sendMessage(MessageDtos.SendMessageRequest(body, contexts)).data.toDomain()
    }

    override suspend fun sendFileMessage(
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
            api.sendFileMessage(bodyPart, filePart).data.toDomain()
        } finally {
            tempFile.delete()
        }
    }

    private fun MessageDtos.ConversationDto.toDomain() = Conversation(
        id = id, patientId = patientId, unreadCount = unreadCount, createdAt = createdAt,
    )
}

internal fun MessageDtos.MessageDto.toDomain() = Message(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    body = body,
    readAt = readAt,
    createdAt = createdAt,
    attachments = attachments.map { attachment ->
        MessageAttachment(
            id = attachment.id,
            originalName = attachment.originalName,
            mimeType = attachment.mimeType,
            fileSize = attachment.fileSize,
        )
    },
    contexts = contexts.map { context ->
        when (context.type.substringAfterLast('\\').lowercase()) {
            "appointment" -> MessageContext.Appointment(context.id)
            "order" -> MessageContext.Order(context.id)
            else -> MessageContext.Unsupported(context.type, context.id)
        }
    },
)
