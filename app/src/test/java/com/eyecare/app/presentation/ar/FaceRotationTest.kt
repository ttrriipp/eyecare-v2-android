package com.eyecare.app.presentation.ar

import kotlin.math.abs
import kotlin.math.atan2
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceRotationTest {

    @Test
    fun `temple-to-temple vector gives meaningful rotation for tilted head`() {
        // Simulate head tilted ~15° clockwise (right side lower)
        val leftTempleX = 0.2f; val leftTempleY = 0.42f
        val rightTempleX = 0.8f; val rightTempleY = 0.55f

        // Old approach: nose bridge 6→168 — nearly vertical, no useful roll
        val noseY1 = 0.44f; val noseY2 = 0.46f
        val noseX1 = 0.50f; val noseX2 = 0.50f
        val oldDeg = Math.toDegrees(atan2((noseY2 - noseY1).toDouble(), (noseX2 - noseX1).toDouble())).toFloat()

        // New approach: left temple → right temple
        val newDeg = Math.toDegrees(atan2((rightTempleY - leftTempleY).toDouble(), (rightTempleX - leftTempleX).toDouble())).toFloat()

        // Old: nearly 90° (vertical) or 0° — neither is useful head roll
        assertTrue(abs(oldDeg) > 80 || abs(oldDeg) < 1, "Old rotation degenerate: $oldDeg")
        // New: should be ~12° reflecting the actual tilt
        assertTrue(abs(newDeg) in 5f..25f, "Temple rotation should be ~12°, got: $newDeg")
    }
}
