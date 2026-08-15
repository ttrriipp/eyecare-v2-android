package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `conversation decodes linked_patient access level and capabilities`() {
        val response = json.decodeFromString<MessageDtos.ConversationResponse>(
            """
            {
              "data": {
                "id": 1,
                "patient_id": 42,
                "unread_count": 3,
                "created_at": "2026-07-23T10:00:00Z",
                "access_level": "linked_patient",
                "capabilities": {
                  "can_upload_attachments": true
                }
              }
            }
            """.trimIndent(),
        )

        val conversation = response.data
        assertEquals(1, conversation.id)
        assertEquals(42, conversation.patientId)
        assertEquals(3, conversation.unreadCount)
        assertEquals("linked_patient", conversation.accessLevel)
        assertTrue(conversation.capabilities?.canUploadAttachments == true)
    }

    @Test
    fun `conversation decodes general_inquiry access level with upload denied`() {
        val response = json.decodeFromString<MessageDtos.ConversationResponse>(
            """
            {
              "data": {
                "id": 2,
                "patient_id": null,
                "unread_count": 0,
                "created_at": "2026-07-23T10:00:00Z",
                "access_level": "general_inquiry",
                "capabilities": {
                  "can_upload_attachments": false
                }
              }
            }
            """.trimIndent(),
        )

        val conversation = response.data
        assertEquals("general_inquiry", conversation.accessLevel)
        assertFalse(conversation.capabilities?.canUploadAttachments == true)
    }

    @Test
    fun `conversation with missing access_level defaults to null`() {
        val response = json.decodeFromString<MessageDtos.ConversationResponse>(
            """
            {
              "data": {
                "id": 3,
                "patient_id": null,
                "unread_count": 0,
                "created_at": "2026-07-23T10:00:00Z"
              }
            }
            """.trimIndent(),
        )

        assertNull(response.data.accessLevel)
        assertNull(response.data.capabilities)
    }

    @Test
    fun `message list decodes without contexts field`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 10,
                "conversation_id": 1,
                "sender_id": 2,
                "body": "Hello",
                "read_at": null,
                "created_at": "2026-07-23T10:00:00Z",
                "attachments": []
              }]
            }
            """.trimIndent(),
        )

        val message = response.data.single()
        assertEquals(10, message.id)
        assertEquals("Hello", message.body)
    }

    @Test
    fun `message with legacy contexts field is ignored by deserializer`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 10,
                "conversation_id": 1,
                "sender_id": 2,
                "body": "Appointment details",
                "read_at": null,
                "created_at": "2026-07-23T10:00:00Z",
                "attachments": [],
                "contexts": [
                  { "type": "appointment", "id": 7 }
                ]
              }]
            }
            """.trimIndent(),
        )

        val message = response.data.single()
        assertEquals(10, message.id)
        assertEquals("Appointment details", message.body)
    }

    @Test
    fun `send message request serializes body only`() {
        val request = MessageDtos.SendMessageRequest(body = "Hello")
        val encoded = json.encodeToString(MessageDtos.SendMessageRequest.serializer(), request)

        assertTrue(encoded.contains("\"body\":\"Hello\""))
        assertFalse(encoded.contains("contexts"))
    }

    // --- C1: Cursor metadata tests ---

    @Test
    fun `message list decodes terminal cursor metadata`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 1,
                "sender_id": 5,
                "sender_type": "staff",
                "body": "Hello",
                "read_at": null,
                "created_at": "2026-08-01T09:00:00Z",
                "attachments": []
              }],
              "meta": {
                "next_cursor": null,
                "has_more": false
              }
            }
            """.trimIndent(),
        )

        assertNull(response.meta.nextCursor)
        assertFalse(response.meta.hasMore)
    }

    @Test
    fun `message list decodes non-terminal cursor metadata`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 50,
                "sender_id": 5,
                "sender_type": "staff",
                "body": "Earlier",
                "read_at": null,
                "created_at": "2026-07-15T08:00:00Z",
                "attachments": []
              }],
              "meta": {
                "next_cursor": "eyJpZCI6NTB9",
                "has_more": true
              }
            }
            """.trimIndent(),
        )

        assertEquals("eyJpZCI6NTB9", response.meta.nextCursor)
        assertTrue(response.meta.hasMore)
    }

    @Test
    fun `message list defaults cursor metadata when meta is absent`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 1,
                "sender_id": 1,
                "sender_type": "patient",
                "body": "Hi",
                "read_at": null,
                "created_at": "2026-08-01T00:00:00Z",
                "attachments": []
              }]
            }
            """.trimIndent(),
        )

        assertNull(response.meta.nextCursor)
        assertFalse(response.meta.hasMore)
    }

    @Test
    fun `cursor remains opaque string and is not decoded by DTO`() {
        val opaqueCursor = "eyJpZCI6NTAsImNyZWF0ZWRfYXQiOiIyMDI2LTA3LTE1VDA4OjAwOjAwKzA4OjAwIn0="
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [],
              "meta": {
                "next_cursor": "$opaqueCursor",
                "has_more": true
              }
            }
            """.trimIndent(),
        )

        assertEquals(opaqueCursor, response.meta.nextCursor)
        // Cursor is stored as opaque string; DTO does not decode it
        assertTrue(response.meta.nextCursor!!.contains("="))
    }

    // --- Mark-read response ---

    @Test
    fun `mark read response decodes marked_count`() {
        val response = json.decodeFromString<MessageDtos.MarkReadResponse>(
            """
            { "marked_count": 5 }
            """.trimIndent(),
        )

        assertEquals(5, response.markedCount)
    }

    @Test
    fun `mark read response defaults to zero when field missing`() {
        val response = json.decodeFromString<MessageDtos.MarkReadResponse>(
            """
            {}
            """.trimIndent(),
        )

        assertEquals(0, response.markedCount)
    }

    // --- Sender type default ---

    @Test
    fun `message with missing sender_type defaults to unknown`() {
        val response = json.decodeFromString<MessageDtos.MessageListResponse>(
            """
            {
              "data": [{
                "id": 1,
                "sender_id": 1,
                "body": "Hi",
                "read_at": null,
                "created_at": "2026-08-01T00:00:00Z",
                "attachments": []
              }]
            }
            """.trimIndent(),
        )

        assertEquals("unknown", response.data.single().senderType)
    }
}
