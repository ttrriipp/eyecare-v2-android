package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.ArTrackingQuality
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FaceTrackingQualityTest {

    @Test
    fun `centered level frontal face is stable`() {
        assertEquals(
            ArTrackingQuality.Stable,
            classifyFaceTrackingQuality(face(), pose()),
        )
    }

    @Test
    fun `off-center face asks user to center face before orientation`() {
        assertEquals(
            ArTrackingQuality.CenterFace,
            classifyFaceTrackingQuality(face(noseBridgeX = 0.7f), pose(yawDeg = 30f)),
        )
    }

    @Test
    fun `turned face asks user to look straight`() {
        assertEquals(
            ArTrackingQuality.LookStraight,
            classifyFaceTrackingQuality(face(), pose(yawDeg = 24f)),
        )
    }

    @Test
    fun `slight side turn remains stable for a natural preview angle`() {
        assertEquals(
            ArTrackingQuality.Stable,
            classifyFaceTrackingQuality(face(), pose(yawDeg = 18f)),
        )
    }

    @Test
    fun `tilted face asks user to level head`() {
        assertEquals(
            ArTrackingQuality.LevelHead,
            classifyFaceTrackingQuality(face(), pose(rollDeg = 16f)),
        )
    }

    @Test
    fun `missing pose remains stabilizing`() {
        assertEquals(
            ArTrackingQuality.Stabilizing,
            classifyFaceTrackingQuality(face(), null),
        )
    }

    private fun face(
        noseBridgeX: Float = 0.5f,
        noseBridgeY: Float = 0.5f,
    ) = FaceFrame(
        noseBridgeX = noseBridgeX,
        noseBridgeY = noseBridgeY,
        leftTempleX = 0.3f,
        rightTempleX = 0.7f,
        faceWidthNorm = 0.4f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = IDENTITY_MATRIX,
        timestampMs = 0L,
    )

    private fun pose(
        pitchDeg: Float = 0f,
        yawDeg: Float = 0f,
        rollDeg: Float = 0f,
    ) = FacePose(
        translationX = 0f,
        translationY = 0f,
        translationZ = 0f,
        pitchDeg = pitchDeg,
        yawDeg = yawDeg,
        rollDeg = rollDeg,
        scale = 1f,
    )

    private companion object {
        val IDENTITY_MATRIX = FaceTransformationMatrix.from(
            floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ),
        )!!
    }
}
