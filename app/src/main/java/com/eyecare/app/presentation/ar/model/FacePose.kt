package com.eyecare.app.presentation.ar.model

/**
 * Renderer-space pose for one tracked face.
 *
 * Translation is expressed in the renderer's world units. Rotations use degrees with the
 * explicit convention pitch=X, yaw=Y, and roll=Z. Scale is a uniform multiplier applied on top
 * of the asset's own calibrated dimensions.
 */
data class FacePose(
    val translationX: Float,
    val translationY: Float,
    val translationZ: Float,
    val pitchDeg: Float,
    val yawDeg: Float,
    val rollDeg: Float,
    val scale: Float,
) {
    init {
        require(
            translationX.isFinite() &&
                translationY.isFinite() &&
                translationZ.isFinite() &&
                pitchDeg.isFinite() &&
                yawDeg.isFinite() &&
                rollDeg.isFinite() &&
                scale.isFinite()
        ) {
            "Face pose values must be finite"
        }
        require(scale > 0f) { "Face pose scale must be positive" }
    }
}

/**
 * Explicit conversion and first-asset calibration inputs for [FacePose].
 *
 * MediaPipe's face transform uses metric units while SceneView uses application-defined world
 * units. [translationScale] performs that conversion; anchors and angle offsets remain editable
 * calibration metadata instead of UI constants. The provisional round-frame values are a
 * reproducible starting point and require physical-device calibration before release.
 */
data class FacePoseCalibration(
    val translationScale: Float,
    val scaleMultiplier: Float,
    val mirrorFrontCamera: Boolean,
    val anchorX: Float = 0f,
    val anchorY: Float = 0f,
    val anchorZ: Float = 0f,
    val pitchOffsetDeg: Float = 0f,
    val yawOffsetDeg: Float = 0f,
    val rollOffsetDeg: Float = 0f,
) {
    init {
        require(translationScale.isFinite() && translationScale > 0f) {
            "Face pose translation scale must be positive and finite"
        }
        require(scaleMultiplier.isFinite() && scaleMultiplier > 0f) {
            "Face pose scale multiplier must be positive and finite"
        }
        require(
            anchorX.isFinite() &&
                anchorY.isFinite() &&
                anchorZ.isFinite() &&
                pitchOffsetDeg.isFinite() &&
                yawOffsetDeg.isFinite() &&
                rollOffsetDeg.isFinite()
        ) {
            "Face pose calibration values must be finite"
        }
    }

    companion object {
        /**
         * First bundled round-frame baseline: centimetre-to-metre conversion, a slightly raised
         * bridge anchor, and the existing front-camera mirror convention.
         */
        val ProvisionalRoundFrame = FacePoseCalibration(
            translationScale = 0.01f,
            scaleMultiplier = 1f,
            mirrorFrontCamera = true,
            anchorY = 0.018f,
        )
    }
}
