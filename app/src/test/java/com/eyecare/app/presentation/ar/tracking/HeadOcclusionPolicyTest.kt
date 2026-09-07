package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import com.eyecare.app.presentation.ar.model.HeadOcclusionMask
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class HeadOcclusionPolicyTest {

    @Test
    fun `paired fresh confident mask is active`() {
        val policy = HeadOcclusionPolicy(maxFreshnessMs = 100L, minConfidence = 0.6f)

        assertEquals(
            HeadOcclusionMode.Mask,
            policy.select(face = face(900L), mask = mask(900L, 0.8f), nowTimestampMs = 1_000L),
        )
    }

    @Test
    fun `missing face stale mask and low confidence use temple fallback`() {
        val policy = HeadOcclusionPolicy(maxFreshnessMs = 100L, minConfidence = 0.6f)

        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = null, mask = mask(1_000L, 0.8f), nowTimestampMs = 1_000L),
        )
        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(899L), mask = mask(899L, 0.8f), nowTimestampMs = 1_000L),
        )
        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(1_000L), mask = mask(1_000L, 0.59f), nowTimestampMs = 1_000L),
        )
    }

    @Test
    fun `mask from a different source timestamp cannot activate`() {
        val policy = HeadOcclusionPolicy()

        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(1_000L), mask = mask(999L, 0.9f), nowTimestampMs = 1_000L),
        )
    }

    @Test
    fun `future and negative timestamps use temple fallback`() {
        val policy = HeadOcclusionPolicy()

        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(-1L), mask = mask(0L, 0.9f), nowTimestampMs = 1_000L),
        )
        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(1_001L), mask = mask(1_001L, 0.9f), nowTimestampMs = 1_000L),
        )
        assertEquals(
            HeadOcclusionMode.TempleFallback,
            policy.select(face = face(1_000L), mask = mask(1_000L, 0.9f), nowTimestampMs = -1L),
        )
    }

    @Test
    fun `freshness and confidence bounds are explicit`() {
        assertThrows(IllegalArgumentException::class.java) {
            HeadOcclusionPolicy(maxFreshnessMs = 0L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HeadOcclusionPolicy(maxFreshnessMs = 1_001L)
        }
        assertThrows(IllegalArgumentException::class.java) {
            HeadOcclusionPolicy(minConfidence = 1.1f)
        }
    }

    private fun face(timestampMs: Long): FaceFrame = FaceFrame(
        noseBridgeX = 0.5f,
        noseBridgeY = 0.5f,
        leftTempleX = 0.25f,
        rightTempleX = 0.75f,
        faceWidthNorm = 0.5f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = FaceTransformationMatrix.from(FloatArray(16))!!,
        timestampMs = timestampMs,
    )

    private fun mask(timestampMs: Long, confidence: Float): HeadOcclusionMask =
        HeadOcclusionMask.from(
            imageWidth = 2,
            imageHeight = 2,
            timestampMs = timestampMs,
            viewportWidthPx = 500f,
            viewportHeightPx = 500f,
            fillScale = 1f,
            cropX = 0f,
            cropY = 0f,
            mirrorFrontCamera = true,
            confidence = confidence,
            activePixelCount = 1,
            alpha = byteArrayOf(0xFF.toByte(), 0, 0, 0),
        )!!
}
