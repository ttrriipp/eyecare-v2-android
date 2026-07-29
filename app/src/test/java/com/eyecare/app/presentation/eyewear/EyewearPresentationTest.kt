package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.EyewearPaymentStatus
import com.eyecare.app.domain.model.EyewearProgress
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class EyewearPresentationTest {

    // Progress labels
    @Test
    fun `progress labels match spec`() {
        assertEquals("Estimate Available", EyewearProgress.ESTIMATE_AVAILABLE.patientLabel)
        assertEquals("In Preparation", EyewearProgress.IN_PREPARATION.patientLabel)
        assertEquals("Ready for Pickup", EyewearProgress.READY_FOR_PICKUP.patientLabel)
        assertEquals("Dispensed", EyewearProgress.DISPENSED.patientLabel)
        assertEquals("Estimate Declined", EyewearProgress.ESTIMATE_DECLINED.patientLabel)
        assertEquals("Estimate Expired", EyewearProgress.ESTIMATE_EXPIRED.patientLabel)
        assertEquals("Cancelled", EyewearProgress.CANCELLED.patientLabel)
        assertEquals("Status Unavailable", EyewearProgress.UNKNOWN.patientLabel)
    }

    // Payment labels
    @Test
    fun `payment labels match spec`() {
        assertEquals("Balance Due", EyewearPaymentStatus.BALANCE_DUE.patientLabel)
        assertEquals("Paid", EyewearPaymentStatus.PAID.patientLabel)
        assertEquals("Payment Status Unavailable", EyewearPaymentStatus.UNKNOWN.patientLabel)
    }

    @Test
    fun `null payment returns null label`() {
        assertNull(paymentLabel(null))
    }

    // Date labeling
    @Test
    fun `consultation date used when present`() {
        val (label, _) = formatDateLabel("2026-07-27T09:00:00+08:00", "2026-07-27T10:00:00+08:00")
        assertEquals("Consultation", label)
    }

    @Test
    fun `created date used when consultation is null`() {
        val (label, _) = formatDateLabel(null, "2026-07-27T10:00:00+08:00")
        assertEquals("Created", label)
    }

    // Balance visibility
    @Test
    fun `balance shown when status is balance_due and value exists`() {
        assertTrue(shouldShowBalance(EyewearPaymentStatus.BALANCE_DUE, BigDecimal("3000")))
    }

    @Test
    fun `balance hidden when paid`() {
        assertFalse(shouldShowBalance(EyewearPaymentStatus.PAID, BigDecimal("0")))
    }

    @Test
    fun `balance hidden when null`() {
        assertFalse(shouldShowBalance(EyewearPaymentStatus.BALANCE_DUE, null))
    }

    @Test
    fun `balance hidden when zero`() {
        assertFalse(shouldShowBalance(EyewearPaymentStatus.BALANCE_DUE, BigDecimal.ZERO))
    }

    // Rating eligibility
    @Test
    fun `rating eligible for dispensed with both IDs`() {
        assertTrue(isRatingEligible(EyewearProgress.DISPENSED, 1, 42))
    }

    @Test
    fun `rating ineligible for non-dispensed`() {
        assertFalse(isRatingEligible(EyewearProgress.IN_PREPARATION, 1, 42))
    }

    @Test
    fun `rating ineligible without item ID`() {
        assertFalse(isRatingEligible(EyewearProgress.DISPENSED, null, 42))
    }

    @Test
    fun `rating ineligible without variant ID`() {
        assertFalse(isRatingEligible(EyewearProgress.DISPENSED, 1, null))
    }

    // Tracker states
    @Test
    fun `estimate available shows estimate active`() {
        val tracker = computeTracker(EyewearProgress.ESTIMATE_AVAILABLE)
        assertEquals(TrackerStep.ESTIMATE, tracker.activeStep)
        assertNull(tracker.terminalMessage)
        assertTrue(tracker.steps[0].second) // estimate complete
        assertFalse(tracker.steps[1].second) // preparation not
    }

    @Test
    fun `dispensed shows all complete and released message`() {
        val tracker = computeTracker(EyewearProgress.DISPENSED)
        assertNull(tracker.activeStep)
        assertEquals("Released to You", tracker.terminalMessage)
        assertTrue(tracker.steps.all { it.second })
    }

    @Test
    fun `cancelled shows terminal message`() {
        val tracker = computeTracker(EyewearProgress.CANCELLED)
        assertEquals("Cancelled", tracker.terminalMessage)
        assertFalse(tracker.steps.any { it.second })
    }

    @Test
    fun `estimate declined shows reached estimate`() {
        val tracker = computeTracker(EyewearProgress.ESTIMATE_DECLINED)
        assertEquals("Estimate Declined", tracker.terminalMessage)
        assertTrue(tracker.steps[0].second) // estimate was reached
        assertFalse(tracker.steps[1].second)
    }

    // Payment method humanization
    @Test
    fun `payment methods humanized correctly`() {
        assertEquals("Cash", humanizePaymentMethod("cash"))
        assertEquals("GCash", humanizePaymentMethod("gcash"))
        assertEquals("Bank Transfer", humanizePaymentMethod("bank_transfer"))
        assertEquals("Credit Card", humanizePaymentMethod("credit_card"))
        assertEquals("Credit Card", humanizePaymentMethod("card"))
    }

    @Test
    fun `unknown payment method humanized safely`() {
        assertEquals("Some new method", humanizePaymentMethod("some_new_method"))
    }
}
