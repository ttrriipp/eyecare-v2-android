package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.AppointmentRequestStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentRequestPresentationTest {

    @Test
    fun `pending shows cancel`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.PENDING)
        assertEquals("Awaiting clinic review", p.label)
        assertTrue(p.showCancel)
    }

    @Test
    fun `accepted is confirmed and not cancellable`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.ACCEPTED)
        assertEquals("Confirmed", p.label)
        assertFalse(p.showCancel)
    }

    @Test
    fun `rejected has no actions`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.REJECTED)
        assertEquals("Not approved", p.label)
        assertFalse(p.showCancel)
    }

    @Test
    fun `cancelled has no actions`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.CANCELLED)
        assertEquals("Cancelled", p.label)
        assertFalse(p.showCancel)
    }

    @Test
    fun `expired has no actions`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.EXPIRED)
        assertEquals("Expired", p.label)
        assertFalse(p.showCancel)
    }

    @Test
    fun `unknown fails closed`() {
        val p = requestStatusPresentation(AppointmentRequestStatus.UNKNOWN)
        assertEquals("Status unavailable", p.label)
        assertFalse(p.showCancel)
    }
}
