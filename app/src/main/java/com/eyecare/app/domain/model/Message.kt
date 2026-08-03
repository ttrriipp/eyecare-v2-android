package com.eyecare.app.domain.model

data class Conversation(
    val id: Int,
    val patientId: Int?,
    val unreadCount: Int,
    val createdAt: String,
)

data class Message(
    val id: Int,
    val senderId: Int,
    val senderType: SenderType,
    val body: String,
    val readAt: String?,
    val createdAt: String,
    val attachments: List<MessageAttachment>,
    val contexts: List<MessageContext> = emptyList(),
)

enum class SenderType {
    PATIENT, STAFF, UNKNOWN;

    companion object {
        fun from(value: String?): SenderType = when (value?.lowercase()) {
            "patient" -> PATIENT
            "staff", "App\\Models\\User" -> STAFF
            else -> UNKNOWN
        }
    }
}

sealed interface MessageContext {
    val id: Int

    data class Quotation(override val id: Int) : MessageContext
    data class OpticalOrder(override val id: Int) : MessageContext
    data class Unsupported(val type: String, override val id: Int) : MessageContext
}

data class MessageAttachment(
    val id: Int,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
    val downloadUrl: String?,
)
