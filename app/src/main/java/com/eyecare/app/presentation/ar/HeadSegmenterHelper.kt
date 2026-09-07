package com.eyecare.app.presentation.ar

import android.content.Context
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import com.google.mediapipe.framework.image.ByteBufferExtractor
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenterResult
import java.nio.ByteOrder

/**
 * Owns the MediaPipe Image Segmenter and exposes only immutable app data.
 *
 * The caller supplies a shared, already-rotated MPImage so face tracking and
 * segmentation can consume one camera frame without competing for ImageProxy
 * ownership.
 */
internal class HeadSegmenterHelper(
    context: Context,
    private val onResult: (HeadSegmentationFrame) -> Unit,
    private val onError: () -> Unit,
) : AutoCloseable {

    private var segmenter: ImageSegmenter? = null
    private var closed = false

    init {
        runCatching {
            val options = ImageSegmenter.ImageSegmenterOptions.builder()
                .setBaseOptions(
                    BaseOptions.builder()
                        .setModelAssetPath(HeadSegmenterConfig.MODEL_ASSET)
                        .build(),
                )
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setOutputCategoryMask(false)
                .setOutputConfidenceMasks(HeadSegmenterConfig.USE_CONFIDENCE_MASK)
                .setResultListener(::handleResult)
                .setErrorListener { onError() }
                .build()
            ImageSegmenter.createFromOptions(context, options)
        }.onSuccess { created ->
            segmenter = created
        }.onFailure {
            onError()
        }
    }

    /** Submits one already-prepared camera image without taking ownership of it. */
    fun segmentAsync(image: MPImage, timestampMs: Long): Boolean {
        if (closed || timestampMs < 0L) return false
        val current = segmenter ?: return false
        return runCatching {
            current.segmentAsync(image, timestampMs)
            true
        }.getOrElse {
            onError()
            false
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        segmenter?.close()
        segmenter = null
    }

    @Suppress("UNUSED_PARAMETER")
    private fun handleResult(result: ImageSegmenterResult, inputImage: MPImage) {
        if (closed) return
        val mask = result.confidenceMasks().orElse(emptyList()).firstOrNull()
        if (mask == null) {
            onError()
            return
        }

        val width = mask.width
        val height = mask.height
        val expectedValues = width.toLong() * height.toLong()
        if (width <= 0 || height <= 0 || expectedValues > Int.MAX_VALUE / Float.SIZE_BYTES) {
            onError()
            return
        }

        val buffer = runCatching {
            ByteBufferExtractor.extract(mask)
                .duplicate()
                .order(ByteOrder.nativeOrder())
        }.getOrElse {
            onError()
            return
        }
        if (buffer.remaining() < expectedValues * Float.SIZE_BYTES) {
            onError()
            return
        }

        val values = FloatArray(expectedValues.toInt())
        buffer.asFloatBuffer().get(values)
        val frame = HeadSegmentationFrame.from(
            imageWidth = width,
            imageHeight = height,
            timestampMs = result.timestampMs(),
            confidence = values,
        )
        if (frame == null) {
            onError()
        } else {
            onResult(frame)
        }
    }
}
