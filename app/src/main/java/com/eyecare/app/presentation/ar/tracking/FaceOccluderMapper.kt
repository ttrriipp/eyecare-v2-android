package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceMeshLandmarks
import kotlin.math.abs

/** Pixel dimensions of the shared camera/SceneView surface. */
internal data class FaceOccluderViewport(
    val widthPx: Float,
    val heightPx: Float,
)

/** Named, bounded calibration values for the face-relative depth projection. */
internal data class FaceOccluderMappingConfig(
    val mirrorFrontCamera: Boolean = true,
    val depthScale: Float = DEFAULT_FACE_DEPTH_SCALE,
    val depthBias: Float = DEFAULT_FACE_DEPTH_BIAS,
    val maxDepthOffset: Float = DEFAULT_MAX_DEPTH_OFFSET,
) {

    init {
        require(depthScale.isFinite() && depthScale in 0f..MAX_DEPTH_SCALE) {
            "Face depth scale must be finite and between 0 and $MAX_DEPTH_SCALE"
        }
        require(depthBias.isFinite() && abs(depthBias) <= MAX_DEPTH_BIAS) {
            "Face depth bias must be finite and within ±$MAX_DEPTH_BIAS"
        }
        require(
            maxDepthOffset.isFinite() &&
                maxDepthOffset in MIN_MAX_DEPTH_OFFSET..MAX_MAX_DEPTH_OFFSET &&
                abs(depthBias) <= maxDepthOffset,
        ) {
            "Face depth offset bound must contain the bias and stay within the supported range"
        }
    }

    private companion object {
        const val MAX_DEPTH_SCALE = 2f
        const val MAX_DEPTH_BIAS = 0.05f
        const val MIN_MAX_DEPTH_OFFSET = 0.01f
        const val MAX_MAX_DEPTH_OFFSET = 0.5f
    }
}

/**
 * Renderer-independent face vertices in SceneView screen coordinates.
 *
 * The depth value is a bounded offset relative to the nose reference. The
 * renderer adapter decides how that offset is applied along its camera ray.
 */
internal class FaceOccluderGeometry private constructor(
    private val values: FloatArray,
) {

    val vertexCount: Int
        get() = values.size / VALUES_PER_VERTEX

    fun xPx(index: Int): Float = values[offset(index)]

    fun yPx(index: Int): Float = values[offset(index) + 1]

    fun depthOffset(index: Int): Float = values[offset(index) + 2]

    fun interleavedValues(): FloatArray = values.copyOf()

    private fun offset(index: Int): Int {
        if (index !in 0 until FaceMeshLandmarks.LANDMARK_COUNT) {
            throw IndexOutOfBoundsException("Face occluder vertex index out of bounds: $index")
        }
        return index * VALUES_PER_VERTEX
    }

    companion object {
        private const val VALUES_PER_VERTEX = 3

        internal fun from(values: FloatArray): FaceOccluderGeometry? =
            values.takeIf { it.size == FaceMeshLandmarks.LANDMARK_COUNT * 3 }
                ?.takeIf { it.all(Float::isFinite) }
                ?.let(::FaceOccluderGeometry)
    }
}

/**
 * Maps one validated face mesh from MediaPipe image coordinates to the
 * aspect-filled SceneView surface without coupling the mapper to Android UI
 * or GPU classes.
 */
internal fun mapFaceOccluder(
    landmarks: FaceMeshLandmarks,
    imageWidth: Int,
    imageHeight: Int,
    faceWidthNorm: Float,
    viewport: FaceOccluderViewport,
    config: FaceOccluderMappingConfig = FaceOccluderMappingConfig(),
): FaceOccluderGeometry? {
    if (
        imageWidth <= 0 ||
        imageHeight <= 0 ||
        !faceWidthNorm.isFinite() ||
        faceWidthNorm <= MIN_FACE_WIDTH_NORM ||
        !viewport.widthPx.isFinite() ||
        !viewport.heightPx.isFinite() ||
        viewport.widthPx <= 0f ||
        viewport.heightPx <= 0f
    ) {
        return null
    }

    val imageWidthPx = imageWidth.toFloat()
    val imageHeightPx = imageHeight.toFloat()
    val fillScale = maxOf(
        viewport.widthPx / imageWidthPx,
        viewport.heightPx / imageHeightPx,
    )
    val scaledImageWidth = imageWidthPx * fillScale
    val scaledImageHeight = imageHeightPx * fillScale
    val cropX = (scaledImageWidth - viewport.widthPx) / 2f
    val cropY = (scaledImageHeight - viewport.heightPx) / 2f
    if (
        !fillScale.isFinite() ||
        !scaledImageWidth.isFinite() ||
        !scaledImageHeight.isFinite() ||
        !cropX.isFinite() ||
        !cropY.isFinite()
    ) {
        return null
    }

    val referenceDepth = landmarks.z(DEPTH_REFERENCE_LANDMARK)
    if (!referenceDepth.isFinite()) return null

    val values = FloatArray(FaceMeshLandmarks.LANDMARK_COUNT * VALUES_PER_VERTEX)
    for (index in 0 until FaceMeshLandmarks.LANDMARK_COUNT) {
        val x = landmarks.x(index)
        val y = landmarks.y(index)
        val z = landmarks.z(index)
        if (!x.isFinite() || !y.isFinite() || !z.isFinite()) return null

        // MediaPipe coordinates are not mirrored; the visible front-camera
        // surface is, so this is the one and only horizontal reflection.
        val imageX = if (config.mirrorFrontCamera) 1f - x else x
        val xPx = imageX * scaledImageWidth - cropX
        val yPx = y * scaledImageHeight - cropY

        // Relative z removes whole-face distance, then face width makes the
        // value scale-independent. The renderer applies the signed offset.
        val relativeDepth = ((z - referenceDepth) / faceWidthNorm) * config.depthScale
        val depthOffset = (relativeDepth + config.depthBias)
            .coerceIn(-config.maxDepthOffset, config.maxDepthOffset)
        if (!xPx.isFinite() || !yPx.isFinite() || !depthOffset.isFinite()) return null

        val offset = index * VALUES_PER_VERTEX
        values[offset] = xPx
        values[offset + 1] = yPx
        values[offset + 2] = depthOffset
    }
    return FaceOccluderGeometry.from(values)
}

private const val DEPTH_REFERENCE_LANDMARK = 168
private const val VALUES_PER_VERTEX = 3
private const val MIN_FACE_WIDTH_NORM = 0.0001f
private const val DEFAULT_FACE_DEPTH_SCALE = 0.5f
private const val DEFAULT_FACE_DEPTH_BIAS = 0.015f
private const val DEFAULT_MAX_DEPTH_OFFSET = 0.2f
