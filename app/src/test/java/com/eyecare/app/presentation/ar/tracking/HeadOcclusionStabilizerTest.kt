package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class HeadOcclusionStabilizerTest {

    @Test
    fun `reuses the latest mask while the next segmentation result is arriving`() {
        val stabilizer = HeadOcclusionStabilizer(maxReuseMs = 100L)
        val mask = mask(timestampMs = 100L)

        stabilizer.select(face(timestampMs = 100L, mask = mask), nowTimestampMs = 105L)

        val reused = stabilizer.select(
            face(timestampMs = 133L, mask = null),
            nowTimestampMs = 140L,
        )

        assertSame(mask, reused)
    }

    @Test
    fun `expires the previous mask after the bounded reuse window`() {
        val stabilizer = HeadOcclusionStabilizer(maxReuseMs = 100L)
        stabilizer.select(
            face(timestampMs = 100L, mask = mask(timestampMs = 100L)),
            nowTimestampMs = 100L,
        )

        val expired = stabilizer.select(
            face(timestampMs = 201L, mask = null),
            nowTimestampMs = 201L,
        )

        assertNull(expired)
    }

    @Test
    fun `clears the previous mask when tracking is lost`() {
        val stabilizer = HeadOcclusionStabilizer()
        stabilizer.select(
            face(timestampMs = 100L, mask = mask(timestampMs = 100L)),
            nowTimestampMs = 100L,
        )

        assertNull(stabilizer.select(face = null, nowTimestampMs = 120L))
        assertNull(
            stabilizer.select(
                face(timestampMs = 133L, mask = null),
                nowTimestampMs = 140L,
            ),
        )
    }

    private fun face(timestampMs: Long, mask: HeadSegmentationFrame?): FaceFrame = FaceFrame(
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
        headSegmentation = mask,
    )

    private fun mask(timestampMs: Long): HeadSegmentationFrame =
        HeadSegmentationFrame.from(
            imageWidth = 2,
            imageHeight = 2,
            timestampMs = timestampMs,
            confidence = FloatArray(4) { 0.8f },
        )!!
}
