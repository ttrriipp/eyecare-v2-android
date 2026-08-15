package com.eyecare.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object NotificationDtos {

    @Serializable
    data class NotificationDto(
        val id: String,
        val kind: String = "unknown",
        val title: String = "",
        val body: String = "",
        @SerialName("mobile_action") val mobileAction: MobileActionDto? = null,
        @SerialName("read_at") val readAt: String? = null,
        @SerialName("created_at") val createdAt: String = "",
    )

    @Serializable
    data class MobileActionDto(
        val type: String,
        val id: Int? = null,
    )

    @Serializable
    data class NotificationListResponse(
        val data: List<NotificationDto>,
        val links: PaginationLinks? = null,
        val meta: PaginationMeta? = null,
    )

    @Serializable
    data class UnreadCountResponse(@SerialName("unread_count") val unreadCount: Int = 0)

    @Serializable
    data class NotificationMessageResponse(val message: String = "")
}
