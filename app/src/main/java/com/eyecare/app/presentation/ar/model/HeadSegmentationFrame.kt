package com.eyecare.app.presentation.ar.model

/**
 * Immutable confidence mask copied from MediaPipe's Image Segmenter result.
 *
 * The values are row-major and normalized to [0, 1]. Framework-owned image
 * objects never leave the segmenter boundary.
 */
class HeadSegmentationFrame private constructor(
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    private val confidence: FloatArray,
) {

    fun isInside(x: Int, y: Int): Boolean =
        x in 0 until imageWidth && y in 0 until imageHeight

    fun confidenceAt(x: Int, y: Int): Float =
        if (isInside(x, y)) confidence[y * imageWidth + x] else 0f

    fun copyConfidence(): FloatArray = confidence.copyOf()

    companion object {
        fun from(
            imageWidth: Int,
            imageHeight: Int,
            timestampMs: Long,
            confidence: FloatArray,
        ): HeadSegmentationFrame? {
            if (imageWidth <= 0 || imageHeight <= 0 || timestampMs < 0L) return null
            if (confidence.size != imageWidth * imageHeight) return null
            if (confidence.any { !it.isFinite() || it !in 0f..1f }) return null
            return HeadSegmentationFrame(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestampMs = timestampMs,
                confidence = confidence.copyOf(),
            )
        }
    }
}
