package com.eyecare.app.presentation.ar

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
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

    val bitmap = frameBitmap ?: return

    Canvas(modifier = modifier.fillMaxSize()) {
        drawIntoCanvas { canvas ->
            val viewW = size.width
            val viewH = size.height
            val centerX = (1f - face.noseBridgeX) * viewW
            val centerY = face.noseBridgeY * viewH
            val frameWidthPx = face.faceWidthNorm * viewW * 1.6f
            val scale = frameWidthPx / bitmap.width
            val frameH = bitmap.height * scale

            val matrix = Matrix().apply {
                postScale(scale, scale)
                postTranslate(centerX - frameWidthPx / 2f, centerY - frameH * 0.35f)
                postRotate(-face.rotationDeg, centerX, centerY)
            }
            canvas.nativeCanvas.drawBitmap(bitmap, matrix, null)
        }
    }
}
