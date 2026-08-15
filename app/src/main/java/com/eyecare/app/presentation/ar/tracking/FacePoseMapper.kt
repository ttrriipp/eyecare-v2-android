package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FacePose
import com.eyecare.app.presentation.ar.model.FacePoseCalibration
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sqrt

private const val AFFINE_TOLERANCE = 0.001f
private const val ORTHONORMAL_TOLERANCE = 0.05f
private const val MIN_AXIS_LENGTH = 0.0001f
private const val MAX_AXIS_LENGTH = 100f
private const val GIMBAL_LOCK_COSINE = 0.0001f
private const val RADIANS_TO_DEGREES = 180.0 / Math.PI

/**
 * Converts MediaPipe's column-major canonical-face transform to renderer coordinates.
 *
 * The source matrix is expected to be an affine, proper transform. A front-camera mirror is
 * applied as a reflection of the renderer's horizontal axis: X translation, yaw, and roll change
 * sign while pitch remains unchanged. Returning null for malformed or non-physical transforms
 * prevents an unbounded pose from reaching SceneView.
 */
fun mapFacePose(
    matrix: FaceTransformationMatrix,
    calibration: FacePoseCalibration,
): FacePose? {
    val m00 = matrix.element(row = 0, column = 0)
    val m01 = matrix.element(row = 0, column = 1)
    val m02 = matrix.element(row = 0, column = 2)
    val m10 = matrix.element(row = 1, column = 0)
    val m11 = matrix.element(row = 1, column = 1)
    val m12 = matrix.element(row = 1, column = 2)
    val m20 = matrix.element(row = 2, column = 0)
    val m21 = matrix.element(row = 2, column = 1)
    val m22 = matrix.element(row = 2, column = 2)

    if (
        abs(matrix.element(row = 3, column = 0)) > AFFINE_TOLERANCE ||
        abs(matrix.element(row = 3, column = 1)) > AFFINE_TOLERANCE ||
        abs(matrix.element(row = 3, column = 2)) > AFFINE_TOLERANCE ||
        abs(matrix.element(row = 3, column = 3) - 1f) > AFFINE_TOLERANCE
    ) {
        return null
    }

    val xLength = length(m00, m10, m20)
    val yLength = length(m01, m11, m21)
    val zLength = length(m02, m12, m22)
    if (
        !xLength.isFinite() || !yLength.isFinite() || !zLength.isFinite() ||
        xLength < MIN_AXIS_LENGTH || yLength < MIN_AXIS_LENGTH || zLength < MIN_AXIS_LENGTH ||
        xLength > MAX_AXIS_LENGTH || yLength > MAX_AXIS_LENGTH || zLength > MAX_AXIS_LENGTH
    ) {
        return null
    }

    val maximumLength = maxOf(xLength, yLength, zLength)
    val minimumLength = minOf(xLength, yLength, zLength)
    if (maximumLength / minimumLength - 1f > ORTHONORMAL_TOLERANCE) {
        return null
    }

    val r00 = m00 / xLength
    val r10 = m10 / xLength
    val r20 = m20 / xLength
    val r01 = m01 / yLength
    val r11 = m11 / yLength
    val r21 = m21 / yLength
    val r02 = m02 / zLength
    val r12 = m12 / zLength
    val r22 = m22 / zLength
    if (
        abs(r00 * r01 + r10 * r11 + r20 * r21) > ORTHONORMAL_TOLERANCE ||
        abs(r00 * r02 + r10 * r12 + r20 * r22) > ORTHONORMAL_TOLERANCE ||
        abs(r01 * r02 + r11 * r12 + r21 * r22) > ORTHONORMAL_TOLERANCE
    ) {
        return null
    }

    val determinant = determinant(
        r00, r01, r02,
        r10, r11, r12,
        r20, r21, r22,
    )
    if (!determinant.isFinite() || determinant <= 0f) return null

    val yawRadians = asin((-r20).coerceIn(-1f, 1f))
    val cosineYaw = cos(yawRadians)
    val pitchRadians: Double
    val rollRadians: Double
    if (abs(cosineYaw) > GIMBAL_LOCK_COSINE) {
        pitchRadians = atan2(r21.toDouble(), r22.toDouble())
        rollRadians = atan2(r10.toDouble(), r00.toDouble())
    } else {
        // At gimbal lock yaw remains deterministic; expose the remaining rotation as pitch and
        // pin roll to zero rather than emitting an unstable arbitrary angle.
        pitchRadians = atan2(-r01.toDouble(), r11.toDouble())
        rollRadians = 0.0
    }

    val mirrorSign = if (calibration.mirrorFrontCamera) -1f else 1f
    val translationX = matrix.element(row = 0, column = 3) * mirrorSign * calibration.translationScale +
        calibration.anchorX
    val translationY = matrix.element(row = 1, column = 3) * calibration.translationScale +
        calibration.anchorY
    val translationZ = matrix.element(row = 2, column = 3) * calibration.translationScale +
        calibration.anchorZ
    val pitchDeg = (pitchRadians * RADIANS_TO_DEGREES).toFloat() + calibration.pitchOffsetDeg
    val yawDeg = (yawRadians * RADIANS_TO_DEGREES).toFloat() * mirrorSign + calibration.yawOffsetDeg
    val rollDeg = (rollRadians * RADIANS_TO_DEGREES).toFloat() * mirrorSign + calibration.rollOffsetDeg
    val scale = ((xLength + yLength + zLength) / 3f) * calibration.scaleMultiplier

    if (
        !translationX.isFinite() || !translationY.isFinite() || !translationZ.isFinite() ||
        !pitchDeg.isFinite() || !yawDeg.isFinite() || !rollDeg.isFinite() ||
        !scale.isFinite() || scale <= 0f
    ) {
        return null
    }

    return FacePose(
        translationX = translationX,
        translationY = translationY,
        translationZ = translationZ,
        pitchDeg = pitchDeg,
        yawDeg = yawDeg,
        rollDeg = rollDeg,
        scale = scale,
    )
}

private fun FaceTransformationMatrix.element(row: Int, column: Int): Float = this[column * 4 + row]

private fun length(x: Float, y: Float, z: Float): Float =
    sqrt(x * x + y * y + z * z)

private fun determinant(
    m00: Float,
    m01: Float,
    m02: Float,
    m10: Float,
    m11: Float,
    m12: Float,
    m20: Float,
    m21: Float,
    m22: Float,
): Float =
    m00 * (m11 * m22 - m12 * m21) -
        m01 * (m10 * m22 - m12 * m20) +
        m02 * (m10 * m21 - m11 * m20)
