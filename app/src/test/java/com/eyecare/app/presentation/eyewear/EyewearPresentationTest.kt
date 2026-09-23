package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.OpticalOrderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EyewearPresentationTest {

    @Test
    fun `formatPeso formats correctly`() {
        assertEquals("\u20B11,400.00", formatPeso(BigDecimal("1400.00")))
        assertEquals("\u20B10.00", formatPeso(BigDecimal("0.00")))
    }

    @Test
    fun `pending_payment label is correct`() {
        assertEquals("Awaiting payment", orderStatusLabel(OpticalOrderStatus.PENDING_PAYMENT))
    }

    @Test
    fun `payment_review label is correct`() {
        assertEquals("Payment under review", orderStatusLabel(OpticalOrderStatus.PAYMENT_REVIEW))
    }

    @Test
    fun `all statuses have labels`() {
        OpticalOrderStatus.entries.forEach { status ->
            val label = orderStatusLabel(status)
            assertTrue(label.isNotBlank(), "Status $status should have a non-blank label")
        }
    }

    @Test
    fun `pending_payment waits before fulfillment starts`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.PENDING_PAYMENT)
        assertNull(tracker.activeStep)
        assertTrue(tracker.steps.none { it.second })
    }

    @Test
    fun `payment_review waits before fulfillment starts`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.PAYMENT_REVIEW)
        assertNull(tracker.activeStep)
        assertTrue(tracker.steps.none { it.second })
    }

    @Test
    fun `tracker retains useful state descriptions for screen readers`() {
        assertEquals(
            "Next after payment verification",
            trackerStepAccessibilityStateLabel(
                status = OpticalOrderStatus.PENDING_PAYMENT,
                step = TrackerStep.CONFIRMED,
                completed = false,
                activeStep = null,
            ),
        )
        assertEquals(
            "Later",
            trackerStepAccessibilityStateLabel(
                status = OpticalOrderStatus.PAYMENT_REVIEW,
                step = TrackerStep.PROCESSING,
                completed = false,
                activeStep = null,
            ),
        )
        assertEquals(
            "In progress",
            trackerStepAccessibilityStateLabel(
                status = OpticalOrderStatus.QUEUED,
                step = TrackerStep.CONFIRMED,
                completed = true,
                activeStep = TrackerStep.CONFIRMED,
            ),
        )
        assertEquals(
            "Next",
            trackerStepAccessibilityStateLabel(
                status = OpticalOrderStatus.QUEUED,
                step = TrackerStep.PROCESSING,
                completed = false,
                activeStep = TrackerStep.CONFIRMED,
            ),
        )
    }

    @Test
    fun `legacy order tracker is unchanged`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.QUEUED)
        assertEquals(TrackerStep.CONFIRMED, tracker.activeStep)
    }

    @Test
    fun `in_progress tracker is unchanged`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.IN_PROGRESS)
        assertEquals(TrackerStep.PROCESSING, tracker.activeStep)
    }

    @Test
    fun `dispensed tracker shows completed`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.DISPENSED)
        assertEquals(TrackerStep.COMPLETED, tracker.activeStep)
    }

    @Test
    fun `cancelled tracker shows cancelled`() {
        val tracker = computeOrderTracker(OpticalOrderStatus.CANCELLED)
        assertNull(tracker.activeStep)
    }

    @Test
    fun `all statuses produce valid tracker`() {
        OpticalOrderStatus.entries.forEach { status ->
            val tracker = computeOrderTracker(status)
            assertEquals(4, tracker.steps.size, "Status $status should have 4 steps")
        }
    }
}
