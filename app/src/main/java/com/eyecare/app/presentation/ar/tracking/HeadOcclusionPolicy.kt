package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.HeadOcclusionMask
import kotlin.math.abs

internal enum class HeadOcclusionMode {
    Mask,
    TempleFallback,
}

/**
 * Enables side-head masking only while the face and mask are current or the
 * mask is within the bounded asynchronous reuse window. Any failure returns
 * the existing yaw-based temple fallback immediately.
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
        yawDegrees: Float? = null,
    ): HeadOcclusionMode {
        val maskLagMs = if (face != null && mask != null) {
            face.timestampMs - mask.timestampMs
        } else {
            Long.MIN_VALUE
        }
        if (
            face == null ||
            mask == null ||
            nowTimestampMs < 0L ||
            face.timestampMs < 0L ||
            mask.timestampMs < 0L ||
            mask.timestampMs > face.timestampMs ||
            maskLagMs > maxFreshnessMs ||
            mask.timestampMs > nowTimestampMs ||
            nowTimestampMs - mask.timestampMs > maxFreshnessMs ||
            mask.confidence < minConfidence ||
            mask.activePixelCount <= 0 ||
            (yawDegrees != null &&
                (!yawDegrees.isFinite() || abs(yawDegrees) < MIN_MASK_YAW_DEGREES))
        ) {
            return HeadOcclusionMode.TempleFallback
        }
        return HeadOcclusionMode.Mask
    }

    private companion object {
        const val DEFAULT_MAX_FRESHNESS_MS = 100L
        const val MAX_FRESHNESS_MS = 1_000L
        const val DEFAULT_MIN_CONFIDENCE = 0.6f
        const val MIN_MASK_YAW_DEGREES = 24f
    }
}
