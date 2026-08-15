package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.NotificationDtos
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("notifications")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("per_page") perPage: Int = 20,
    ): NotificationDtos.NotificationListResponse

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): NotificationDtos.UnreadCountResponse

    @PATCH("notifications/{id}/read")
    suspend fun markOneRead(
        @Path("id") notificationId: String,
    ): NotificationDtos.NotificationMessageResponse

    @PATCH("notifications/read-all")
    suspend fun markAllRead(): NotificationDtos.NotificationMessageResponse
}
