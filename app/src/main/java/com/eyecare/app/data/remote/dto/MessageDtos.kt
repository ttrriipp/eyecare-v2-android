package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object MessageDtos {

    @Serializable
    data class ConversationDto(
        val id: Int,
        @SerialName("patient_id") val patientId: Int? = null,
        @SerialName("unread_count") val unreadCount: Int = 0,
        @SerialName("created_at") val createdAt: String,
        @SerialName("access_level") val accessLevel: String? = null,
        val capabilities: ConversationCapabilitiesDto? = null,
    )

    @Serializable
    data class ConversationCapabilitiesDto(
        @SerialName("can_upload_attachments") val canUploadAttachments: Boolean = false,
    )

    @Serializable
    data class MessageDto(
        val id: Int,
        @SerialName("sender_id") val senderId: Int,
        @SerialName("sender_type") val senderType: String = "unknown",
        val body: String,
        @SerialName("read_at") val readAt: String? = null,
        @SerialName("created_at") val createdAt: String,
        val attachments: List<AttachmentDto> = emptyList(),
    )

    @Serializable
    data class AttachmentDto(
        val id: Int,
        @SerialName("original_name") val originalName: String,
        @SerialName("mime_type") val mimeType: String,
        @SerialName("file_size") val fileSize: Long,
        @SerialName("download_url") val downloadUrl: String = "",
    )

    @Serializable
    data class CursorMetaDto(
        @SerialName("next_cursor") val nextCursor: String?,
        @SerialName("has_more") val hasMore: Boolean,
    )

    @Serializable data class ConversationResponse(val data: ConversationDto)

    @Serializable
    data class MessageListResponse(
        val data: List<MessageDto>,
        val meta: CursorMetaDto,
    )

    @Serializable data class MessageResponse(val data: MessageDto)

    @Serializable
    data class MarkReadResponse(
        @SerialName("marked_count") val markedCount: Int = 0,
    )

    @Serializable
    data class SendMessageRequest(
        val body: String,
    )
}
