package com.eyecare.app.presentation.ar.tracking

import kotlin.math.cos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceDistanceScaleTrackerTest {

    @Test
    fun first_trusted_sample_preserves_the_mapped_pose_scale() {
        val tracker = FaceDistanceScaleTracker()

        val scale = tracker.update(
            faceWidthNorm = 0.4f,
            mappedPoseScale = 1.2f,
            yawDeg = 0f,
        )

        assertEquals(1.2f, scale)
    }

    @Test
    fun a_wider_face_increases_scale_from_the_initial_baseline() {
        val tracker = FaceDistanceScaleTracker()
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)

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
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)

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
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)

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
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)

        val sideViewScale = tracker.update(
            faceWidthNorm = 0.4f * cos(Math.toRadians(15.0)).toFloat(),
            mappedPoseScale = 1f,
            yawDeg = 15f,
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
        assertEquals(
            1f,
            tracker.update(
                faceWidthNorm = 0.4f,
                mappedPoseScale = 1f,
                yawDeg = 0f,
            ),
        )
    }

    @Test
    fun untrusted_pose_holds_the_last_trusted_scale() {
        val tracker = FaceDistanceScaleTracker()
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)
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
        assertEquals(
            1f,
            tracker.update(
                faceWidthNorm = 0.4f,
                mappedPoseScale = 1f,
                yawDeg = 0f,
            ),
        )
    }

    @Test
    fun reset_starts_a_new_baseline() {
        val tracker = FaceDistanceScaleTracker()
        tracker.update(faceWidthNorm = 0.4f, mappedPoseScale = 1f, yawDeg = 0f)
        tracker.update(faceWidthNorm = 0.6f, mappedPoseScale = 1f, yawDeg = 0f)

        tracker.reset()

        val newBaselineScale = tracker.update(
            faceWidthNorm = 0.6f,
            mappedPoseScale = 1.25f,
            yawDeg = 0f,
        )

        assertEquals(1.25f, newBaselineScale)
        assertTrue(newBaselineScale!! > 0f)
    }
}
