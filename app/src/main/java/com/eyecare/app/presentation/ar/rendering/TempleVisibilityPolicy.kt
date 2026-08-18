package com.eyecare.app.presentation.ar.rendering

import kotlin.math.abs

/**
 * Which independently modelled temples should remain visible.
 */
internal enum class TempleVisibility {
    Both,
    LeftOnly,
    RightOnly,
}

/**
 * Adds a conservative hysteresis band to the already-smoothed face yaw.
 *
 * Positive renderer yaw hides the model's left temple and negative yaw hides the right temple.
 * Assets without those named renderables are left untouched by the renderer, so this policy is
 * also safe for the existing combined GLBs.
 */
internal class TempleVisibilityPolicy(
    private val hideYawDegrees: Float = DEFAULT_HIDE_YAW_DEGREES,
    private val showYawDegrees: Float = DEFAULT_SHOW_YAW_DEGREES,
) {

    private var hiddenTemple: TempleSide? = null

    init {
        require(hideYawDegrees.isFinite() && hideYawDegrees > 0f) {
            "Hide yaw threshold must be positive and finite"
        }
        require(showYawDegrees.isFinite() && showYawDegrees >= 0f) {
            "Show yaw threshold must be non-negative and finite"
        }
        require(showYawDegrees < hideYawDegrees) {
            "Show yaw threshold must be lower than the hide threshold"
        }
    }

    fun update(yawDegrees: Float?): TempleVisibility {
        if (yawDegrees == null || !yawDegrees.isFinite()) {
            hiddenTemple = null
            return TempleVisibility.Both
        }

        val absoluteYaw = abs(yawDegrees)
        hiddenTemple = when {
            absoluteYaw >= hideYawDegrees && yawDegrees > 0f -> TempleSide.Left
            absoluteYaw >= hideYawDegrees && yawDegrees < 0f -> TempleSide.Right
            absoluteYaw <= showYawDegrees -> null
            else -> hiddenTemple
        }

        return when (hiddenTemple) {
            TempleSide.Left -> TempleVisibility.LeftOnly
            TempleSide.Right -> TempleVisibility.RightOnly
            null -> TempleVisibility.Both
        }
    }

    private enum class TempleSide {
        Left,
        Right,
    }

    private companion object {
        const val DEFAULT_HIDE_YAW_DEGREES = 32f
        const val DEFAULT_SHOW_YAW_DEGREES = 22f
    }
}
