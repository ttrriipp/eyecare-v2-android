package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.HeadOcclusionMask
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Pixel dimensions of the shared camera/SceneView surface for head masks. */
internal data class HeadOcclusionViewport(
    val widthPx: Float,
    val heightPx: Float,
)

/** Bounded, named values for side-region mask extraction. */
internal data class HeadOcclusionMappingConfig(
    val mirrorFrontCamera: Boolean = true,
    val confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD,
    val centralFaceMarginNorm: Float = DEFAULT_CENTRAL_FACE_MARGIN_NORM,
    /** Expands confident side pixels to cover the segmenter's soft boundary. */
    val boundaryPaddingPixels: Int = DEFAULT_BOUNDARY_PADDING_PIXELS,
) {

    init {
        require(
            confidenceThreshold.isFinite() &&
                confidenceThreshold in 0f..1f,
        ) {
            "Head confidence threshold must be between 0 and 1"
        }
        require(
            centralFaceMarginNorm.isFinite() &&
                centralFaceMarginNorm in 0f..MAX_CENTRAL_FACE_MARGIN_NORM,
        ) {
            "Central face margin must be between 0 and $MAX_CENTRAL_FACE_MARGIN_NORM"
        }
        require(boundaryPaddingPixels in 0..MAX_BOUNDARY_PADDING_PIXELS) {
            "Boundary padding must be between 0 and $MAX_BOUNDARY_PADDING_PIXELS pixels"
        }
    }

    private companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.6f
        const val DEFAULT_CENTRAL_FACE_MARGIN_NORM = 0.04f
        const val MAX_CENTRAL_FACE_MARGIN_NORM = 0.25f
        const val DEFAULT_BOUNDARY_PADDING_PIXELS = 2
        const val MAX_BOUNDARY_PADDING_PIXELS = 4
    }
}

/**
 * Converts a rotated segmenter result into a compact side-head/ear mask.
 *
 * Camera rotation is deliberately not repeated here: [ArFrameAnalyzer] rotates
 * the bitmap once before submitting the same MPImage to face and segmentation
 * tasks. This mapper therefore applies the same mirror and aspect-fill rules as
 * [mapFaceOccluder] to those already-rotated coordinates.
 */
internal fun mapHeadOcclusionMask(
    segmentation: HeadSegmentationFrame,
    face: FaceFrame,
    viewport: HeadOcclusionViewport,
    config: HeadOcclusionMappingConfig = HeadOcclusionMappingConfig(),
): HeadOcclusionMask? {
    if (
        segmentation.imageWidth <= 0 ||
        segmentation.imageHeight <= 0 ||
        segmentation.imageWidth.toLong() * segmentation.imageHeight.toLong() >
            HeadOcclusionMask.MAX_MASK_PIXELS ||
        !viewport.widthPx.isFinite() ||
        !viewport.heightPx.isFinite() ||
        viewport.widthPx <= 0f ||
        viewport.heightPx <= 0f ||
        face.imageWidth <= 0 ||
        face.imageHeight <= 0 ||
        !face.faceWidthNorm.isFinite() ||
        face.faceWidthNorm <= MIN_FACE_WIDTH_NORM ||
        face.faceWidthNorm > 1f ||
        !face.leftTempleX.isFinite() ||
        !face.rightTempleX.isFinite() ||
        face.leftTempleX !in 0f..1f ||
        face.rightTempleX !in 0f..1f
    ) {
        return null
    }

    val maskWidth = segmentation.imageWidth.toFloat()
    val maskHeight = segmentation.imageHeight.toFloat()
    val fillScale = max(
        viewport.widthPx / maskWidth,
        viewport.heightPx / maskHeight,
    )
    val scaledWidth = maskWidth * fillScale
    val scaledHeight = maskHeight * fillScale
    val cropX = (scaledWidth - viewport.widthPx) / 2f
    val cropY = (scaledHeight - viewport.heightPx) / 2f
    if (
        !fillScale.isFinite() ||
        !scaledWidth.isFinite() ||
        !scaledHeight.isFinite() ||
        !cropX.isFinite() ||
        !cropY.isFinite()
    ) {
        return null
    }

    val centralLeft = (
        min(face.leftTempleX, face.rightTempleX) - config.centralFaceMarginNorm
        ).coerceIn(0f, 1f)
    val centralRight = (
        max(face.leftTempleX, face.rightTempleX) + config.centralFaceMarginNorm
        ).coerceIn(0f, 1f)
    if (centralLeft >= centralRight) return null

    val alpha = ByteArray(segmentation.imageWidth * segmentation.imageHeight)
    var sourceActivePixelCount = 0
    var confidenceSum = 0f
    for (y in 0 until segmentation.imageHeight) {
        for (x in 0 until segmentation.imageWidth) {
            val sourceX = (x + 0.5f) / maskWidth
            // Keep the full central face/frame corridor visible. The segmenter
            // result is already in the same rotated image coordinates as face.
            if (sourceX in centralLeft..centralRight) continue

            val confidence = segmentation.confidenceAt(x, y)
            if (confidence < config.confidenceThreshold) continue

            val byteAlpha = (confidence * 255f).roundToInt().coerceIn(1, 255)
            alpha[y * segmentation.imageWidth + x] = byteAlpha.toByte()
            sourceActivePixelCount++
            confidenceSum += confidence
        }
    }
    if (sourceActivePixelCount == 0) return null

    // Selfie segmentation intentionally returns a soft person boundary. A
    // small source-space expansion prevents a temple from leaking through a
    // one- or two-pixel confidence gap, while reapplying the central corridor
    // guard keeps the front frame and face surface out of this side mask.
    var activePixelCount = sourceActivePixelCount
    for (y in 0 until segmentation.imageHeight) {
        for (x in 0 until segmentation.imageWidth) {
            val sourceConfidence = segmentation.confidenceAt(x, y)
            if (sourceConfidence < config.confidenceThreshold) continue

            val byteAlpha = (sourceConfidence * 255f).roundToInt().coerceIn(1, 255)
            val minX = (x - config.boundaryPaddingPixels).coerceAtLeast(0)
            val maxX = (x + config.boundaryPaddingPixels)
                .coerceAtMost(segmentation.imageWidth - 1)
            val minY = (y - config.boundaryPaddingPixels).coerceAtLeast(0)
            val maxY = (y + config.boundaryPaddingPixels)
                .coerceAtMost(segmentation.imageHeight - 1)
            for (expandedY in minY..maxY) {
                for (expandedX in minX..maxX) {
                    val expandedSourceX = (expandedX + 0.5f) / maskWidth
                    if (expandedSourceX in centralLeft..centralRight) continue

                    val expandedIndex = expandedY * segmentation.imageWidth + expandedX
                    val previousAlpha = alpha[expandedIndex].toInt() and 0xFF
                    if (previousAlpha == 0) activePixelCount++
                    if (byteAlpha > previousAlpha) {
                        alpha[expandedIndex] = byteAlpha.toByte()
                    }
                }
            }
        }
    }

    return HeadOcclusionMask.from(
        imageWidth = segmentation.imageWidth,
        imageHeight = segmentation.imageHeight,
        timestampMs = segmentation.timestampMs,
        viewportWidthPx = viewport.widthPx,
        viewportHeightPx = viewport.heightPx,
        fillScale = fillScale,
        cropX = cropX,
        cropY = cropY,
        mirrorFrontCamera = config.mirrorFrontCamera,
        confidence = (confidenceSum / sourceActivePixelCount).coerceIn(0f, 1f),
        activePixelCount = activePixelCount,
        alpha = alpha,
    )
}

private const val MIN_FACE_WIDTH_NORM = 0.0001f
