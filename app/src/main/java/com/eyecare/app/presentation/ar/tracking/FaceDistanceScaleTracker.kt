package com.eyecare.app.presentation.ar.tracking

import kotlin.math.abs
import kotlin.math.cos

/**
 * Derives a visible frame scale from the detected face's projected width.
 *
 * MediaPipe's pose matrix is useful for translation and rotation, but its metric scale is not a
 * reliable screen-size signal when the camera moves. This tracker snapshots the median face width
 * and mapped pose scale from a short run of trusted samples, then applies only the relative width
 * change to that calibrated baseline. Keeping the baseline scale avoids replacing the asset's
 * published calibration.
 *
 * The baseline is accepted only for a centered, near-frontal pose. A small yaw compensation
 * prevents a normal head turn from being interpreted as the face moving farther away. The
 * multiplier is bounded, and a short live median window prevents landmark spikes from briefly
 * enlarging or shrinking the frame during tracking transitions.
 */
internal class FaceDistanceScaleTracker(
    private val minMultiplier: Float = DEFAULT_MIN_MULTIPLIER,
    private val maxMultiplier: Float = DEFAULT_MAX_MULTIPLIER,
    private val minimumYawCosine: Float = DEFAULT_MINIMUM_YAW_COSINE,
    private val minimumTrustedSamples: Int = DEFAULT_MINIMUM_TRUSTED_SAMPLES,
) {

    private var referenceFaceWidthNorm: Float? = null
    private var referencePoseScale: Float? = null
    private var currentScale: Float? = null
    private val startupFaceWidths = mutableListOf<Float>()
    private val startupPoseScales = mutableListOf<Float>()
    private val recentFaceWidths = FloatArray(LIVE_WIDTH_WINDOW_SIZE)
    private val sortedFaceWidths = FloatArray(LIVE_WIDTH_WINDOW_SIZE)
    private var recentFaceWidthCount = 0
    private var nextRecentFaceWidthIndex = 0

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
        require(minimumTrustedSamples > 0) {
            "Minimum trusted face samples must be positive"
        }
    }

    /**
     * Returns the calibrated visible scale for this sample.
     *
     * Before the first stable baseline, null is returned until enough consecutive trusted samples
     * arrive for the renderer to wait for a reliable reading. Afterward, invalid or untrusted
     * samples keep the last trusted scale.
     */
    fun update(
        faceWidthNorm: Float,
        mappedPoseScale: Float,
        yawDeg: Float,
        faceCenterX: Float = DEFAULT_FACE_CENTER,
        faceCenterY: Float = DEFAULT_FACE_CENTER,
        pitchDeg: Float = 0f,
        rollDeg: Float = 0f,
    ): Float? {
        if (
            !faceWidthNorm.isFinite() || faceWidthNorm <= MIN_FACE_WIDTH_NORM ||
            !mappedPoseScale.isFinite() || mappedPoseScale <= 0f ||
            !yawDeg.isFinite() ||
            !faceCenterX.isFinite() || !faceCenterY.isFinite() ||
            !pitchDeg.isFinite() || !rollDeg.isFinite()
        ) {
            clearStartupSamplesIfNeeded()
            return currentScale
        }

        if (!isTrustedPose(faceCenterX, faceCenterY, pitchDeg, yawDeg, rollDeg)) {
            // Do not establish a baseline from an oblique or off-guide face. Once tracking has
            // started, holding the last trusted value avoids a scale jump while the user turns.
            clearStartupSamplesIfNeeded()
            return currentScale
        }

        val correctedFaceWidth = compensateForYaw(faceWidthNorm, yawDeg)
        val baselineWidth = referenceFaceWidthNorm
        val baselineScale = referencePoseScale
        if (baselineWidth == null || baselineScale == null) {
            startupFaceWidths += correctedFaceWidth
            startupPoseScales += mappedPoseScale
            if (startupFaceWidths.size < minimumTrustedSamples) return null

            val stableBaselineWidth = median(startupFaceWidths)
            referenceFaceWidthNorm = stableBaselineWidth
            referencePoseScale = median(startupPoseScales)
            currentScale = referencePoseScale
            seedLiveFaceWidth(stableBaselineWidth)
            startupFaceWidths.clear()
            startupPoseScales.clear()
            return currentScale
        }

        val stableFaceWidth = medianLiveFaceWidth(correctedFaceWidth)
        val relativeWidth = (stableFaceWidth / baselineWidth)
            .coerceIn(minMultiplier, maxMultiplier)
        val scale = baselineScale * relativeWidth
        currentScale = scale.takeIf { it.isFinite() && it > 0f }
        return currentScale
    }

    /** Clears the session baseline so the next valid sample establishes a new calibration point. */
    fun reset() {
        referenceFaceWidthNorm = null
        referencePoseScale = null
        currentScale = null
        startupFaceWidths.clear()
        startupPoseScales.clear()
        recentFaceWidthCount = 0
        nextRecentFaceWidthIndex = 0
    }

    private fun clearStartupSamplesIfNeeded() {
        if (referenceFaceWidthNorm == null || referencePoseScale == null) {
            startupFaceWidths.clear()
            startupPoseScales.clear()
        }
    }

    private fun median(values: List<Float>): Float {
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[middle]
        } else {
            (sorted[middle - 1] + sorted[middle]) / 2f
        }
    }

    private fun seedLiveFaceWidth(width: Float) {
        recentFaceWidths.fill(width)
        recentFaceWidthCount = LIVE_WIDTH_WINDOW_SIZE
        nextRecentFaceWidthIndex = 0
    }

    private fun medianLiveFaceWidth(width: Float): Float {
        recentFaceWidths[nextRecentFaceWidthIndex] = width
        nextRecentFaceWidthIndex = (nextRecentFaceWidthIndex + 1) % LIVE_WIDTH_WINDOW_SIZE
        recentFaceWidthCount = (recentFaceWidthCount + 1).coerceAtMost(LIVE_WIDTH_WINDOW_SIZE)

        for (index in 0 until recentFaceWidthCount) {
            sortedFaceWidths[index] = recentFaceWidths[index]
        }
        for (index in 1 until recentFaceWidthCount) {
            val value = sortedFaceWidths[index]
            var insertionIndex = index - 1
            while (insertionIndex >= 0 && sortedFaceWidths[insertionIndex] > value) {
                sortedFaceWidths[insertionIndex + 1] = sortedFaceWidths[insertionIndex]
                insertionIndex--
            }
            sortedFaceWidths[insertionIndex + 1] = value
        }
        return sortedFaceWidths[recentFaceWidthCount / 2]
    }

    private fun isTrustedPose(
        faceCenterX: Float,
        faceCenterY: Float,
        pitchDeg: Float,
        yawDeg: Float,
        rollDeg: Float,
    ): Boolean =
        abs(faceCenterX - DEFAULT_FACE_CENTER) <= MAX_CENTER_OFFSET_X &&
            abs(faceCenterY - DEFAULT_FACE_CENTER) <= MAX_CENTER_OFFSET_Y &&
            abs(pitchDeg) <= MAX_PITCH_DEGREES &&
            abs(yawDeg) <= MAX_YAW_DEGREES &&
            abs(rollDeg) <= MAX_ROLL_DEGREES

    private fun compensateForYaw(faceWidthNorm: Float, yawDeg: Float): Float {
        val yawRadians = Math.toRadians(yawDeg.toDouble())
        val projectedWidthFactor = abs(cos(yawRadians)).toFloat()
            .coerceAtLeast(minimumYawCosine)
        return faceWidthNorm / projectedWidthFactor
    }

    private companion object {
        // Temple landmarks become unreliable when the detector has only a small face span.
        const val MIN_FACE_WIDTH_NORM = 0.05f
        const val DEFAULT_FACE_CENTER = 0.5f
        // Scale calibration is enabled only while the face remains inside the central guide.
        const val MAX_CENTER_OFFSET_X = 0.15f
        const val MAX_CENTER_OFFSET_Y = 0.18f
        const val MAX_PITCH_DEGREES = 12f
        const val MAX_YAW_DEGREES = 12f
        const val MAX_ROLL_DEGREES = 10f
        // Do not let extreme oblique poses amplify detector noise without bound.
        const val DEFAULT_MINIMUM_YAW_COSINE = 0.5f
        // Keep the initial backend/asset calibration as the center of the visual range.
        const val DEFAULT_MIN_MULTIPLIER = 0.8f
        const val DEFAULT_MAX_MULTIPLIER = 1.3f
        const val DEFAULT_MINIMUM_TRUSTED_SAMPLES = 8
        const val LIVE_WIDTH_WINDOW_SIZE = 5
    }
}
