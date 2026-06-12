package com.eyecare.app.presentation.ar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarkerResult
import kotlin.math.atan2
import kotlin.math.sqrt

private const val MODEL_ASSET = "face_landmarker.task"

// Key landmark indices from MediaPipe Face Mesh 478-point model
private const val NOSE_BRIDGE_1 = 6
private const val NOSE_BRIDGE_2 = 168
private const val LEFT_TEMPLE = 234
private const val RIGHT_TEMPLE = 454

class FaceLandmarkerHelper(
    context: Context,
    private val onResult: (ArFaceState) -> Unit,
) {
    private var landmarker: FaceLandmarker? = null

    init {
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(
                BaseOptions.builder()
                    .setModelAssetPath(MODEL_ASSET)
                    .build()
            )
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumFaces(1)
            .setResultListener(::handleResult)
            .setErrorListener { e -> onResult(ArFaceState.NoFace) }
            .build()

        runCatching {
            landmarker = FaceLandmarker.createFromOptions(context, options)
        }.onFailure { onResult(ArFaceState.NoFace) }
    }

    /** Called from CameraX ImageAnalysis on each frame. */
    fun detectAsync(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap().rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(bitmap).build()
        runCatching {
            landmarker?.detectAsync(mpImage, SystemClock.uptimeMillis())
        }
        imageProxy.close()
    }

    fun close() {
        landmarker?.close()
        landmarker = null
    }

    private fun handleResult(result: FaceLandmarkerResult, input: com.google.mediapipe.framework.image.MPImage) {
        if (result.faceLandmarks().isEmpty()) {
            onResult(ArFaceState.NoFace)
            return
        }
        val landmarks = result.faceLandmarks()[0]
        val w = input.width
        val h = input.height

        fun lm(idx: Int) = landmarks[idx]

        val noseBridgeX = (lm(NOSE_BRIDGE_1).x() + lm(NOSE_BRIDGE_2).x()) / 2f
        val noseBridgeY = (lm(NOSE_BRIDGE_1).y() + lm(NOSE_BRIDGE_2).y()) / 2f
        val leftX = lm(LEFT_TEMPLE).x()
        val rightX = lm(RIGHT_TEMPLE).x()
        val faceWidth = rightX - leftX

        // Compute roll angle from nose bridge pair
        val dx = lm(NOSE_BRIDGE_2).x() - lm(NOSE_BRIDGE_1).x()
        val dy = lm(NOSE_BRIDGE_2).y() - lm(NOSE_BRIDGE_1).y()
        val rotationDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()

        onResult(
            ArFaceState.Detected(
                FaceFrame(
                    noseBridgeX = noseBridgeX,
                    noseBridgeY = noseBridgeY,
                    leftTempleX = leftX,
                    rightTempleX = rightX,
                    faceWidthNorm = faceWidth,
                    rotationDeg = rotationDeg,
                    imageWidth = w,
                    imageHeight = h,
                )
            )
        )
    }

    private fun Bitmap.rotate(degrees: Float): Bitmap {
        if (degrees == 0f) return this
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}
