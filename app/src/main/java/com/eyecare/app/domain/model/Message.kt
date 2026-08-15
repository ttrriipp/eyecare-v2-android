package com.eyecare.app.domain.model

data class Conversation(
    val id: Int,
    val patientId: Int?,
    val unreadCount: Int,
    val createdAt: String,
    val accessLevel: ConversationAccessLevel,
    val capabilities: ConversationCapabilities,
)

enum class ConversationAccessLevel {
    LINKED_PATIENT,
    GENERAL_INQUIRY,
    UNKNOWN;

    companion object {
        fun from(value: String?): ConversationAccessLevel = when (value?.lowercase()) {
            "linked_patient" -> LINKED_PATIENT
            "general_inquiry" -> GENERAL_INQUIRY
            else -> UNKNOWN
        }
    }
}

data class ConversationCapabilities(
    val canUploadAttachments: Boolean,
) {
    companion object {
        val SAFE_DEFAULT = ConversationCapabilities(canUploadAttachments = false)
    }
}

data class Message(
    val id: Int,
    val senderId: Int,
    val senderType: SenderType,
    val body: String,
    val readAt: String?,
    val createdAt: String,
    val attachments: List<MessageAttachment>,
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

data class MessageAttachment(
    val id: Int,
    val originalName: String,
    val mimeType: String,
    val fileSize: Long,
    val downloadUrl: String,
)

data class MessagePage(
    val messages: List<Message>,
    val nextCursor: String?,
    val hasMore: Boolean,
)
