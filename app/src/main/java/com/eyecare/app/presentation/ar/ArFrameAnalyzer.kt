package com.eyecare.app.presentation.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder

/**
 * Converts one CameraX frame once and submits it to both vision tasks.
 *
 * The analyzer owns the ImageProxy lifetime. The task helpers only borrow the
 * resulting MPImage for asynchronous inference.
 */
internal class ArFrameAnalyzer(
    context: Context,
    onFaceResult: (com.eyecare.app.presentation.ar.model.ArFaceState) -> Unit,
) : AutoCloseable {

    private val pairer = ArFrameMaskPairer(onFaceResult)
    private val faceLandmarker = FaceLandmarkerHelper(context, pairer::onFaceResult)
    private val segmenter = HeadSegmenterHelper(
        context = context,
        onResult = pairer::onMaskResult,
        onError = {},
    )
    private val rotationMatrix = Matrix()
    private var closed = false

    fun analyze(imageProxy: ImageProxy) {
        imageProxy.use { proxy ->
            if (closed) return@use

            val timestampMs = SystemClock.uptimeMillis()
            val mpImage = runCatching {
                val bitmap = proxy.toBitmap().rotate(proxy.imageInfo.rotationDegrees.toFloat())
                BitmapImageBuilder(bitmap).build()
            }.getOrElse {
                pairer.onFaceResult(
                    com.eyecare.app.presentation.ar.model.ArFaceState.NoFace,
                )
                return@use
            }

            faceLandmarker.detectAsync(mpImage, timestampMs)
            segmenter.segmentAsync(mpImage, timestampMs)
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        segmenter.close()
        faceLandmarker.close()
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        if (degrees == 0f) return this
        rotationMatrix.reset()
        rotationMatrix.postRotate(degrees)
        return Bitmap.createBitmap(this, 0, 0, width, height, rotationMatrix, true)
    }
}
