package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame

/**
 * Keeps the last valid segmentation result while the asynchronous segmenter
 * catches up with face tracking. This prevents a one-frame switch to the yaw
 * fallback from flashing the temples on every result.
 *
 * The reuse window is deliberately bounded. A face loss, invalid timestamp,
 * future result, or expired mask clears the cached result immediately.
 */
internal class HeadOcclusionStabilizer(
    private val maxReuseMs: Long = DEFAULT_MAX_REUSE_MS,
) {

    init {
        require(maxReuseMs in 1L..MAX_REUSE_MS) {
            "Head mask reuse must be between 1 and $MAX_REUSE_MS ms"
        }
    }

    private var latestMask: HeadSegmentationFrame? = null

    fun select(
        face: FaceFrame?,
        nowTimestampMs: Long,
    ): HeadSegmentationFrame? {
        if (face == null || nowTimestampMs < 0L || face.timestampMs < 0L) {
            latestMask = null
            return null
        }

        val currentMask = face.headSegmentation
        if (currentMask != null) {
            val currentLagMs = face.timestampMs - currentMask.timestampMs
            val currentAgeMs = nowTimestampMs - currentMask.timestampMs
            if (
                currentMask.timestampMs >= 0L &&
                currentMask.timestampMs <= face.timestampMs &&
                currentLagMs in 0L..maxReuseMs &&
                currentAgeMs in 0L..maxReuseMs
            ) {
                latestMask = currentMask
                return currentMask
            }

            latestMask = null
            return null
        }

        val previousMask = latestMask ?: return null
        val lagMs = face.timestampMs - previousMask.timestampMs
        val ageMs = nowTimestampMs - previousMask.timestampMs
        if (
            previousMask.timestampMs < 0L ||
            previousMask.timestampMs > face.timestampMs ||
            lagMs !in 0L..maxReuseMs ||
            ageMs !in 0L..maxReuseMs
        ) {
            latestMask = null
            return null
        }
        return previousMask
    }

    fun reset() {
        latestMask = null
    }

    private companion object {
        const val DEFAULT_MAX_REUSE_MS = 100L
        const val MAX_REUSE_MS = 1_000L
    }
}
