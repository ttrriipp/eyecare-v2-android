package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FacePose
import kotlin.math.exp

/**
 * Timestamp-aware exponential stabilizer for renderer poses.
 *
 * This class owns only immutable values and primitive state. A no-face result is returned as null
 * immediately, so tracking loss is never hidden behind the previous pose. A large or invalid
 * timestamp gap starts a new baseline instead of smearing a stale pose into a later frame.
 */
class PoseStabilizer(
    private val responseTimeMs: Float = DEFAULT_RESPONSE_TIME_MS,
    private val maxTimestampGapMs: Long = DEFAULT_MAX_TIMESTAMP_GAP_MS,
) {
    private var previousPose: FacePose? = null
    private var previousTimestampMs = 0L
    private var hasPrevious = false

    init {
        require(responseTimeMs.isFinite() && responseTimeMs > 0f) {
            "Pose stabilizer response time must be positive and finite"
        }
        require(maxTimestampGapMs > 0L) {
            "Pose stabilizer timestamp gap must be positive"
        }
    }

    /**
     * Updates the filter with one timestamped detection, or resets it when [pose] is null.
     *
     * The first valid pose after initialization or a gap is emitted unchanged. Subsequent poses
     * use a time-derived exponential alpha, making smoothing consistent across frame rates.
     */
    fun update(pose: FacePose?, timestampMs: Long): FacePose? {
        if (pose == null || timestampMs < 0L) {
            reset()
            return null
        }

        val lastPose = previousPose
        if (!hasPrevious || lastPose == null) {
            return seed(pose, timestampMs)
        }

        if (timestampMs <= previousTimestampMs) {
            reset()
            return null
        }

        val elapsedMs = timestampMs - previousTimestampMs
        if (elapsedMs > maxTimestampGapMs) {
            return seed(pose, timestampMs)
        }

        val alpha = (1.0 - exp(-elapsedMs.toDouble() / responseTimeMs.toDouble()))
            .toFloat()
            .coerceIn(0f, 1f)
        val stabilized = blend(lastPose, pose, alpha)
        previousPose = stabilized
        previousTimestampMs = timestampMs
        return stabilized
    }

    /** Clears the baseline so the next valid detection is emitted unchanged. */
    fun reset() {
        previousPose = null
        previousTimestampMs = 0L
        hasPrevious = false
    }

    private fun seed(pose: FacePose, timestampMs: Long): FacePose {
        previousPose = pose
        previousTimestampMs = timestampMs
        hasPrevious = true
        return pose
    }

    private fun blend(previous: FacePose, current: FacePose, alpha: Float): FacePose = FacePose(
        translationX = lerp(previous.translationX, current.translationX, alpha),
        translationY = lerp(previous.translationY, current.translationY, alpha),
        translationZ = lerp(previous.translationZ, current.translationZ, alpha),
        pitchDeg = lerpAngle(previous.pitchDeg, current.pitchDeg, alpha),
        yawDeg = lerpAngle(previous.yawDeg, current.yawDeg, alpha),
        rollDeg = lerpAngle(previous.rollDeg, current.rollDeg, alpha),
        scale = lerp(previous.scale, current.scale, alpha),
    )

    private fun lerp(previous: Float, current: Float, alpha: Float): Float =
        previous + (current - previous) * alpha

    private fun lerpAngle(previous: Float, current: Float, alpha: Float): Float =
        previous + shortestAngleDelta(current - previous) * alpha

    private fun shortestAngleDelta(delta: Float): Float {
        val wrapped = (delta + HALF_TURN_DEGREES) % FULL_TURN_DEGREES
        return if (wrapped < 0f) {
            wrapped + HALF_TURN_DEGREES
        } else {
            wrapped - HALF_TURN_DEGREES
        }
    }

    private companion object {
        const val DEFAULT_RESPONSE_TIME_MS = 100f
        const val DEFAULT_MAX_TIMESTAMP_GAP_MS = 250L
        const val HALF_TURN_DEGREES = 180f
        const val FULL_TURN_DEGREES = 360f
    }
}
