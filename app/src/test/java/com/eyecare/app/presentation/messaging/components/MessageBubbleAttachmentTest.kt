package com.eyecare.app.presentation.messaging.components

import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.MessageAttachment
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MessageBubbleAttachmentTest {

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
    fun `image preview remains disabled outside linked patient conversations`() {
        val attachment = MessageAttachment(
            id = 9,
            originalName = "eye-photo.png",
            mimeType = "image/png",
            fileSize = 1024,
            downloadUrl = "/api/v1/conversation/attachments/9",
        )

        assertFalse(shouldRenderImagePreview(attachment, ConversationAccessLevel.GENERAL_INQUIRY))
    }

    @Test
    fun `attachment preview url normalizes api base trailing slash`() {
        assertEquals(
            "http://10.0.2.2/api/v1/conversation/attachments/7",
            buildAttachmentPreviewUrl(7, "http://10.0.2.2/api/v1/"),
        )
    }
}
