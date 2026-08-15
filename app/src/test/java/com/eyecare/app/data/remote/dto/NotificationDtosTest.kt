package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NotificationDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    // --- UUID string ID preservation ---

    @Test
    fun `notification list preserves UUID string IDs`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertEquals("550e8400-e29b-41d4-a716-446655440000", response.data[0].id)
        assertEquals("6ba7b810-9dad-11d1-80b4-00c04fd430c8", response.data[1].id)
    }

    @Test
    fun `notification ID is never coerced to integer`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        response.data.forEach { notification ->
            assertEquals(36, notification.id.length, "UUID should be 36 chars")
            assert(notification.id.contains("-")) { "UUID should contain hyphens" }
        }
    }

    // --- Stable kind mapping ---

    @Test
    fun `notification decodes stable kind field`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertEquals("new_message", response.data[0].kind)
        assertEquals("unknown_future_kind", response.data[1].kind)
    }

    @Test
    fun `notification with missing kind defaults to unknown`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            """
            {
              "data": [{
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "title": "Test",
                "body": "Body",
                "created_at": "2026-08-15T10:00:00+08:00"
              }]
            }
            """.trimIndent(),
        )

        assertEquals("unknown", response.data[0].kind)
    }

    // --- Mobile action mapping ---

    @Test
    fun `notification decodes mobile_action with conversation type`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertNotNull(response.data[0].mobileAction)
        assertEquals("conversation", response.data[0].mobileAction!!.type)
    }

    @Test
    fun `notification with null mobile_action decodes to null`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertNull(response.data[1].mobileAction)
    }

    @Test
    fun `mobile_action with optional id field decodes correctly`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            """
            {
              "data": [{
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "kind": "new_message",
                "title": "Test",
                "body": "Body",
                "mobile_action": { "type": "conversation", "id": 42 },
                "created_at": "2026-08-15T10:00:00+08:00"
              }]
            }
            """.trimIndent(),
        )

        assertEquals("conversation", response.data[0].mobileAction!!.type)
        assertEquals(42, response.data[0].mobileAction!!.id)
    }

    @Test
    fun `mobile_action with unknown type decodes as-is`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            """
            {
              "data": [{
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "kind": "some_other_kind",
                "title": "Test",
                "body": "Body",
                "mobile_action": { "type": "unknown_future_action" },
                "created_at": "2026-08-15T10:00:00+08:00"
              }]
            }
            """.trimIndent(),
        )

        assertEquals("unknown_future_action", response.data[0].mobileAction!!.type)
    }

    // --- Legacy fields ignored ---

    @Test
    fun `legacy type action_url related_type related_id fields are ignored`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationWithLegacyFields,
        )

        val notification = response.data[0]
        assertEquals("a1b2c3d4-e5f6-7890-abcd-ef1234567890", notification.id)
        assertEquals("new_message", notification.kind)
        assertEquals("conversation", notification.mobileAction!!.type)
        // Legacy fields do not affect kind or action mapping
    }

    @Test
    fun `legacy PHP class type field does not leak into kind`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            """
            {
              "data": [{
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "type": "App\\Notifications\\NewMessageReceived",
                "kind": "new_message",
                "title": "Test",
                "body": "Body",
                "action_url": "/admin/conversations/1",
                "mobile_action": { "type": "conversation" },
                "related_type": "App\\Models\\Conversation",
                "related_id": 1,
                "created_at": "2026-08-15T10:00:00+08:00"
              }]
            }
            """.trimIndent(),
        )

        assertEquals("new_message", response.data[0].kind)
        assertEquals("conversation", response.data[0].mobileAction!!.type)
    }

    // --- Page metadata ---

    @Test
    fun `notification list decodes page metadata`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        val meta = response.meta!!
        assertEquals(1, meta.currentPage)
        assertEquals(1, meta.lastPage)
        assertEquals(20, meta.perPage)
        assertEquals(2, meta.total)
    }

    @Test
    fun `notification list decodes links`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertNotNull(response.links)
    }

    @Test
    fun `notification list with null meta and links decodes safely`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            """
            {
              "data": [{
                "id": "550e8400-e29b-41d4-a716-446655440000",
                "kind": "new_message",
                "title": "Test",
                "body": "Body",
                "created_at": "2026-08-15T10:00:00+08:00"
              }]
            }
            """.trimIndent(),
        )

        assertNull(response.meta)
        assertNull(response.links)
        assertEquals(1, response.data.size)
    }

    // --- Unread count ---

    @Test
    fun `unread count response decodes correctly`() {
        val response = json.decodeFromString<NotificationDtos.UnreadCountResponse>(
            ApiContractFixtures.notificationUnreadCount,
        )

        assertEquals(3, response.unreadCount)
    }

    @Test
    fun `unread count defaults to zero when field missing`() {
        val response = json.decodeFromString<NotificationDtos.UnreadCountResponse>("{}")

        assertEquals(0, response.unreadCount)
    }

    // --- Mark-one / mark-all response ---

    @Test
    fun `mark one read response decodes message`() {
        val response = json.decodeFromString<NotificationDtos.NotificationMessageResponse>(
            ApiContractFixtures.notificationMarkOneResponse,
        )

        assertEquals("Notification marked as read.", response.message)
    }

    @Test
    fun `mark all read response decodes message`() {
        val response = json.decodeFromString<NotificationDtos.NotificationMessageResponse>(
            ApiContractFixtures.notificationMarkAllResponse,
        )

        assertEquals("All notifications marked as read.", response.message)
    }

    @Test
    fun `notification message response defaults to empty string`() {
        val response = json.decodeFromString<NotificationDtos.NotificationMessageResponse>("{}")

        assertEquals("", response.message)
    }

    // --- Read/created timestamps ---

    @Test
    fun `notification decodes read_at and created_at timestamps`() {
        val response = json.decodeFromString<NotificationDtos.NotificationListResponse>(
            ApiContractFixtures.notificationList,
        )

        assertNull(response.data[0].readAt)
        assertEquals("2026-08-15T10:00:00+08:00", response.data[0].createdAt)
        assertEquals("2026-08-14T09:00:00+08:00", response.data[1].readAt)
        assertEquals("2026-08-14T08:00:00+08:00", response.data[1].createdAt)
    }
}
