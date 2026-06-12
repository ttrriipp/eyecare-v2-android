package com.eyecare.app.presentation.ar.model

/**
 * Computed face geometry derived from MediaPipe Face Landmarker output.
 * Coordinates are normalised [0,1] relative to the image frame.
 */
data class FaceFrame(
    /** Centre of nose bridge (average of landmarks 6 + 168), normalised x/y */
    val noseBridgeX: Float,
    val noseBridgeY: Float,
    /** Temple landmarks 234 (left) and 454 (right), normalised x */
    val leftTempleX: Float,
    val rightTempleX: Float,
    /** Temple-to-temple width in normalised units */
    val faceWidthNorm: Float,
    /** Face roll angle in degrees (positive = clockwise tilt) */
    val rotationDeg: Float,
    /** Image dimensions used to produce these values */
    val imageWidth: Int,
    val imageHeight: Int,
)

sealed interface ArFaceState {
    /** MediaPipe detected a face and computed geometry */
    data class Detected(val frame: FaceFrame) : ArFaceState
    /** No face in current frame */
    data object NoFace : ArFaceState
    /** Landmarker not yet initialised or closed */
    data object Initialising : ArFaceState
}
