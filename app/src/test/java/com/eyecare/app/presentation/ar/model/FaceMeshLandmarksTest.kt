package com.eyecare.app.presentation.ar.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FaceMeshLandmarksTest {

    @Test
    fun `accepts exactly 478 finite landmarks and exposes coordinates`() {
        val values = FloatArray(FaceMeshLandmarks.VALUE_COUNT) { it.toFloat() }

        val mesh = FaceMeshLandmarks.from(values)

        assertEquals(0f, mesh?.x(0))
        assertEquals(1f, mesh?.y(0))
        assertEquals(2f, mesh?.z(0))
        assertEquals(1431f, mesh?.x(FaceMeshLandmarks.LANDMARK_COUNT - 1))
        assertEquals(1432f, mesh?.y(FaceMeshLandmarks.LANDMARK_COUNT - 1))
        assertEquals(1433f, mesh?.z(FaceMeshLandmarks.LANDMARK_COUNT - 1))
    }

    @Test
    fun `does not expose the caller buffer after construction`() {
        val values = FloatArray(FaceMeshLandmarks.VALUE_COUNT) { it.toFloat() }
        val mesh = FaceMeshLandmarks.from(values)!!

        values[0] = -1f

        assertEquals(0f, mesh.x(0))
    }

    @Test
    fun `rejects wrong-sized and non-finite landmark buffers`() {
        assertNull(FaceMeshLandmarks.from(FloatArray(FaceMeshLandmarks.VALUE_COUNT - 1)))

        val nonFinite = FloatArray(FaceMeshLandmarks.VALUE_COUNT)
        nonFinite[FaceMeshLandmarks.VALUE_COUNT / 2] = Float.NaN

        assertNull(FaceMeshLandmarks.from(nonFinite))
    }

    @Test
    fun `rejects landmark indices outside the mesh`() {
        val mesh = FaceMeshLandmarks.from(FloatArray(FaceMeshLandmarks.VALUE_COUNT))!!

        assertThrows(IndexOutOfBoundsException::class.java) {
            mesh.x(FaceMeshLandmarks.LANDMARK_COUNT)
        }
    }
}
