package com.eyecare.app.domain.model

data class Conversation(
    val id: Int,
    val appointmentId: Int?,
    val orderId: Int?,
    val subject: String?,
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
)

data class MessageAttachment(
    val id: Int,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
)
