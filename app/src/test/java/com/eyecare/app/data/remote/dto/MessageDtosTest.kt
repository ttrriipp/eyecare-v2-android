package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageDtosTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun `message list decodes context links`() {
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
                  { "type": "appointment", "id": 7 },
                  { "type": "order", "id": 12 }
                ]
              }]
            }
            """.trimIndent(),
        )

        assertEquals(
            listOf(
                MessageDtos.ContextLinkDto("appointment", 7),
                MessageDtos.ContextLinkDto("order", 12),
            ),
            response.data.single().contexts,
        )
    }

    @Test
    fun `message without contexts defaults to an empty list`() {
        val message = json.decodeFromString<MessageDtos.MessageDto>(
            """
            {
              "id": 10,
              "conversation_id": 1,
              "sender_id": 2,
              "body": "Hello",
              "created_at": "2026-07-23T10:00:00Z"
            }
            """.trimIndent(),
        )

        assertTrue(message.contexts.isEmpty())
    }
}
