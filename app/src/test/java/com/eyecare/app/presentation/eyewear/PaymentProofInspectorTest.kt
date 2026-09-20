package com.eyecare.app.presentation.eyewear

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaymentProofInspectorTest {

    @Test
    fun `validates jpg mime type`() {
        val result = PaymentProofInspector.validateMimeType("image/jpeg")
        assertNull(result)
    }

    @Test
    fun `validates jpeg mime type`() {
        val result = PaymentProofInspector.validateMimeType("image/jpeg")
        assertNull(result)
    }

    @Test
    fun `validates png mime type`() {
        val result = PaymentProofInspector.validateMimeType("image/png")
        assertNull(result)
    }

    @Test
    fun `rejects gif mime type`() {
        val result = PaymentProofInspector.validateMimeType("image/gif")
        assertTrue(result != null)
        assertTrue(result!!.contains("JPG", ignoreCase = true) || result.contains("PNG", ignoreCase = true))
    }

    @Test
    fun `rejects pdf mime type`() {
        val result = PaymentProofInspector.validateMimeType("application/pdf")
        assertTrue(result != null)
    }

    @Test
    fun `rejects empty mime type`() {
        val result = PaymentProofInspector.validateMimeType("")
        assertTrue(result != null)
    }

    @Test
    fun `validates file size within limit`() {
        val result = PaymentProofInspector.validateFileSize(4 * 1024 * 1024) // 4 MB
        assertNull(result)
    }

    @Test
    fun `validates file size at limit`() {
        val result = PaymentProofInspector.validateFileSize(5 * 1024 * 1024) // 5 MB
        assertNull(result)
    }

    @Test
    fun `rejects file size over limit`() {
        val result = PaymentProofInspector.validateFileSize(6 * 1024 * 1024) // 6 MB
        assertTrue(result != null)
        assertTrue(result!!.contains("5 MB", ignoreCase = true))
    }

    @Test
    fun `validates image dimensions within limit`() {
        val result = PaymentProofInspector.validateDimensions(4000, 4000)
        assertNull(result)
    }

    @Test
    fun `validates image dimensions at limit`() {
        val result = PaymentProofInspector.validateDimensions(8000, 8000)
        assertNull(result)
    }

    @Test
    fun `rejects image dimensions over limit`() {
        val result = PaymentProofInspector.validateDimensions(9000, 4000)
        assertTrue(result != null)
        assertTrue(result!!.contains("8,000", ignoreCase = true))
    }

    @Test
    fun `rejects zero dimensions`() {
        val result = PaymentProofInspector.validateDimensions(0, 4000)
        assertTrue(result != null)
    }

    @Test
    fun `validates sender name within limit`() {
        val result = PaymentProofInspector.validateSenderName("Ana Reyes")
        assertNull(result)
    }

    @Test
    fun `rejects blank sender name`() {
        val result = PaymentProofInspector.validateSenderName("")
        assertTrue(result != null)
    }

    @Test
    fun `rejects sender name over 100 chars`() {
        val result = PaymentProofInspector.validateSenderName("A".repeat(101))
        assertTrue(result != null)
    }

    @Test
    fun `validates reference number within limit`() {
        val result = PaymentProofInspector.validateReferenceNumber("GCASH-12345")
        assertNull(result)
    }

    @Test
    fun `rejects blank reference number`() {
        val result = PaymentProofInspector.validateReferenceNumber("")
        assertTrue(result != null)
    }

    @Test
    fun `rejects reference number over 100 chars`() {
        val result = PaymentProofInspector.validateReferenceNumber("X".repeat(101))
        assertTrue(result != null)
    }
}