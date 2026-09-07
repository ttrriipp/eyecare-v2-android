package com.eyecare.app.presentation.ar

import com.eyecare.app.presentation.ar.model.ArFaceState
import com.eyecare.app.presentation.ar.model.FaceFrame
import com.eyecare.app.presentation.ar.model.FaceTransformationMatrix
import com.eyecare.app.presentation.ar.model.HeadSegmentationFrame
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ArFrameMaskPairerTest {

    @Test
    fun `face is published immediately and matching mask is attached later`() {
        val received = mutableListOf<ArFaceState>()
        val pairer = ArFrameMaskPairer(received::add)
        val face = face(timestampMs = 100L)
        val mask = mask(timestampMs = 100L)

        pairer.onFaceResult(ArFaceState.Detected(face))
        pairer.onMaskResult(mask)

        assertEquals(2, received.size)
        assertNull((received[0] as ArFaceState.Detected).frame.headSegmentation)
        assertEquals(mask, (received[1] as ArFaceState.Detected).frame.headSegmentation)
    }

    @Test
    fun `mask that arrives first is paired with its face timestamp`() {
        val received = mutableListOf<ArFaceState>()
        val pairer = ArFrameMaskPairer(received::add)
        val mask = mask(timestampMs = 200L)

        pairer.onMaskResult(mask)
        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 200L)))

        assertEquals(1, received.size)
        assertEquals(mask, (received.single() as ArFaceState.Detected).frame.headSegmentation)
    }

    @Test
    fun `mask for an older face cannot replace the current face`() {
        val received = mutableListOf<ArFaceState>()
        val pairer = ArFrameMaskPairer(received::add)

        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 300L)))
        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 333L)))
        pairer.onMaskResult(mask(timestampMs = 300L))

        assertEquals(2, received.size)
        assertNull((received.last() as ArFaceState.Detected).frame.headSegmentation)
    }

    @Test
    fun `older face result is ignored after a newer face has been published`() {
        val received = mutableListOf<ArFaceState>()
        val pairer = ArFrameMaskPairer(received::add)

        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 500L)))
        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 467L)))

        assertEquals(1, received.size)
        assertEquals(500L, (received.single() as ArFaceState.Detected).frame.timestampMs)
    }

    @Test
    fun `face loss clears pending masks and publishes no face`() {
        val received = mutableListOf<ArFaceState>()
        val pairer = ArFrameMaskPairer(received::add)

        pairer.onMaskResult(mask(timestampMs = 400L))
        pairer.onFaceResult(ArFaceState.NoFace)
        pairer.onFaceResult(ArFaceState.Detected(face(timestampMs = 400L)))

        assertEquals(2, received.size)
        assertEquals(ArFaceState.NoFace, received[0])
        assertNull((received[1] as ArFaceState.Detected).frame.headSegmentation)
    }

    private fun face(timestampMs: Long): FaceFrame = FaceFrame(
        noseBridgeX = 0.5f,
        noseBridgeY = 0.5f,
        leftTempleX = 0.25f,
        rightTempleX = 0.75f,
        faceWidthNorm = 0.5f,
        rotationDeg = 0f,
        imageWidth = 640,
        imageHeight = 480,
        transformationMatrix = FaceTransformationMatrix.from(FloatArray(16))!!,
        timestampMs = timestampMs,
    )

    private fun mask(timestampMs: Long): HeadSegmentationFrame =
        HeadSegmentationFrame.from(
            imageWidth = 2,
            imageHeight = 2,
            timestampMs = timestampMs,
            confidence = FloatArray(4) { 0.8f },
        )!!
}
