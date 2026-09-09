package com.eyecare.app.presentation.ar

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SegmentationSubmissionSchedulerTest {

    @Test
    fun `first frame is submitted immediately`() {
        val scheduler = SegmentationSubmissionScheduler(minIntervalMs = 66L)

        assertTrue(scheduler.shouldSubmit(1_000L))
    }

    @Test
    fun `frames inside the cadence window are skipped`() {
        val scheduler = SegmentationSubmissionScheduler(minIntervalMs = 66L)

        assertTrue(scheduler.shouldSubmit(1_000L))
        assertFalse(scheduler.shouldSubmit(1_065L))
    }

    @Test
    fun `a frame at the cadence boundary is submitted`() {
        val scheduler = SegmentationSubmissionScheduler(minIntervalMs = 66L)

        assertTrue(scheduler.shouldSubmit(1_000L))
        assertTrue(scheduler.shouldSubmit(1_066L))
    }

    @Test
    fun `out of order timestamps do not rewind the cadence`() {
        val scheduler = SegmentationSubmissionScheduler(minIntervalMs = 66L)

        assertTrue(scheduler.shouldSubmit(1_000L))
        assertFalse(scheduler.shouldSubmit(900L))
        assertFalse(scheduler.shouldSubmit(1_050L))
        assertTrue(scheduler.shouldSubmit(1_066L))
    }

    @Test
    fun `reset allows the next frame immediately`() {
        val scheduler = SegmentationSubmissionScheduler(minIntervalMs = 66L)

        assertTrue(scheduler.shouldSubmit(1_000L))
        scheduler.reset()

        assertTrue(scheduler.shouldSubmit(1_001L))
    }
}
