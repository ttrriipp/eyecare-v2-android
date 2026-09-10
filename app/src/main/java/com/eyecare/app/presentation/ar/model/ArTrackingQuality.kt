package com.eyecare.app.presentation.ar.model

/**
 * Quality of the current face signal from the user's point of view.
 *
 * The non-stable values intentionally describe one next action so the preview can pause instead
 * of showing a frame that may be misaligned.
 */
enum class ArTrackingQuality {
    Stabilizing,
    Stable,
    CenterFace,
    LookStraight,
    LevelHead,
}
