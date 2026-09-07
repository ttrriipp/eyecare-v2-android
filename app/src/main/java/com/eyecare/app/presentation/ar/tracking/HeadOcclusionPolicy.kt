package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.HeadOcclusionMask

internal enum class HeadOcclusionMode {
    Mask,
    TempleFallback,
}

/**
 * Enables side-head masking only while the paired face and mask are current.
 * Any failure returns the existing yaw-based temple fallback immediately.
 */
internal class HeadOcclusionPolicy(
    private val maxFreshnessMs: Long = DEFAULT_MAX_FRESHNESS_MS,
    private val minConfidence: Float = DEFAULT_MIN_CONFIDENCE,
) {

    init {
        require(maxFreshnessMs in 1L..MAX_FRESHNESS_MS) {
            "Head occlusion freshness must be between 1 and $MAX_FRESHNESS_MS ms"
        }
        require(minConfidence.isFinite() && minConfidence in 0f..1f) {
            "Head occlusion confidence must be between 0 and 1"
        }
    }

    fun select(
        face: FaceFrame?,
        mask: HeadOcclusionMask?,
        nowTimestampMs: Long,
    ): HeadOcclusionMode {
        if (
            face == null ||
            mask == null ||
            nowTimestampMs < 0L ||
            face.timestampMs < 0L ||
            mask.timestampMs < 0L ||
            mask.timestampMs != face.timestampMs ||
            mask.timestampMs > nowTimestampMs ||
            nowTimestampMs - mask.timestampMs > maxFreshnessMs ||
            mask.confidence < minConfidence ||
            mask.activePixelCount <= 0
        ) {
            return HeadOcclusionMode.TempleFallback
        }
        return HeadOcclusionMode.Mask
    }

    private companion object {
        const val DEFAULT_MAX_FRESHNESS_MS = 100L
        const val MAX_FRESHNESS_MS = 1_000L
        const val DEFAULT_MIN_CONFIDENCE = 0.6f
    }
}
