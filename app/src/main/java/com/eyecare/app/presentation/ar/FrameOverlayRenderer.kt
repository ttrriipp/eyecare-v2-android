package com.eyecare.app.presentation.ar

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.toBitmap
import com.eyecare.app.presentation.ar.model.FaceFrame

@Composable
fun FrameOverlayRenderer(
    face: FaceFrame,
    frameAssetUrl: String?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var frameBitmap by remember(frameAssetUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(frameAssetUrl) {
        if (frameAssetUrl == null) return@LaunchedEffect
        val result = imageLoader.execute(
            ImageRequest.Builder(context).data(frameAssetUrl).build()
        )
        if (result is SuccessResult) frameBitmap = result.image.toBitmap()
    }

    val bitmap = frameBitmap

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val viewW = size.width
            val viewH = size.height
            val centerX = (1f - face.noseBridgeX) * viewW
            val centerY = face.noseBridgeY * viewH
            val frameWidthPx = face.faceWidthNorm * viewW * 1.6f

            if (bitmap != null) {
                val scale = frameWidthPx / bitmap.width
                val frameH = bitmap.height * scale
                val matrix = Matrix().apply {
                    postScale(scale, scale)
                    postTranslate(centerX - frameWidthPx / 2f, centerY - frameH * 0.35f)
                    postRotate(-face.rotationDeg, centerX, centerY)
                }
                canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
            } else {
                // Wireframe placeholder — visible while asset loads or if no URL provided
                val paint = Paint().apply {
                    color = Color(0xFF4A90E2).toArgb()
                    strokeWidth = 4f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val h = frameWidthPx * 0.35f
                val top = centerY - h * 0.7f
                // Left lens
                canvas.nativeCanvas.drawRoundRect(
                    RectF(centerX - frameWidthPx / 2f, top, centerX - frameWidthPx * 0.05f, top + h),
                    16f, 16f, paint,
                )
                // Right lens
                canvas.nativeCanvas.drawRoundRect(
                    RectF(centerX + frameWidthPx * 0.05f, top, centerX + frameWidthPx / 2f, top + h),
                    16f, 16f, paint,
                )
                // Bridge
                canvas.nativeCanvas.drawLine(
                    centerX - frameWidthPx * 0.05f, top + h * 0.5f,
                    centerX + frameWidthPx * 0.05f, top + h * 0.5f,
                    paint,
                )
            }
        }
    }
}
