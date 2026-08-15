package com.eyecare.app.domain.repository

import com.eyecare.app.domain.model.NotificationPage

interface NotificationRepository {
    suspend fun getNotifications(page: Int = 1): Result<NotificationPage>
    suspend fun getUnreadCount(): Result<Int>
    suspend fun markOneRead(notificationId: String): Result<Unit>
    suspend fun markAllRead(): Result<Unit>
}
