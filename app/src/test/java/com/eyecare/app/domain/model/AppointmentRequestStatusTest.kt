package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentRequestStatusTest {

    @Test
    fun `fromRaw maps known statuses`() {
        assertEquals(AppointmentRequestStatus.PENDING, AppointmentRequestStatus.fromRaw("pending"))
        assertEquals(AppointmentRequestStatus.ACCEPTED, AppointmentRequestStatus.fromRaw("accepted"))
        assertEquals(AppointmentRequestStatus.REJECTED, AppointmentRequestStatus.fromRaw("rejected"))
        assertEquals(AppointmentRequestStatus.CANCELLED, AppointmentRequestStatus.fromRaw("cancelled"))
        assertEquals(AppointmentRequestStatus.EXPIRED, AppointmentRequestStatus.fromRaw("expired"))
    }

    @Test
    fun `fromRaw maps unknown to UNKNOWN`() {
        assertEquals(AppointmentRequestStatus.UNKNOWN, AppointmentRequestStatus.fromRaw("bogus"))
        assertEquals(AppointmentRequestStatus.UNKNOWN, AppointmentRequestStatus.fromRaw(""))
    }

    @Test
    fun `terminal statuses are terminal`() {
        assertTrue(AppointmentRequestStatus.ACCEPTED.isTerminal)
        assertTrue(AppointmentRequestStatus.REJECTED.isTerminal)
        assertTrue(AppointmentRequestStatus.CANCELLED.isTerminal)
        assertTrue(AppointmentRequestStatus.EXPIRED.isTerminal)
        assertFalse(AppointmentRequestStatus.PENDING.isTerminal)
        assertFalse(AppointmentRequestStatus.UNKNOWN.isTerminal)
    }

    @Test
    fun `only pending is cancellable`() {
        assertTrue(AppointmentRequestStatus.PENDING.isCancellable)
        assertFalse(AppointmentRequestStatus.ACCEPTED.isCancellable)
        assertFalse(AppointmentRequestStatus.REJECTED.isCancellable)
        assertFalse(AppointmentRequestStatus.CANCELLED.isCancellable)
        assertFalse(AppointmentRequestStatus.EXPIRED.isCancellable)
        assertFalse(AppointmentRequestStatus.UNKNOWN.isCancellable)
    }
}
