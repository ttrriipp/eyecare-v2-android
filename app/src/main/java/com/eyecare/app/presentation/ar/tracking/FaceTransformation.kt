package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix

/**
 * Extracts exactly one valid matrix for the one-face configuration.
 *
 * MediaPipe returns one matrix per detected face when matrix output is enabled. Treating any
 * other cardinality as non-tracking prevents an ambiguous or stale transform from reaching a
 * renderer.
 */
fun extractSingleFaceTransformationMatrix(
    matrices: List<FloatArray>,
): FaceTransformationMatrix? {
    if (matrices.size != 1) return null
    return FaceTransformationMatrix.from(matrices.single())
}
