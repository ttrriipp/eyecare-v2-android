package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FacePoseCalibration
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import kotlin.math.cos
import kotlin.math.sin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FacePoseMapperTest {

    @Test
    fun mapsNeutralMatrixToCalibrationAnchor() {
        val pose = mapFacePose(
            matrix = transformationMatrix(),
            calibration = calibration(
                translationScale = 1f,
                mirrorFrontCamera = false,
                anchorX = 0.1f,
                anchorY = -0.2f,
                anchorZ = 0.3f,
            ),
        )

        assertNotNull(pose)
        assertEquals(0.1f, pose!!.translationX, EPSILON)
        assertEquals(-0.2f, pose.translationY, EPSILON)
        assertEquals(0.3f, pose.translationZ, EPSILON)
        assertEquals(0f, pose.pitchDeg, EPSILON)
        assertEquals(0f, pose.yawDeg, EPSILON)
        assertEquals(0f, pose.rollDeg, EPSILON)
        assertEquals(1f, pose.scale, EPSILON)
    }

    @Test
    fun mapsTranslationThroughMetricScaleAndFrontCameraMirror() {
        val pose = mapFacePose(
            matrix = transformationMatrix(translationX = 4f, translationY = -2f, translationZ = -10f),
            calibration = calibration(
                translationScale = 0.1f,
                mirrorFrontCamera = true,
                anchorX = 0.3f,
                anchorY = 0.2f,
                anchorZ = 0.1f,
            ),
        )

        assertNotNull(pose)
        assertEquals(-0.1f, pose!!.translationX, EPSILON)
        assertEquals(0f, pose.translationY, EPSILON)
        assertEquals(-0.9f, pose.translationZ, EPSILON)
    }

    @Test
    fun extractsPitchYawAndRollFromColumnMajorMatrix() {
        val pose = mapFacePose(
            matrix = transformationMatrix(pitchDeg = 12f, yawDeg = -20f, rollDeg = 15f),
            calibration = calibration(mirrorFrontCamera = false),
        )

        assertNotNull(pose)
        assertEquals(12f, pose!!.pitchDeg, ANGLE_EPSILON)
        assertEquals(-20f, pose.yawDeg, ANGLE_EPSILON)
        assertEquals(15f, pose.rollDeg, ANGLE_EPSILON)
    }

    @Test
    fun mirroringFlipsHorizontalTranslationYawAndRollOnly() {
        val pose = mapFacePose(
            matrix = transformationMatrix(pitchDeg = 12f, yawDeg = -20f, rollDeg = 15f, translationX = 2f),
            calibration = calibration(mirrorFrontCamera = true),
        )

        assertNotNull(pose)
        assertEquals(-2f, pose!!.translationX, EPSILON)
        assertEquals(12f, pose.pitchDeg, ANGLE_EPSILON)
        assertEquals(20f, pose.yawDeg, ANGLE_EPSILON)
        assertEquals(-15f, pose.rollDeg, ANGLE_EPSILON)
    }

    @Test
    fun combinesMatrixScaleWithExplicitCalibrationMultiplier() {
        val pose = mapFacePose(
            matrix = transformationMatrix(uniformScale = 2f),
            calibration = calibration(scaleMultiplier = 0.5f, mirrorFrontCamera = false),
        )

        assertNotNull(pose)
        assertEquals(1f, pose!!.scale, EPSILON)
    }

    @Test
    fun provisionalRoundFrameLiftsTheModelAboveTheTransformOrigin() {
        val pose = mapFacePose(
            matrix = transformationMatrix(),
            calibration = FacePoseCalibration.ProvisionalRoundFrame,
        )

        assertNotNull(pose)
        assertEquals(0.014f, pose!!.translationY, EPSILON)
    }

    @Test
    fun rejectsNonAffineDegenerateAndReflectedMatrices() {
        val nonAffine = transformationMatrix().toMutableArray().also { it[3] = 0.25f }
        val degenerate = transformationMatrix().toMutableArray().also { it[0] = 0f; it[5] = 0f }
        val reflected = transformationMatrix().toMutableArray().also { it[0] = -1f }
        val nonUniform = transformationMatrix().toMutableArray().also { it[0] = 2f }

        assertNull(mapFacePose(FaceTransformationMatrix.from(nonAffine)!!, calibration()))
        assertNull(mapFacePose(FaceTransformationMatrix.from(degenerate)!!, calibration()))
        assertNull(mapFacePose(FaceTransformationMatrix.from(reflected)!!, calibration()))
        assertNull(mapFacePose(FaceTransformationMatrix.from(nonUniform)!!, calibration()))
    }

    private fun calibration(
        translationScale: Float = 1f,
        scaleMultiplier: Float = 1f,
        mirrorFrontCamera: Boolean = true,
        anchorX: Float = 0f,
        anchorY: Float = 0f,
        anchorZ: Float = 0f,
    ): FacePoseCalibration = FacePoseCalibration(
        translationScale = translationScale,
        scaleMultiplier = scaleMultiplier,
        mirrorFrontCamera = mirrorFrontCamera,
        anchorX = anchorX,
        anchorY = anchorY,
        anchorZ = anchorZ,
    )

    private fun transformationMatrix(
        pitchDeg: Float = 0f,
        yawDeg: Float = 0f,
        rollDeg: Float = 0f,
        translationX: Float = 0f,
        translationY: Float = 0f,
        translationZ: Float = 0f,
        uniformScale: Float = 1f,
    ): FaceTransformationMatrix {
        val pitch = Math.toRadians(pitchDeg.toDouble())
        val yaw = Math.toRadians(yawDeg.toDouble())
        val roll = Math.toRadians(rollDeg.toDouble())
        val cp = cos(pitch)
        val sp = sin(pitch)
        val cy = cos(yaw)
        val sy = sin(yaw)
        val cr = cos(roll)
        val sr = sin(roll)

        // R = Rz(roll) * Ry(yaw) * Rx(pitch), serialized column-major.
        val rowMajor = doubleArrayOf(
            cr * cy, cr * sy * sp - sr * cp, cr * sy * cp + sr * sp, translationX.toDouble(),
            sr * cy, sr * sy * sp + cr * cp, sr * sy * cp - cr * sp, translationY.toDouble(),
            -sy, cy * sp, cy * cp, translationZ.toDouble(),
            0.0, 0.0, 0.0, 1.0,
        )
        val columnMajor = FloatArray(16)
        for (row in 0..3) {
            for (column in 0..3) {
                val value = rowMajor[row * 4 + column]
                columnMajor[column * 4 + row] = if (row < 3 && column < 3) {
                    (value * uniformScale).toFloat()
                } else {
                    value.toFloat()
                }
            }
        }
        return FaceTransformationMatrix.from(columnMajor)!!
    }

    private fun FaceTransformationMatrix.toMutableArray(): FloatArray = values.toFloatArray()

    private companion object {
        const val EPSILON = 0.0001f
        const val ANGLE_EPSILON = 0.1f
    }
}
