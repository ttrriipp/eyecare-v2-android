package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.MessageContext
import com.eyecare.app.domain.model.SenderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class ChatRepositoryMappingsTest {

    @Test
    fun `message contexts map to typed domain variants`() {
        val message = MessageDtos.MessageDto(
            id = 10,
            senderId = 2,
            senderType = "patient",
            body = "Linked records",
            createdAt = "2026-07-23T10:00:00Z",
            contexts = listOf(
                MessageDtos.ContextLinkDto("quotation", 7),
                MessageDtos.ContextLinkDto("optical_order", 12),
                MessageDtos.ContextLinkDto("product", 20),
            ),
        ).toDomain()

        assertInstanceOf(MessageContext.Quotation::class.java, message.contexts[0])
        assertEquals(7, message.contexts[0].id)
        assertInstanceOf(MessageContext.OpticalOrder::class.java, message.contexts[1])
        assertEquals(12, message.contexts[1].id)
        assertEquals(MessageContext.Unsupported("product", 20), message.contexts[2])
    }

    @Test
    fun `sender type maps correctly`() {
        val patient = MessageDtos.MessageDto(
            id = 1, senderId = 1, senderType = "patient", body = "Hi", createdAt = "2026-08-01T00:00:00Z",
        ).toDomain()
        assertEquals(SenderType.PATIENT, patient.senderType)

        val staff = MessageDtos.MessageDto(
            id = 2, senderId = 2, senderType = "staff", body = "Hello", createdAt = "2026-08-01T00:00:00Z",
        ).toDomain()
        assertEquals(SenderType.STAFF, staff.senderType)

        val unknown = MessageDtos.MessageDto(
            id = 3, senderId = 3, senderType = null, body = "?", createdAt = "2026-08-01T00:00:00Z",
        ).toDomain()
        assertEquals(SenderType.UNKNOWN, unknown.senderType)
    }

    @Test
    fun `attachment download url passes through`() {
        val message = MessageDtos.MessageDto(
            id = 4, senderId = 1, senderType = "patient", body = "file", createdAt = "2026-08-01T00:00:00Z",
            attachments = listOf(MessageDtos.AttachmentDto(1, "photo.jpg", "image/jpeg", 1024, "https://example.com/photo.jpg")),
        ).toDomain()
        assertEquals("https://example.com/photo.jpg", message.attachments[0].downloadUrl)
    }
}
