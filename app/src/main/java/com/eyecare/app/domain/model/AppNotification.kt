package com.eyecare.app.domain.model

data class AppNotification(
    val id: String,
    val kind: NotificationKind,
    val title: String,
    val body: String,
    val mobileAction: MobileDestination?,
    val readAt: String?,
    val createdAt: String,
)

enum class NotificationKind {
    NEW_MESSAGE, UNKNOWN;

    companion object {
        fun from(value: String?): NotificationKind = when (value) {
            "new_message" -> NEW_MESSAGE
            else -> UNKNOWN
        }
    }
}

enum class MobileDestination {
    CONVERSATION, UNKNOWN;

    companion object {
        fun from(value: String?): MobileDestination = when (value) {
            "conversation" -> CONVERSATION
            else -> UNKNOWN
        }
    }
}

data class NotificationPage(
    val notifications: List<AppNotification>,
    val currentPage: Int,
    val lastPage: Int,
    val total: Int,
)
