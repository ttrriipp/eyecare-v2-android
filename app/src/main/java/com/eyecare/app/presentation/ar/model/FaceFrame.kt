package com.eyecare.app.presentation.ar.model

/**
 * One validated MediaPipe facial transformation matrix.
 *
 * Values preserve the 16-value order emitted by Face Landmarker.
 * The immutable list prevents a MediaPipe-owned mutable array from leaking into presentation
 * state or changing after the callback returns.
 */
class FaceTransformationMatrix private constructor(
    values: List<Float>,
) {
    val values: List<Float> = values.toList()

    init {
        require(this.values.size == MATRIX_ELEMENT_COUNT) {
            "Facial transformation matrix must contain exactly 16 values"
        }
        require(this.values.all(Float::isFinite)) {
            "Facial transformation matrix must contain only finite values"
        }
    }

    operator fun get(index: Int): Float = values[index]

    override fun equals(other: Any?): Boolean =
        other is FaceTransformationMatrix && values == other.values

    override fun hashCode(): Int = values.hashCode()

    companion object {
        const val MATRIX_ELEMENT_COUNT = 16

        fun from(rawValues: FloatArray): FaceTransformationMatrix? {
            if (rawValues.size != MATRIX_ELEMENT_COUNT || rawValues.any { !it.isFinite() }) {
                return null
            }
            return FaceTransformationMatrix(rawValues.toList())
        }
    }
}

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
    /** Validated canonical-face-to-detected-face transform emitted by MediaPipe. */
    val transformationMatrix: FaceTransformationMatrix,
    /** MediaPipe timestamp associated with this detection. */
    val timestampMs: Long,
)

sealed interface ArFaceState {
    /** MediaPipe detected a face and computed geometry */
    data class Detected(val frame: FaceFrame) : ArFaceState
    /** No face in current frame */
    data object NoFace : ArFaceState
    /** Landmarker not yet initialised or closed */
    data object Initialising : ArFaceState
}
