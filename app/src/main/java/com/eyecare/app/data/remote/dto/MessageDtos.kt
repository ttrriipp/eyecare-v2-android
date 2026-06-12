package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object MessageDtos {

    @Serializable
    data class ConversationDto(
        val id: Int,
        @SerialName("customer_id") val customerId: Int? = null,
        @SerialName("staff_id") val staffId: Int? = null,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("order_id") val orderId: Int? = null,
        val subject: String? = null,
        @SerialName("created_at") val createdAt: String,
    )

    @Serializable
    data class MessageDto(
        val id: Int,
        @SerialName("conversation_id") val conversationId: Int,
        @SerialName("sender_id") val senderId: Int,
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
    )

    @Serializable data class ConversationListResponse(val data: List<ConversationDto>)
    @Serializable data class ConversationResponse(val data: ConversationDto)
    @Serializable data class MessageListResponse(val data: List<MessageDto>)
    @Serializable data class MessageResponse(val data: MessageDto)

    @Serializable
    data class CreateConversationRequest(
        val body: String,
        val subject: String? = null,
        @SerialName("appointment_id") val appointmentId: Int? = null,
        @SerialName("order_id") val orderId: Int? = null,
    )

    @Serializable
    data class SendMessageRequest(val body: String)
}
