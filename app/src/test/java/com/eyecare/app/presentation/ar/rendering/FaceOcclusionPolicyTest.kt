package com.eyecare.app.presentation.ar.rendering

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import com.eyecare.app.presentation.ar.tracking.FaceOccluderGeometry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FaceOcclusionPolicyTest {

    @Test
    fun `current complete geometry selects depth occlusion`() {
        val policy = FaceOcclusionPolicy(maxFreshnessMs = 100L)

        assertEquals(
            FaceOcclusionMode.Depth,
            policy.select(
                faceTimestampMs = 900L,
                nowTimestampMs = 1_000L,
                geometry = validGeometry(),
            ),
        )
    }

    @Test
    fun `missing geometry selects temple fallback`() {
        val policy = FaceOcclusionPolicy()

        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = 1_000L, nowTimestampMs = 1_000L, geometry = null),
        )
    }

    @Test
    fun `stale geometry selects temple fallback`() {
        val policy = FaceOcclusionPolicy(maxFreshnessMs = 100L)

        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(
                faceTimestampMs = 899L,
                nowTimestampMs = 1_000L,
                geometry = validGeometry(),
            ),
        )
    }

    @Test
    fun `negative or future timestamps select temple fallback`() {
        val policy = FaceOcclusionPolicy()
        val geometry = validGeometry()

        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = -1L, nowTimestampMs = 1_000L, geometry = geometry),
        )
        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = 1_001L, nowTimestampMs = 1_000L, geometry = geometry),
        )
        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = 1_000L, nowTimestampMs = -1L, geometry = geometry),
        )
    }

    @Test
    fun `face loss then recovery is deterministic`() {
        val policy = FaceOcclusionPolicy(maxFreshnessMs = 100L)
        val geometry = validGeometry()

        assertEquals(
            FaceOcclusionMode.Depth,
            policy.select(faceTimestampMs = 1_000L, nowTimestampMs = 1_000L, geometry = geometry),
        )
        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = null, nowTimestampMs = 1_033L, geometry = null),
        )
        assertEquals(
            FaceOcclusionMode.Depth,
            policy.select(faceTimestampMs = 1_066L, nowTimestampMs = 1_066L, geometry = geometry),
        )
    }

    @Test
    fun `incomplete geometry is rejected`() {
        val policy = FaceOcclusionPolicy()
        val incomplete = FaceOccluderGeometry.from(FloatArray(FaceMeshLandmarks.LANDMARK_COUNT * 2))

        assertEquals(
            FaceOcclusionMode.TempleFallback,
            policy.select(faceTimestampMs = 1_000L, nowTimestampMs = 1_000L, geometry = incomplete),
        )
    }

    @Test
    fun `freshness window must be positive and bounded`() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            FaceOcclusionPolicy(maxFreshnessMs = 0L)
        }
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
            FaceOcclusionPolicy(maxFreshnessMs = 1_001L)
        }
    }

    private fun validGeometry(): FaceOccluderGeometry =
        FaceOccluderGeometry.from(
            FloatArray(FaceMeshLandmarks.LANDMARK_COUNT * 3),
        )!!
}
