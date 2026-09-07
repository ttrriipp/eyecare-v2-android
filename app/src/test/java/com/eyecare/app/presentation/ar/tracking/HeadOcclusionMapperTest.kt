package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HeadOcclusionMapperTest {

    @Test
    fun `central face corridor is excluded while side columns remain`() {
        val mask = mapHeadOcclusionMask(
            segmentation = segmentation(width = 10, height = 2) { _, _ -> 0.9f },
            face = face(),
            viewport = HeadOcclusionViewport(widthPx = 500f, heightPx = 500f),
            config = HeadOcclusionMappingConfig(
                mirrorFrontCamera = false,
                centralFaceMarginNorm = 0.05f,
            ),
        )

        assertEquals(8, mask?.activePixelCount)
        assertTrue(checkNotNull(mask).isActive(0, 0))
        assertTrue(checkNotNull(mask).isActive(9, 0))
        assertEquals(0, checkNotNull(mask).alphaAt(3, 0))
        assertEquals(0, checkNotNull(mask).alphaAt(6, 0))
    }

    @Test
    fun `confidence below threshold produces no mask`() {
        val mask = mapHeadOcclusionMask(
            segmentation = segmentation(width = 4, height = 4) { _, _ -> 0.59f },
            face = face(),
            viewport = HeadOcclusionViewport(widthPx = 400f, heightPx = 400f),
            config = HeadOcclusionMappingConfig(confidenceThreshold = 0.6f),
        )

        assertNull(mask)
    }

    @Test
    fun `front camera mirror is applied once after shared rotation`() {
        val mask = mapHeadOcclusionMask(
            segmentation = segmentation(width = 1_000, height = 500) { x, _ ->
                if (x == 0) 0.9f else 0f
            },
            face = face(),
            viewport = HeadOcclusionViewport(widthPx = 500f, heightPx = 500f),
            config = HeadOcclusionMappingConfig(mirrorFrontCamera = true),
        )

        // The analyzer has already rotated the shared MPImage. The leftmost
        // source pixel is therefore mirrored to the right side of this
        // 500px aspect-filled viewport, not rotated again here.
        assertEquals(749.5f, checkNotNull(mask).viewportXForSourcePixel(0))
    }

    @Test
    fun `aspect fill uses the same centered crop as face geometry`() {
        val mask = mapHeadOcclusionMask(
            segmentation = segmentation(width = 1_000, height = 500) { x, _ ->
                if (x == 0) 0.9f else 0f
            },
            face = face(),
            viewport = HeadOcclusionViewport(widthPx = 500f, heightPx = 500f),
            config = HeadOcclusionMappingConfig(mirrorFrontCamera = false),
        )

        assertEquals(-249.5f, checkNotNull(mask).viewportXForSourcePixel(0))
        assertEquals(250.5f, checkNotNull(mask).viewportYForSourcePixel(250))
    }

    @Test
    fun `rotated source dimensions are preserved without a second rotation`() {
        val mask = mapHeadOcclusionMask(
            segmentation = segmentation(width = 4, height = 8) { x, _ ->
                if (x == 0) 0.9f else 0f
            },
            face = face(),
            viewport = HeadOcclusionViewport(widthPx = 400f, heightPx = 400f),
            config = HeadOcclusionMappingConfig(mirrorFrontCamera = false),
        )

        assertEquals(4, checkNotNull(mask).imageWidth)
        assertEquals(8, checkNotNull(mask).imageHeight)
        assertEquals(50f, checkNotNull(mask).viewportXForSourcePixel(0))
    }

    @Test
    fun `invalid dimensions and face coordinates fail closed`() {
        val validFace = face()
        val validViewport = HeadOcclusionViewport(widthPx = 500f, heightPx = 500f)

        assertNull(
            mapHeadOcclusionMask(
                segmentation = segmentation(width = 2, height = 2) { _, _ -> 0.9f },
                face = validFace.copy(faceWidthNorm = 0f),
                viewport = validViewport,
            )
        )
        assertNull(
            mapHeadOcclusionMask(
                segmentation = segmentation(width = 2, height = 2) { _, _ -> 0.9f },
                face = validFace.copy(leftTempleX = Float.NaN),
                viewport = validViewport,
            )
        )
        assertNull(
            mapHeadOcclusionMask(
                segmentation = segmentation(width = 2, height = 2) { _, _ -> 0.9f },
                face = validFace,
                viewport = HeadOcclusionViewport(widthPx = Float.NaN, heightPx = 500f),
            )
        )
    }

    private fun face(): FaceFrame = FaceFrame(
        noseBridgeX = 0.5f,
        noseBridgeY = 0.5f,
        leftTempleX = 0.25f,
        rightTempleX = 0.75f,
        faceWidthNorm = 0.5f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = FaceTransformationMatrix.from(FloatArray(16))!!,
        timestampMs = 100L,
    )

    private fun segmentation(
        width: Int,
        height: Int,
        value: (x: Int, y: Int) -> Float,
    ): HeadSegmentationFrame = HeadSegmentationFrame.from(
        imageWidth = width,
        imageHeight = height,
        timestampMs = 100L,
        confidence = FloatArray(width * height) { index ->
            value(index % width, index / width)
        },
    )!!
}
