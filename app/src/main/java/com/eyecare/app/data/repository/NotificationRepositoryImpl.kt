package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.NotificationApiService
import com.eyecare.app.data.remote.dto.NotificationDtos
import com.eyecare.app.domain.model.AppNotification
import com.eyecare.app.domain.model.MobileDestination
import com.eyecare.app.domain.model.NotificationKind
import com.eyecare.app.domain.model.NotificationPage
import com.eyecare.app.domain.repository.NotificationRepository
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val api: NotificationApiService,
) : NotificationRepository {

    override suspend fun getNotifications(page: Int): Result<NotificationPage> = safeApiCall {
        val response = api.getNotifications(page = page)
        NotificationPage(
            notifications = response.data.map { it.toDomain() },
            currentPage = response.meta?.currentPage ?: 1,
            lastPage = response.meta?.lastPage ?: 1,
            total = response.meta?.total ?: response.data.size,
        )
    }

    override suspend fun getUnreadCount(): Result<Int> = safeApiCall {
        api.getUnreadCount().unreadCount
    }

    override suspend fun markOneRead(notificationId: String): Result<Unit> = safeApiCall {
        api.markOneRead(notificationId)
        Unit
    }

    override suspend fun markAllRead(): Result<Unit> = safeApiCall {
        api.markAllRead()
        Unit
    }

    private fun NotificationDtos.NotificationDto.toDomain() = AppNotification(
        id = id,
        kind = NotificationKind.from(kind),
        title = title,
        body = body,
        mobileAction = mobileAction?.let { MobileDestination.from(it.type) },
        readAt = readAt,
        createdAt = createdAt,
    )
}
