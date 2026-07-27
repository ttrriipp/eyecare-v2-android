package com.eyecare.app.domain.model

data class Conversation(
    val id: Int,
    val patientId: Int?,
    val unreadCount: Int,
    val createdAt: String,
)

data class Message(
    val id: Int,
    val conversationId: Int,
    val senderId: Int,
    val body: String,
    val readAt: String?,
    val createdAt: String,
    val attachments: List<MessageAttachment>,
    val contexts: List<MessageContext> = emptyList(),
)

sealed interface MessageContext {
    val id: Int

    data class Appointment(override val id: Int) : MessageContext
    data class Order(override val id: Int) : MessageContext
    data class Unsupported(val type: String, override val id: Int) : MessageContext
}

data class MessageAttachment(
    val id: Int,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
)
