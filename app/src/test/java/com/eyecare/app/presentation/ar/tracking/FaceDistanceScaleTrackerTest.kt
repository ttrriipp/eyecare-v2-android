package com.eyecare.app.presentation.ar.tracking

import kotlin.math.sqrt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FaceDistanceScaleTrackerTest {

    @Test
    fun first_valid_sample_preserves_the_mapped_pose_scale() {
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
            faceWidthNorm = 0.4f * sqrt(0.5f),
            mappedPoseScale = 1f,
            yawDeg = 45f,
        )

        assertEquals(1f, sideViewScale!!, 0.001f)
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
