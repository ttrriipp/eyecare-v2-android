package com.eyecare.app.presentation.ar.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.tracking.HeadOcclusionViewport
import com.eyecare.app.presentation.ar.tracking.mapHeadOcclusionMask
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Debug-only visualization of the mapped side-head mask.
 *
 * Keeping this as a separate layer lets us validate rotation, mirror, crop,
 * ear coverage, and central-face exclusion before changing the Filament
 * composition. It is never composed in release builds by the caller.
 */
@Composable
internal fun HeadOcclusionDebugOverlay(
    face: FaceFrame?,
    modifier: Modifier = Modifier,
) {
    val currentFace = face ?: return
    val segmentation = currentFace.headSegmentation ?: return
    var viewportSize by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .onSizeChanged { viewportSize = it },
    ) {
        if (viewportSize.width <= 0 || viewportSize.height <= 0) return@Box

        val mask = remember(
            currentFace.timestampMs,
            segmentation,
            viewportSize,
        ) {
            mapHeadOcclusionMask(
                segmentation = segmentation,
                face = currentFace,
                viewport = HeadOcclusionViewport(
                    widthPx = viewportSize.width.toFloat(),
                    heightPx = viewportSize.height.toFloat(),
                ),
            )
        } ?: return@Box

        Canvas(Modifier.fillMaxSize()) {
            // Debug drawing is intentionally downsampled. The renderer will
            // sample the original compact buffer rather than these rectangles.
            val stepX = max(1, mask.imageWidth / DEBUG_GRID_SIZE)
            val stepY = max(1, mask.imageHeight / DEBUG_GRID_SIZE)
            for (y in 0 until mask.imageHeight step stepY) {
                for (x in 0 until mask.imageWidth step stepX) {
                    val alpha = mask.alphaAt(x, y)
                    if (alpha == 0) continue

                    val endX = min(mask.imageWidth - 1, x + stepX - 1)
                    val endY = min(mask.imageHeight - 1, y + stepY - 1)
                    val x0 = mask.viewportXForSourcePixel(x)
                    val x1 = mask.viewportXForSourcePixel(endX)
                    val y0 = mask.viewportYForSourcePixel(y)
                    val y1 = mask.viewportYForSourcePixel(endY)
                    val cellHalf = mask.fillScale * 0.5f
                    val left = min(x0, x1) - cellHalf
                    val top = min(y0, y1) - cellHalf
                    val width = abs(x1 - x0) + mask.fillScale
                    val height = abs(y1 - y0) + mask.fillScale

                    drawRect(
                        color = DEBUG_MASK_COLOR.copy(
                            alpha = DEBUG_MIN_ALPHA +
                                (DEBUG_MAX_ALPHA - DEBUG_MIN_ALPHA) * alpha / 255f,
                        ),
                        topLeft = Offset(left, top),
                        size = Size(width, height),
                    )
                }
            }
        }
    }
}

private const val DEBUG_GRID_SIZE = 64
private const val DEBUG_MIN_ALPHA = 0.12f
private const val DEBUG_MAX_ALPHA = 0.32f
private val DEBUG_MASK_COLOR = Color(0xFFFF1744)
