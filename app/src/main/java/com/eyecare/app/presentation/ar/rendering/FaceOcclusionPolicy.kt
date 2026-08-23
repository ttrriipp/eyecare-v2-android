package com.eyecare.app.presentation.ar.rendering

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import com.eyecare.app.presentation.ar.tracking.FaceOccluderGeometry

internal enum class FaceOcclusionMode {
    Depth,
    TempleFallback,
}

/**
 * Selects depth occlusion only for a complete, current face result.
 *
 * The policy is intentionally stateless: every decision revalidates the
 * timestamp and geometry, so face loss cannot leave an invisible stale blocker
 * active and reacquisition does not depend on old state.
 */
internal class FaceOcclusionPolicy(
    private val maxFreshnessMs: Long = DEFAULT_MAX_FRESHNESS_MS,
) {

    init {
        require(maxFreshnessMs in 1L..MAX_FRESHNESS_MS) {
            "Face occlusion freshness must be between 1 and $MAX_FRESHNESS_MS ms"
        }
    }

    fun select(
        faceTimestampMs: Long?,
        nowTimestampMs: Long,
        geometry: FaceOccluderGeometry?,
    ): FaceOcclusionMode {
        if (
            faceTimestampMs == null ||
            faceTimestampMs < 0L ||
            nowTimestampMs < 0L ||
            faceTimestampMs > nowTimestampMs ||
            geometry == null ||
            geometry.vertexCount != FaceMeshLandmarks.LANDMARK_COUNT
        ) {
            return FaceOcclusionMode.TempleFallback
        }

        val ageMs = nowTimestampMs - faceTimestampMs
        return if (ageMs <= maxFreshnessMs) {
            FaceOcclusionMode.Depth
        } else {
            FaceOcclusionMode.TempleFallback
        }
    }

    private companion object {
        const val DEFAULT_MAX_FRESHNESS_MS = 250L
        const val MAX_FRESHNESS_MS = 1_000L
    }
}
