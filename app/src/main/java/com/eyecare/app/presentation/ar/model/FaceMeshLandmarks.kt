package com.eyecare.app.presentation.ar.model

/**
 * Immutable copy of one MediaPipe face mesh.
 *
 * Coordinates are stored as interleaved normalised x/y/z triples. Keeping the
 * values in one primitive buffer avoids creating hundreds of Kotlin point
 * objects for every live camera result while preventing MediaPipe-owned data
 * from leaking into presentation state.
 */
class FaceMeshLandmarks private constructor(
    private val values: FloatArray,
) {

    fun x(index: Int): Float = values[offset(index)]

    fun y(index: Int): Float = values[offset(index) + 1]

    fun z(index: Int): Float = values[offset(index) + 2]

    private fun offset(index: Int): Int {
        if (index !in 0 until LANDMARK_COUNT) {
            throw IndexOutOfBoundsException("Face landmark index out of bounds: $index")
        }
        return index * COMPONENTS_PER_LANDMARK
    }

    companion object {
        const val LANDMARK_COUNT = 478
        private const val COMPONENTS_PER_LANDMARK = 3
        const val VALUE_COUNT = LANDMARK_COUNT * COMPONENTS_PER_LANDMARK

        /** Returns null when the result is not a complete finite face mesh. */
        fun from(rawValues: FloatArray): FaceMeshLandmarks? {
            if (rawValues.size != VALUE_COUNT || rawValues.any { !it.isFinite() }) {
                return null
            }
            return FaceMeshLandmarks(rawValues.copyOf())
        }
    }
}
