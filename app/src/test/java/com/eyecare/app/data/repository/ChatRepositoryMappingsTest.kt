package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.dto.MessageDtos
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.SenderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChatRepositoryMappingsTest {

    @Test
    fun `conversation maps linked_patient access level`() {
        val dto = MessageDtos.ConversationDto(
            id = 1,
            patientId = 42,
            unreadCount = 0,
            createdAt = "2026-08-01T00:00:00Z",
            accessLevel = "linked_patient",
            capabilities = MessageDtos.ConversationCapabilitiesDto(canUploadAttachments = true),
        )

        val domain = dto.toDomain()
        assertEquals(ConversationAccessLevel.LINKED_PATIENT, domain.accessLevel)
        assertTrue(domain.capabilities.canUploadAttachments)
    }

    @Test
    fun `conversation maps general_inquiry access level`() {
        val dto = MessageDtos.ConversationDto(
            id = 2,
            patientId = null,
            unreadCount = 0,
            createdAt = "2026-08-01T00:00:00Z",
            accessLevel = "general_inquiry",
            capabilities = MessageDtos.ConversationCapabilitiesDto(canUploadAttachments = false),
        )

        val domain = dto.toDomain()
        assertEquals(ConversationAccessLevel.GENERAL_INQUIRY, domain.accessLevel)
        assertFalse(domain.capabilities.canUploadAttachments)
    }

    @Test
    fun `conversation with null access_level fails closed to UNKNOWN`() {
        val dto = MessageDtos.ConversationDto(
            id = 3,
            patientId = null,
            unreadCount = 0,
            createdAt = "2026-08-01T00:00:00Z",
            accessLevel = null,
            capabilities = null,
        )

        val domain = dto.toDomain()
        assertEquals(ConversationAccessLevel.UNKNOWN, domain.accessLevel)
        assertFalse(domain.capabilities.canUploadAttachments)
    }

    @Test
    fun `conversation with unknown access_level fails closed to UNKNOWN`() {
        val dto = MessageDtos.ConversationDto(
            id = 4,
            patientId = null,
            unreadCount = 0,
            createdAt = "2026-08-01T00:00:00Z",
            accessLevel = "new_future_type",
            capabilities = MessageDtos.ConversationCapabilitiesDto(canUploadAttachments = true),
        )

        val domain = dto.toDomain()
        assertEquals(ConversationAccessLevel.UNKNOWN, domain.accessLevel)
        assertTrue(domain.capabilities.canUploadAttachments)
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
            id = 3, senderId = 3, senderType = "unknown", body = "?", createdAt = "2026-08-01T00:00:00Z",
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

    @Test
    fun `MessagePage terminal cursor mapping`() {
        val response = MessageDtos.MessageListResponse(
            data = listOf(
                MessageDtos.MessageDto(
                    id = 1, senderId = 1, senderType = "patient", body = "Hi",
                    createdAt = "2026-08-01T00:00:00Z",
                ),
            ),
            meta = MessageDtos.CursorMetaDto(nextCursor = null, hasMore = false),
        )

        val page = com.eyecare.app.domain.model.MessagePage(
            messages = response.data.map { it.toDomain() },
            nextCursor = response.meta.nextCursor,
            hasMore = response.meta.hasMore,
        )

        assertEquals(1, page.messages.size)
        assertNull(page.nextCursor)
        assertFalse(page.hasMore)
    }

    @Test
    fun `MessagePage non-terminal cursor mapping`() {
        val cursor = "eyJpZCI6NTB9="
        val response = MessageDtos.MessageListResponse(
            data = listOf(
                MessageDtos.MessageDto(
                    id = 50, senderId = 5, senderType = "staff", body = "Earlier message.",
                    createdAt = "2026-07-15T08:00:00+08:00",
                ),
            ),
            meta = MessageDtos.CursorMetaDto(nextCursor = cursor, hasMore = true),
        )

        val page = com.eyecare.app.domain.model.MessagePage(
            messages = response.data.map { it.toDomain() },
            nextCursor = response.meta.nextCursor,
            hasMore = response.meta.hasMore,
        )

        assertEquals(1, page.messages.size)
        assertNotNull(page.nextCursor)
        assertEquals(cursor, page.nextCursor)
        assertTrue(page.hasMore)
    }
}

private fun MessageDtos.ConversationDto.toDomain() = com.eyecare.app.domain.model.Conversation(
    id = id,
    patientId = patientId,
    unreadCount = unreadCount,
    createdAt = createdAt,
    accessLevel = com.eyecare.app.domain.model.ConversationAccessLevel.from(accessLevel),
    capabilities = capabilities?.let {
        com.eyecare.app.domain.model.ConversationCapabilities(canUploadAttachments = it.canUploadAttachments)
    } ?: com.eyecare.app.domain.model.ConversationCapabilities.SAFE_DEFAULT,
)
