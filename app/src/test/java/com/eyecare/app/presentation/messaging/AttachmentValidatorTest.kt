package com.eyecare.app.presentation.messaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AttachmentValidatorTest {

    @Test
    fun `jpg mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("image/jpeg"))
    }

    @Test
    fun `png mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("image/png"))
    }

    @Test
    fun `gif mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("image/gif"))
    }

    @Test
    fun `pdf mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("application/pdf"))
    }

    @Test
    fun `doc mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("application/msword"))
    }

    @Test
    fun `docx mime type is allowed`() {
        assertTrue(AttachmentValidator.isAllowedType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    }

    @Test
    fun `mp4 mime type is rejected`() {
        assertFalse(AttachmentValidator.isAllowedType("video/mp4"))
    }

    @Test
    fun `file within 10MB limit is valid`() {
        assertTrue(AttachmentValidator.isWithinSizeLimit(10 * 1024 * 1024L))
    }

    @Test
    fun `file exceeding 10MB is invalid`() {
        assertFalse(AttachmentValidator.isWithinSizeLimit(10 * 1024 * 1024L + 1))
    }

    @Test
    fun `validate returns success for valid file`() {
        val result = AttachmentValidator.validate("image/jpeg", 5 * 1024 * 1024L)
        assertTrue(result.isSuccess)
    }

    @Test
    fun `validate returns failure for invalid type`() {
        val result = AttachmentValidator.validate("video/mp4", 1024L)
        assertTrue(result.isFailure)
        assertEquals("Unsupported file type. Allowed: images, PDF, Word documents.", result.exceptionOrNull()?.message)
    }

    @Test
    fun `validate returns failure for oversized file`() {
        val result = AttachmentValidator.validate("image/jpeg", 11 * 1024 * 1024L)
        assertTrue(result.isFailure)
        assertEquals("File exceeds 10 MB limit.", result.exceptionOrNull()?.message)
    }
}
