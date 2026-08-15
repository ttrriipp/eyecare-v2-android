package com.eyecare.app.data.repository

import android.content.Context
import android.net.Uri
import com.eyecare.app.data.remote.api.ConversationApiService
import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.ConversationCapabilities
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.model.MessagePage
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.domain.repository.AttachmentDownload
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

    override suspend fun getConversation(): Result<Conversation> = safeApiCall {
        api.getConversation().data.toDomain()
    }

    override suspend fun getMessages(cursor: String?): Result<MessagePage> = safeApiCall {
        api.getMessages(cursor).toDomainPage()
    }

    override suspend fun searchMessages(query: String, cursor: String?): Result<MessagePage> = safeApiCall {
        api.searchMessages(query, cursor).toDomainPage()
    }

    override suspend fun markMessagesRead(): Result<Int> = safeApiCall {
        api.markMessagesRead().markedCount
    }

    override suspend fun sendMessage(body: String): Result<Message> = safeApiCall {
        api.sendMessage(MessageDtos.SendMessageRequest(body)).data.toDomain()
    }

    override suspend fun sendFileMessage(
        body: String,
        uri: Uri,
        mimeType: String,
        fileName: String,
    ): Result<Message> = safeApiCall {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: error("Cannot open file")
        val tempFile = File.createTempFile("upload_", null, context.cacheDir)
        try {
            tempFile.outputStream().use { out -> inputStream.use { it.copyTo(out) } }
            val bodyPart = body.toRequestBody("text/plain".toMediaType())
            val filePart = MultipartBody.Part.createFormData(
                "attachment", fileName,
                tempFile.asRequestBody(mimeType.toMediaType()),
            )
            api.sendFileMessage(bodyPart, filePart).data.toDomain()
        } finally {
            tempFile.delete()
        }
    }

    override suspend fun downloadAttachment(attachmentId: Int): Result<AttachmentDownload> = safeApiCall {
        val response = api.downloadAttachment(attachmentId)
        if (!response.isSuccessful) {
            val body = response.errorBody()?.string()
            val error = com.eyecare.app.data.remote.ApiErrorDecoder.decode(response.code(), body)
            throw ApiDomainError(
                httpStatus = error.httpStatus,
                code = error.code,
                message = error.message,
                fieldErrors = error.fieldErrors,
            )
        }
        val body = response.body() ?: error("Empty attachment response")
        val bytes = body.byteStream().readBytes()
        val fileName = response.headers()["Content-Disposition"]
            ?.let { Regex("filename=\"?([^\"]+)\"?").find(it)?.groupValues?.get(1) }
            ?: "attachment_$attachmentId"
        AttachmentDownload(fileName = fileName, mimeType = body.contentType()?.toString() ?: "application/octet-stream", bytes = bytes)
    }

    private fun MessageDtos.ConversationDto.toDomain() = Conversation(
        id = id,
        patientId = patientId,
        unreadCount = unreadCount,
        createdAt = createdAt,
        accessLevel = ConversationAccessLevel.from(accessLevel),
        capabilities = capabilities?.toDomain() ?: ConversationCapabilities.SAFE_DEFAULT,
    )

    private fun MessageDtos.ConversationCapabilitiesDto.toDomain() = ConversationCapabilities(
        canUploadAttachments = canUploadAttachments,
    )

    private fun MessageDtos.MessageListResponse.toDomainPage(): MessagePage {
        if (meta.hasMore && meta.nextCursor.isNullOrBlank()) {
            error("Invalid cursor metadata")
        }
        return MessagePage(
            messages = data.map { it.toDomain() },
            nextCursor = meta.nextCursor,
            hasMore = meta.hasMore,
        )
    }
}

internal fun MessageDtos.MessageDto.toDomain() = Message(
    id = id,
    senderId = senderId,
    senderType = SenderType.from(senderType),
    body = body,
    readAt = readAt,
    createdAt = createdAt,
    attachments = attachments.map { attachment ->
        MessageAttachment(
            id = attachment.id,
            originalName = attachment.originalName,
            mimeType = attachment.mimeType,
            fileSize = attachment.fileSize,
            downloadUrl = attachment.downloadUrl,
        )
    },
)
