package com.eyecare.app.presentation.ar.tracking

import com.eyecare.app.presentation.ar.model.FacePose
import kotlin.math.abs
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PoseStabilizerTest {

    @Test
    fun reducesAlternatingTranslationJitter() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(translationX = 0f), timestampMs = 0L)

        val outputs = listOf(1f, -1f, 1f, -1f, 1f, -1f).mapIndexed { index, value ->
            stabilizer.update(pose(translationX = value), timestampMs = (index + 1) * 33L)!!
                .translationX
        }

        assertTrue(outputs.all { abs(it) < 1f })
        assertTrue(outputs.map(::abs).average() < 0.45)
    }

    @Test
    fun convergesTowardDeliberateStepChange() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(translationX = 0f), timestampMs = 0L)

        var output: FacePose? = null
        repeat(10) { index ->
            output = stabilizer.update(pose(translationX = 1f), timestampMs = (index + 1) * 33L)
        }

        assertNotNull(output)
        assertTrue(output!!.translationX in 0.85f..1f)
    }

    @Test
    fun noFaceImmediatelyClearsHistoryAndDoesNotHoldLastPose() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(translationX = 0f), timestampMs = 0L)
        stabilizer.update(pose(translationX = 1f), timestampMs = 33L)

        assertNull(stabilizer.update(null, timestampMs = 66L))
        val resumed = stabilizer.update(pose(translationX = 1f), timestampMs = 99L)

        assertNotNull(resumed)
        assertEquals(1f, resumed!!.translationX, EPSILON)
    }

    @Test
    fun largeTimestampGapSeedsCurrentPoseWithoutOldHistory() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(translationX = 0f), timestampMs = 0L)
        stabilizer.update(pose(translationX = 1f), timestampMs = 33L)

        val afterGap = stabilizer.update(pose(translationX = 1f), timestampMs = 1_000L)

        assertNotNull(afterGap)
        assertEquals(1f, afterGap!!.translationX, EPSILON)
    }

    @Test
    fun invalidTimestampClearsHistoryAndReturnsNoPose() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(translationX = 0f), timestampMs = 0L)
        stabilizer.update(pose(translationX = 1f), timestampMs = 33L)

        assertNull(stabilizer.update(pose(translationX = 1f), timestampMs = 32L))
        val resumed = stabilizer.update(pose(translationX = 1f), timestampMs = 65L)

        assertNotNull(resumed)
        assertEquals(1f, resumed!!.translationX, EPSILON)
    }

    @Test
    fun smoothsAngleAcrossTheWrapBoundaryUsingShortestPath() {
        val stabilizer = PoseStabilizer(responseTimeMs = 100f, maxTimestampGapMs = 250L)
        stabilizer.update(pose(rollDeg = 179f), timestampMs = 0L)

        val output = stabilizer.update(pose(rollDeg = -179f), timestampMs = 33L)

        assertNotNull(output)
        assertTrue(abs(output!!.rollDeg) > 179f)
    }

    private fun pose(
        translationX: Float = 0f,
        rollDeg: Float = 0f,
    ): FacePose = FacePose(
        translationX = translationX,
        translationY = 0f,
        translationZ = 0f,
        pitchDeg = 0f,
        yawDeg = 0f,
        rollDeg = rollDeg,
        scale = 1f,
    )

    private companion object {
        const val EPSILON = 0.0001f
    }
}
