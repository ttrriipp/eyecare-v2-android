package com.eyecare.app.presentation.ar.model

/**
 * Compact, immutable side-head mask ready for the renderer boundary.
 *
 * The byte buffer stays at the segmenter's resolution. The viewport transform is
 * retained alongside it so a renderer or debug overlay can sample the same
 * source pixels without allocating a viewport-sized bitmap on every frame.
 */
class HeadOcclusionMask private constructor(
    val imageWidth: Int,
    val imageHeight: Int,
    val timestampMs: Long,
    val viewportWidthPx: Float,
    val viewportHeightPx: Float,
    val fillScale: Float,
    val cropX: Float,
    val cropY: Float,
    val mirrorFrontCamera: Boolean,
    val confidence: Float,
    val activePixelCount: Int,
    private val alpha: ByteArray,
) {

    /** Returns a confidence-weighted alpha in [0, 255] for one source pixel. */
    fun alphaAt(x: Int, y: Int): Int =
        if (x in 0 until imageWidth && y in 0 until imageHeight) {
            alpha[y * imageWidth + x].toInt() and 0xFF
        } else {
            0
        }

    fun isActive(x: Int, y: Int): Boolean = alphaAt(x, y) > 0

    /** Copy only when a boundary explicitly needs ownership of the raw buffer. */
    fun copyAlpha(): ByteArray = alpha.copyOf()

    /** Maps the centre of a source pixel to the visible aspect-filled viewport. */
    fun viewportXForSourcePixel(x: Int): Float {
        require(x in 0 until imageWidth) { "Source x out of bounds: $x" }
        val sourceX = (x + 0.5f) / imageWidth.toFloat()
        val visibleX = if (mirrorFrontCamera) 1f - sourceX else sourceX
        return visibleX * imageWidth.toFloat() * fillScale - cropX
    }

    /** Maps the centre of a source pixel to the visible aspect-filled viewport. */
    fun viewportYForSourcePixel(y: Int): Float {
        require(y in 0 until imageHeight) { "Source y out of bounds: $y" }
        val sourceY = (y + 0.5f) / imageHeight.toFloat()
        return sourceY * imageHeight.toFloat() * fillScale - cropY
    }

    companion object {
        internal const val MAX_MASK_PIXELS = 1_048_576

        internal fun from(
            imageWidth: Int,
            imageHeight: Int,
            timestampMs: Long,
            viewportWidthPx: Float,
            viewportHeightPx: Float,
            fillScale: Float,
            cropX: Float,
            cropY: Float,
            mirrorFrontCamera: Boolean,
            confidence: Float,
            activePixelCount: Int,
            alpha: ByteArray,
        ): HeadOcclusionMask? {
            if (
                imageWidth <= 0 ||
                imageHeight <= 0 ||
                imageWidth.toLong() * imageHeight.toLong() > MAX_MASK_PIXELS ||
                timestampMs < 0L ||
                !viewportWidthPx.isFinite() ||
                !viewportHeightPx.isFinite() ||
                viewportWidthPx <= 0f ||
                viewportHeightPx <= 0f ||
                !fillScale.isFinite() ||
                fillScale <= 0f ||
                !cropX.isFinite() ||
                !cropY.isFinite() ||
                !confidence.isFinite() ||
                confidence !in 0f..1f ||
                activePixelCount <= 0 ||
                activePixelCount > imageWidth * imageHeight ||
                alpha.size != imageWidth * imageHeight
            ) {
                return null
            }
            return HeadOcclusionMask(
                imageWidth = imageWidth,
                imageHeight = imageHeight,
                timestampMs = timestampMs,
                viewportWidthPx = viewportWidthPx,
                viewportHeightPx = viewportHeightPx,
                fillScale = fillScale,
                cropX = cropX,
                cropY = cropY,
                mirrorFrontCamera = mirrorFrontCamera,
                confidence = confidence,
                activePixelCount = activePixelCount,
                alpha = alpha.copyOf(),
            )
        }
    }
}
