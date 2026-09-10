package com.eyecare.app.presentation.ar.tracking

import kotlin.math.abs
import kotlin.math.cos

/**
 * Derives a visible frame scale from the detected face's projected width.
 *
 * MediaPipe's pose matrix is useful for translation and rotation, but its metric scale is not a
 * reliable screen-size signal when the camera moves. This tracker snapshots the first valid face
 * width and mapped pose scale, then applies only the relative width change to that calibrated
 * baseline. Keeping the baseline scale avoids replacing the asset's published calibration.
 *
 * A small yaw compensation prevents a normal head turn from being interpreted as the face moving
 * farther away. The multiplier is bounded because landmark spans can briefly be noisy during
 * tracking transitions.
 */
internal class FaceDistanceScaleTracker(
    private val minMultiplier: Float = DEFAULT_MIN_MULTIPLIER,
    private val maxMultiplier: Float = DEFAULT_MAX_MULTIPLIER,
    private val minimumYawCosine: Float = DEFAULT_MINIMUM_YAW_COSINE,
) {

    private var referenceFaceWidthNorm: Float? = null
    private var referencePoseScale: Float? = null

    init {
        require(minMultiplier.isFinite() && minMultiplier > 0f) {
            "Minimum face distance scale multiplier must be positive and finite"
        }
        require(maxMultiplier.isFinite() && maxMultiplier >= minMultiplier) {
            "Maximum face distance scale multiplier must be finite and at least the minimum"
        }
        require(minimumYawCosine.isFinite() && minimumYawCosine > 0f && minimumYawCosine <= 1f) {
            "Minimum yaw cosine must be in the range (0, 1]"
        }
    }

    /**
     * Returns the calibrated visible scale for this sample, or null when the sample is invalid.
     */
    fun update(
        faceWidthNorm: Float,
        mappedPoseScale: Float,
        yawDeg: Float,
    ): Float? {
        if (
            !faceWidthNorm.isFinite() || faceWidthNorm <= MIN_FACE_WIDTH_NORM ||
            !mappedPoseScale.isFinite() || mappedPoseScale <= 0f ||
            !yawDeg.isFinite()
        ) {
            return null
        }

        val correctedFaceWidth = compensateForYaw(faceWidthNorm, yawDeg)
        val baselineWidth = referenceFaceWidthNorm
        val baselineScale = referencePoseScale
        if (baselineWidth == null || baselineScale == null) {
            referenceFaceWidthNorm = correctedFaceWidth
            referencePoseScale = mappedPoseScale
            return mappedPoseScale
        }

        val relativeWidth = (correctedFaceWidth / baselineWidth)
            .coerceIn(minMultiplier, maxMultiplier)
        val scale = baselineScale * relativeWidth
        return scale.takeIf { it.isFinite() && it > 0f }
    }

    /** Clears the session baseline so the next valid sample establishes a new calibration point. */
    fun reset() {
        referenceFaceWidthNorm = null
        referencePoseScale = null
    }

    private fun compensateForYaw(faceWidthNorm: Float, yawDeg: Float): Float {
        val yawRadians = Math.toRadians(yawDeg.toDouble())
        val projectedWidthFactor = abs(cos(yawRadians)).toFloat()
            .coerceAtLeast(minimumYawCosine)
        return faceWidthNorm / projectedWidthFactor
    }

    private companion object {
        // Temple landmarks become unreliable when the detector has only a small face span.
        const val MIN_FACE_WIDTH_NORM = 0.05f
        // Do not let extreme oblique poses amplify detector noise without bound.
        const val DEFAULT_MINIMUM_YAW_COSINE = 0.5f
        // Keep the initial backend/asset calibration as the center of the visual range.
        const val DEFAULT_MIN_MULTIPLIER = 0.8f
        const val DEFAULT_MAX_MULTIPLIER = 1.3f
    }
}
