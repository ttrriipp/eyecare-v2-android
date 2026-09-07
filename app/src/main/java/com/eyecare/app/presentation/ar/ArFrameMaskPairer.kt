package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import java.util.LinkedHashMap

/**
 * Pairs asynchronous face and segmentation callbacks without allowing an old
 * mask to move the current frame backwards.
 */
internal class ArFrameMaskPairer(
    private val publishFaceResult: (ArFaceState) -> Unit,
) {

    private val facesByTimestamp = LinkedHashMap<Long, FaceFrame>()
    private val masksByTimestamp = LinkedHashMap<Long, HeadSegmentationFrame>()
    private var latestTimestampMs = Long.MIN_VALUE

    fun onFaceResult(state: ArFaceState) {
        val output = synchronized(this) {
            when (state) {
                is ArFaceState.Detected -> onDetectedFace(state.frame)
                ArFaceState.NoFace,
                ArFaceState.Initialising,
                -> {
                    clearLocked()
                    state
                }
            }
        }
        output?.let(publishFaceResult)
    }

    fun onMaskResult(mask: HeadSegmentationFrame) {
        val output = synchronized(this) {
            if (mask.timestampMs < latestTimestampMs) {
                null
            } else {
                masksByTimestamp[mask.timestampMs] = mask
                facesByTimestamp[mask.timestampMs]?.let { face ->
                    if (mask.timestampMs == latestTimestampMs) {
                        val updated = face.copy(headSegmentation = mask)
                        facesByTimestamp[mask.timestampMs] = updated
                        ArFaceState.Detected(updated)
                    } else {
                        null
                    }
                }
            }
        }
        output?.let(publishFaceResult)
    }

    private fun onDetectedFace(face: FaceFrame): ArFaceState? {
        if (face.timestampMs < latestTimestampMs) {
            return null
        }

        latestTimestampMs = face.timestampMs
        val mask = masksByTimestamp.remove(face.timestampMs)
        val updated = face.copy(headSegmentation = mask)
        facesByTimestamp[face.timestampMs] = updated
        trimPendingLocked()
        return ArFaceState.Detected(updated)
    }

    private fun trimPendingLocked() {
        while (facesByTimestamp.size > MAX_PENDING_RESULTS) {
            facesByTimestamp.remove(facesByTimestamp.keys.first())
        }
        while (masksByTimestamp.size > MAX_PENDING_RESULTS) {
            masksByTimestamp.remove(masksByTimestamp.keys.first())
        }
    }

    private fun clearLocked() {
        facesByTimestamp.clear()
        masksByTimestamp.clear()
        latestTimestampMs = Long.MIN_VALUE
    }

    private companion object {
        const val MAX_PENDING_RESULTS = 4
    }
}
