package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.ArTrackingQuality
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import kotlin.math.abs

/** Shared pose limits used by distance calibration and the on-screen tracking guidance. */
internal object FaceTrackingThresholds {
    const val DEFAULT_FACE_CENTER = 0.5f
    const val MAX_CENTER_OFFSET_X = 0.15f
    const val MAX_CENTER_OFFSET_Y = 0.18f
    const val MAX_PITCH_DEGREES = 12f
    // Allow a modest side turn while staying inside the renderer's far-temple safety range.
    const val MAX_YAW_DEGREES = 20f
    const val MAX_ROLL_DEGREES = 10f
}

/**
 * Classifies the current detector output into one actionable tracking state.
 *
 * A missing pose means calibration is still in progress (or the transform was rejected). Once a
 * pose is available, position is checked before orientation so the user receives one clear next
 * action instead of competing warnings.
 */
internal fun classifyFaceTrackingQuality(
    face: FaceFrame,
    pose: FacePose?,
): ArTrackingQuality {
    if (pose == null) return ArTrackingQuality.Stabilizing
    if (
        !face.noseBridgeX.isFinite() || !face.noseBridgeY.isFinite() ||
        !pose.pitchDeg.isFinite() || !pose.yawDeg.isFinite() || !pose.rollDeg.isFinite()
    ) {
        return ArTrackingQuality.Stabilizing
    }

    return when {
        abs(face.noseBridgeX - FaceTrackingThresholds.DEFAULT_FACE_CENTER) >
            FaceTrackingThresholds.MAX_CENTER_OFFSET_X ||
            abs(face.noseBridgeY - FaceTrackingThresholds.DEFAULT_FACE_CENTER) >
            FaceTrackingThresholds.MAX_CENTER_OFFSET_Y -> ArTrackingQuality.CenterFace
        abs(pose.pitchDeg) > FaceTrackingThresholds.MAX_PITCH_DEGREES ||
            abs(pose.yawDeg) > FaceTrackingThresholds.MAX_YAW_DEGREES -> ArTrackingQuality.LookStraight
        abs(pose.rollDeg) > FaceTrackingThresholds.MAX_ROLL_DEGREES -> ArTrackingQuality.LevelHead
        else -> ArTrackingQuality.Stable
    }
}

/** Returns true only for samples safe to use as a calibration baseline. */
internal fun isTrustedFacePose(
    faceCenterX: Float,
    faceCenterY: Float,
    pitchDeg: Float,
    yawDeg: Float,
    rollDeg: Float,
): Boolean =
    abs(faceCenterX - FaceTrackingThresholds.DEFAULT_FACE_CENTER) <=
        FaceTrackingThresholds.MAX_CENTER_OFFSET_X &&
        abs(faceCenterY - FaceTrackingThresholds.DEFAULT_FACE_CENTER) <=
        FaceTrackingThresholds.MAX_CENTER_OFFSET_Y &&
        abs(pitchDeg) <= FaceTrackingThresholds.MAX_PITCH_DEGREES &&
        abs(yawDeg) <= FaceTrackingThresholds.MAX_YAW_DEGREES &&
        abs(rollDeg) <= FaceTrackingThresholds.MAX_ROLL_DEGREES
