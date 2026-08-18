package com.eyecare.app.presentation.messaging.components

import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageAttachment
import com.eyecare.app.domain.model.SenderType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset

class MessageBubbleAttachmentTest {

    // Fixed at UTC so results don't depend on the machine running the test.
    private val now = OffsetDateTime.parse("2026-08-18T15:00:00Z")

    @Test
    fun `timestamp from today shows time only`() {
        assertEquals(
            "9:05 AM",
            formatMessageTimestamp("2026-08-18T09:05:00Z", now = now, zone = ZoneOffset.UTC),
        )
    }

    @Test
    fun `timestamp from yesterday is prefixed`() {
        assertEquals(
            "Yesterday, 9:05 AM",
            formatMessageTimestamp("2026-08-17T09:05:00Z", now = now, zone = ZoneOffset.UTC),
        )
    }

    @Test
    fun `timestamp from earlier this year shows the date without a year`() {
        assertEquals(
            "Aug 1, 9:05 AM",
            formatMessageTimestamp("2026-08-01T09:05:00Z", now = now, zone = ZoneOffset.UTC),
        )
    }

    @Test
    fun `timestamp from a prior year includes the year`() {
        assertEquals(
            "Aug 18, 2025, 9:05 AM",
            formatMessageTimestamp("2025-08-18T09:05:00Z", now = now, zone = ZoneOffset.UTC),
        )
    }

    @Test
    fun `unparseable timestamp falls back to a truncated raw string`() {
        assertEquals(
            "not a real times",
            formatMessageTimestamp("not a real timestamp", now = now, zone = ZoneOffset.UTC),
        )
    }

    @Test
    fun `attachment placeholder body is hidden regardless of capitalization`() {
        assertFalse(shouldShowMessageBody("attachment", hasAttachments = true))
        assertFalse(shouldShowMessageBody(" Attachment ", hasAttachments = true))
        assertTrue(shouldShowMessageBody("Please review the attachment", hasAttachments = true))
    }

    @Test
    fun `image attachment remains previewable when response omits download url`() {
        val attachment = MessageAttachment(
            id = 7,
            originalName = "eye-photo.jpg",
            mimeType = "image/jpeg",
            fileSize = 1024,
            downloadUrl = null,
        )

        assertTrue(shouldRenderImagePreview(attachment, ConversationAccessLevel.LINKED_PATIENT))
    }

    @Test
    fun `image preview falls back to a known image filename when mime type is generic`() {
        val attachment = MessageAttachment(
            id = 10,
            originalName = "Screenshot_2026-08-16-18-54-140_com.tcare.jpg",
            mimeType = "application/octet-stream",
            fileSize = 1024,
            downloadUrl = null,
        )

        assertTrue(shouldRenderImagePreview(attachment, ConversationAccessLevel.LINKED_PATIENT))
    }

    @Test
    fun `non-image attachment is not previewed as an image`() {
        val attachment = MessageAttachment(
            id = 8,
            originalName = "receipt.pdf",
            mimeType = "application/pdf",
            fileSize = 1024,
            downloadUrl = null,
        )

        assertFalse(shouldRenderImagePreview(attachment, ConversationAccessLevel.LINKED_PATIENT))
    }

    @Test
    fun `pdf filename is not previewed as an image when mime type is generic`() {
        val attachment = MessageAttachment(
            id = 11,
            originalName = "mwd_g2_chapter4_draft-1.pdf",
            mimeType = "application/octet-stream",
            fileSize = 1024,
            downloadUrl = null,
        )

        assertFalse(shouldRenderImagePreview(attachment, ConversationAccessLevel.LINKED_PATIENT))
    }

    @Test
    fun `general inquiry image preview is allowed for account-owned attachments`() {
        val attachment = MessageAttachment(
            id = 9,
            originalName = "eye-photo.png",
            mimeType = "image/png",
            fileSize = 1024,
            downloadUrl = "/api/v1/conversation/attachments/9",
        )

        assertTrue(shouldRenderImagePreview(attachment, ConversationAccessLevel.GENERAL_INQUIRY))
    }

    @Test
    fun `unknown conversation access keeps attachment content disabled`() {
        val attachment = MessageAttachment(
            id = 12,
            originalName = "eye-photo.png",
            mimeType = "image/png",
            fileSize = 1024,
            downloadUrl = "/api/v1/conversation/attachments/12",
        )

        assertFalse(canAccessAttachments(ConversationAccessLevel.UNKNOWN))
        assertFalse(shouldRenderImagePreview(attachment, ConversationAccessLevel.UNKNOWN))
    }

    @Test
    fun `linked and general inquiry conversations can open account-owned attachments`() {
        assertTrue(canAccessAttachments(ConversationAccessLevel.LINKED_PATIENT))
        assertTrue(canAccessAttachments(ConversationAccessLevel.GENERAL_INQUIRY))
    }

    @Test
    fun `attachment preview url normalizes api base trailing slash`() {
        assertEquals(
            "http://10.0.2.2/api/v1/conversation/attachments/7",
            buildAttachmentPreviewUrl(7, "http://10.0.2.2/api/v1/"),
        )
    }

    @Test
    fun `own message without read timestamp has sent receipt`() {
        assertEquals("Sent", readReceiptLabel(message(readAt = null), isOwn = true))
    }

    @Test
    fun `own message with read timestamp has read receipt`() {
        assertEquals(
            "Read",
            readReceiptLabel(message(readAt = "2026-08-18T09:10:00Z"), isOwn = true),
        )
    }

    @Test
    fun `incoming message has no outgoing read receipt`() {
        assertNull(readReceiptLabel(message(readAt = "2026-08-18T09:10:00Z"), isOwn = false))
    }

    private fun message(readAt: String?) = Message(
        id = 1,
        senderId = 42,
        senderType = SenderType.PATIENT,
        body = "Hello",
        readAt = readAt,
        createdAt = "2026-08-18T09:00:00Z",
        attachments = emptyList(),
    )
}
