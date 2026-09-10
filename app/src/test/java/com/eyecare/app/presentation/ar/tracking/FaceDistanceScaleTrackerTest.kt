package com.eyecare.app.presentation.ar.tracking

import kotlin.math.cos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceDistanceScaleTrackerTest {

    @Test
    fun startup_waits_for_consecutive_trusted_samples_before_exposing_scale() {
        val tracker = FaceDistanceScaleTracker(minimumTrustedSamples = 3)

        assertNull(tracker.update(
            faceWidthNorm = 0.4f,
            mappedPoseScale = 1.2f,
            yawDeg = 0f,
        ))
        assertNull(tracker.update(
            faceWidthNorm = 0.4f,
            mappedPoseScale = 1.2f,
            yawDeg = 0f,
        ))

        assertEquals(1.2f, tracker.update(
            faceWidthNorm = 0.4f,
            mappedPoseScale = 1.2f,
            yawDeg = 0f,
        ))
    }

    @Test
    fun startup_uses_the_median_to_ignore_one_noisy_trusted_sample() {
        val tracker = FaceDistanceScaleTracker(minimumTrustedSamples = 3)

        assertNull(tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f))
        assertNull(tracker.update(faceWidthNorm = 0.8f, mappedPoseScale = 1.8f, yawDeg = 0f))

        assertEquals(
            1f,
            tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)!!,
            0.001f,
        )

        assertEquals(
            1.025f,
            tracker.update(faceWidthNorm = 0.41f, mappedPoseScale = 1f, yawDeg = 0f)!!,
            0.001f,
        )
    }

    @Test
    fun untrusted_sample_breaks_startup_consecutiveness() {
        val tracker = FaceDistanceScaleTracker(minimumTrustedSamples = 3)

        assertNull(tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f))
        assertNull(tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f, faceCenterX = 0.8f))
        assertNull(tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f))
        assertNull(tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f))
        assertEquals(1f, tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f))
    }

    @Test
    fun a_wider_face_increases_scale_from_the_initial_baseline() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)

        val closerScale = tracker.update(
            faceWidthNorm = 0.6f,
            mappedPoseScale = 1f,
            yawDeg = 0f,
        )

        assertEquals(1.3f, closerScale)
    }

    @Test
    fun a_narrower_face_decreases_scale_from_the_initial_baseline() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)

        val fartherScale = tracker.update(
            faceWidthNorm = 0.2f,
            mappedPoseScale = 1f,
            yawDeg = 0f,
        )

        assertEquals(0.8f, fartherScale)
    }

    @Test
    fun live_matrix_scale_changes_do_not_replace_the_calibrated_baseline() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)

        val closerScale = tracker.update(
            faceWidthNorm = 0.6f,
            mappedPoseScale = 1.8f,
            yawDeg = 0f,
        )

        assertEquals(1.3f, closerScale)
    }

    @Test
    fun projected_width_is_compensated_for_head_yaw() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)

        val sideViewScale = tracker.update(
            faceWidthNorm = 0.4f * cos(Math.toRadians(10.0)).toFloat(),
            mappedPoseScale = 1f,
            yawDeg = 10f,
        )

        assertEquals(1f, sideViewScale!!, 0.001f)
    }

    @Test
    fun untrusted_first_pose_does_not_establish_a_scale_baseline() {
        val tracker = FaceDistanceScaleTracker()

        val scale = tracker.update(
            faceWidthNorm = 0.4f,
            mappedPoseScale = 1f,
            yawDeg = 0f,
            faceCenterX = 0.8f,
        )

        assertNull(scale)
        assertEquals(1f, establishBaseline(tracker)!!)
    }

    @Test
    fun untrusted_pose_holds_the_last_trusted_scale() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)
        tracker.update(faceWidthNorm = 0.6f, mappedPoseScale = 1f, yawDeg = 0f)

        val heldScale = tracker.update(
            faceWidthNorm = 0.2f,
            mappedPoseScale = 1f,
            yawDeg = 35f,
        )

        assertEquals(1.3f, heldScale)
    }

    @Test
    fun invalid_samples_are_ignored_without_creating_a_baseline() {
        val tracker = FaceDistanceScaleTracker()

        assertNull(
            tracker.update(
                faceWidthNorm = 0f,
                mappedPoseScale = 1f,
                yawDeg = 0f,
            ),
        )
        assertEquals(1f, establishBaseline(tracker)!!)
    }

    @Test
    fun reset_starts_a_new_baseline() {
        val tracker = FaceDistanceScaleTracker()
        establishBaseline(tracker)
        tracker.update(faceWidthNorm = 0.6f, mappedPoseScale = 1f, yawDeg = 0f)

        tracker.reset()

        var newBaselineScale: Float? = null
        repeat(8) {
            newBaselineScale = tracker.update(
                faceWidthNorm = 0.6f,
                mappedPoseScale = 1.25f,
                yawDeg = 0f,
            )
        }

        val baselineScale = newBaselineScale ?: error("Expected the new baseline scale")
        assertEquals(1.25f, baselineScale)
        assertTrue(baselineScale > 0f)
    }

    private fun establishBaseline(
        tracker: FaceDistanceScaleTracker,
        faceWidthNorm: Float = 0.4f,
        mappedPoseScale: Float = 1f,
    ): Float? {
        var scale: Float? = null
        repeat(8) {
            scale = tracker.update(
                faceWidthNorm = faceWidthNorm,
                mappedPoseScale = mappedPoseScale,
                yawDeg = 0f,
            )
        }
        return scale
    }
}
