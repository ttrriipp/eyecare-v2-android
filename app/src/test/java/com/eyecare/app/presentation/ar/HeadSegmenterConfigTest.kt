package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeadSegmenterConfigTest {

    @Test
    fun `configuration points at the approved person segmentation asset`() {
        assertEquals("selfie_segmenter.tflite", HeadSegmenterConfig.MODEL_ASSET)
        assertTrue(HeadSegmenterConfig.USE_CONFIDENCE_MASK)
        assertEquals(HeadSegmenterConfig.PERSON_CATEGORY_INDEX, 1)
    }

    @Test
    fun `segmentation frame copies and exposes an immutable confidence buffer`() {
        val raw = floatArrayOf(0.1f, 0.8f, 0.2f, 1f)

        val frame = HeadSegmentationFrame.from(
            imageWidth = 2,
            imageHeight = 2,
            timestampMs = 42L,
            confidence = raw,
        )

        assertTrue(frame != null)
        assertArrayEquals(raw, frame!!.copyConfidence())
        assertNotSame(raw, frame.copyConfidence())

        raw[0] = 0f
        assertEquals(0.1f, frame.confidenceAt(0, 0))
    }

    @Test
    fun `segmentation frame rejects malformed input`() {
        assertNull(
            HeadSegmentationFrame.from(
                imageWidth = 0,
                imageHeight = 2,
                timestampMs = 42L,
                confidence = FloatArray(0),
            ),
        )
        assertNull(
            HeadSegmentationFrame.from(
                imageWidth = 2,
                imageHeight = 2,
                timestampMs = -1L,
                confidence = FloatArray(4),
            ),
        )
        assertNull(
            HeadSegmentationFrame.from(
                imageWidth = 2,
                imageHeight = 2,
                timestampMs = 42L,
                confidence = FloatArray(3),
            ),
        )
        assertNull(
            HeadSegmentationFrame.from(
                imageWidth = 2,
                imageHeight = 2,
                timestampMs = 42L,
                confidence = floatArrayOf(0f, Float.NaN, 0f, 0f),
            ),
        )
    }

    @Test
    fun `confidence lookup rejects coordinates outside the mask`() {
        val frame = HeadSegmentationFrame.from(
            imageWidth = 2,
            imageHeight = 2,
            timestampMs = 42L,
            confidence = FloatArray(4),
        )!!

        assertFalse(frame.isInside(-1, 0))
        assertFalse(frame.isInside(0, 2))
        assertEquals(0f, frame.confidenceAt(1, 1))
    }
}
