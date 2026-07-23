package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.MessageContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ChatRepositoryMappingsTest {

    @Test
    fun `message contexts map to typed domain variants`() {
        val message = MessageDtos.MessageDto(
            id = 10,
            conversationId = 1,
            senderId = 2,
            body = "Linked records",
            createdAt = "2026-07-23T10:00:00Z",
            contexts = listOf(
                MessageDtos.ContextLinkDto("App\\Models\\Appointment", 7),
                MessageDtos.ContextLinkDto("App\\Models\\Order", 12),
                MessageDtos.ContextLinkDto("product", 20),
            ),
        ).toDomain()

        assertInstanceOf(MessageContext.Appointment::class.java, message.contexts[0])
        assertEquals(7, message.contexts[0].id)
        assertInstanceOf(MessageContext.Order::class.java, message.contexts[1])
        assertEquals(12, message.contexts[1].id)
        assertEquals(MessageContext.Unsupported("product", 20), message.contexts[2])
    }
}
