package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceOccluderMapperTest {

    @Test
    fun `matching aspect ratios preserve the center`() {
        val mesh = meshWith(
            index = 168,
            x = 0.5f,
            y = 0.5f,
            z = -0.2f,
        )

        val geometry = mapFaceOccluder(
            landmarks = mesh,
            imageWidth = 1000,
            imageHeight = 1000,
            faceWidthNorm = 0.4f,
            viewport = FaceOccluderViewport(widthPx = 500f, heightPx = 500f),
            config = FaceOccluderMappingConfig(mirrorFrontCamera = false),
        )

        assertEquals(250f, geometry?.xPx(168))
        assertEquals(250f, geometry?.yPx(168))
    }

    @Test
    fun `wider image uses centered horizontal aspect-fill crop`() {
        val geometry = mapFaceOccluder(
            landmarks = meshWith(index = 10, x = 0f, y = 0.5f, z = 0f),
            imageWidth = 1000,
            imageHeight = 500,
            faceWidthNorm = 0.4f,
            viewport = FaceOccluderViewport(widthPx = 500f, heightPx = 500f),
            config = FaceOccluderMappingConfig(mirrorFrontCamera = false),
        )

        assertEquals(-250f, geometry?.xPx(10))
        assertEquals(250f, geometry?.yPx(10))
    }

    @Test
    fun `taller image uses centered vertical aspect-fill crop`() {
        val geometry = mapFaceOccluder(
            landmarks = meshWith(index = 10, x = 0.5f, y = 0f, z = 0f),
            imageWidth = 500,
            imageHeight = 1000,
            faceWidthNorm = 0.4f,
            viewport = FaceOccluderViewport(widthPx = 1000f, heightPx = 500f),
            config = FaceOccluderMappingConfig(mirrorFrontCamera = false),
        )

        assertEquals(500f, geometry?.xPx(10))
        assertEquals(-750f, geometry?.yPx(10))
    }

    @Test
    fun `front camera mirroring is applied exactly once`() {
        val geometry = mapFaceOccluder(
            landmarks = meshWith(index = 10, x = 0.25f, y = 0.5f, z = 0f),
            imageWidth = 1000,
            imageHeight = 1000,
            faceWidthNorm = 0.4f,
            viewport = FaceOccluderViewport(widthPx = 1000f, heightPx = 1000f),
            config = FaceOccluderMappingConfig(mirrorFrontCamera = true),
        )

        assertEquals(750f, geometry?.xPx(10))
    }

    @Test
    fun `face relative depth preserves ordering and clamps extremes`() {
        val values = FloatArray(FaceMeshLandmarks.VALUE_COUNT)
        values[168 * 3 + 2] = -0.4f
        values[10 * 3 + 2] = -0.2f
        values[11 * 3 + 2] = 10f
        val mesh = FaceMeshLandmarks.from(values)!!

        val geometry = mapFaceOccluder(
            landmarks = mesh,
            imageWidth = 1000,
            imageHeight = 1000,
            faceWidthNorm = 0.4f,
            viewport = FaceOccluderViewport(widthPx = 1000f, heightPx = 1000f),
            config = FaceOccluderMappingConfig(
                mirrorFrontCamera = false,
                depthScale = 0.5f,
                depthBias = 0.02f,
                maxDepthOffset = 0.2f,
            ),
        )

        assertEquals(0.02f, geometry?.depthOffset(168))
        checkNotNull(geometry)
        assertTrue(geometry.depthOffset(10) > geometry.depthOffset(168))
        assertEquals(0.2f, geometry.depthOffset(11))
    }

    @Test
    fun `invalid dimensions face width and coordinates fail closed`() {
        val validMesh = meshWith(index = 10, x = 0.5f, y = 0.5f, z = 0f)
        val viewport = FaceOccluderViewport(widthPx = 500f, heightPx = 500f)

        assertNull(mapFaceOccluder(validMesh, 0, 1000, 0.4f, viewport))
        assertNull(mapFaceOccluder(validMesh, 1000, 1000, 0f, viewport))
        assertNull(
            mapFaceOccluder(
                validMesh,
                1000,
                1000,
                0.4f,
                FaceOccluderViewport(widthPx = Float.NaN, heightPx = 500f),
            )
        )

        val nonFinite = FloatArray(FaceMeshLandmarks.VALUE_COUNT)
        nonFinite[10 * 3] = Float.POSITIVE_INFINITY
        assertNull(FaceMeshLandmarks.from(nonFinite))
    }

    @Test
    fun `depth bias and scale stay within documented bounds`() {
        assertThrows(IllegalArgumentException::class.java) {
            FaceOccluderMappingConfig(depthBias = 0.051f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            FaceOccluderMappingConfig(depthScale = 2.01f)
        }
    }

    private fun meshWith(index: Int, x: Float, y: Float, z: Float): FaceMeshLandmarks {
        val values = FloatArray(FaceMeshLandmarks.VALUE_COUNT)
        values[index * 3] = x
        values[index * 3 + 1] = y
        values[index * 3 + 2] = z
        return FaceMeshLandmarks.from(values)!!
    }
}
