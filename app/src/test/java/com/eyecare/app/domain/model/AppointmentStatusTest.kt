package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentStatusTest {

    @Test
    fun `scheduled maps from backend string`() {
        assertEquals(AppointmentStatus.SCHEDULED, AppointmentStatus.from("scheduled"))
    }

    @Test
    fun `checked_in maps from backend string`() {
        assertEquals(AppointmentStatus.CHECKED_IN, AppointmentStatus.from("checked_in"))
    }

    @Test
    fun `fulfilled maps from backend string`() {
        assertEquals(AppointmentStatus.FULFILLED, AppointmentStatus.from("fulfilled"))
    }

    @Test
    fun `cancelled maps from backend string`() {
        assertEquals(AppointmentStatus.CANCELLED, AppointmentStatus.from("cancelled"))
    }

    @Test
    fun `no_show maps from backend string`() {
        assertEquals(AppointmentStatus.NO_SHOW, AppointmentStatus.from("no_show"))
    }

    @Test
    fun `unknown raw string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("something_else"))
    }

    @Test
    fun `retired pending string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("pending"))
    }

    @Test
    fun `retired confirmed string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("confirmed"))
    }

    @Test
    fun `retired arrived string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("arrived"))
    }

    @Test
    fun `retired completed string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("completed"))
    }

    @Test
    fun `empty string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from(""))
    }

    @Test
    fun `blank string maps to UNKNOWN`() {
        assertEquals(AppointmentStatus.UNKNOWN, AppointmentStatus.from("   "))
    }

    // Capability matrix tests

    @Test
    fun `scheduled can cancel`() {
        assertTrue(AppointmentStatus.SCHEDULED.canCancel)
    }

    @Test
    fun `scheduled can reschedule`() {
        assertTrue(AppointmentStatus.SCHEDULED.canReschedule)
    }

    @Test
    fun `scheduled cannot leave feedback`() {
        assertFalse(AppointmentStatus.SCHEDULED.canLeaveFeedback)
    }

    @Test
    fun `checked_in can cancel`() {
        assertTrue(AppointmentStatus.CHECKED_IN.canCancel)
    }

    @Test
    fun `checked_in cannot reschedule`() {
        assertFalse(AppointmentStatus.CHECKED_IN.canReschedule)
    }

    @Test
    fun `checked_in cannot leave feedback`() {
        assertFalse(AppointmentStatus.CHECKED_IN.canLeaveFeedback)
    }

    @Test
    fun `fulfilled cannot cancel`() {
        assertFalse(AppointmentStatus.FULFILLED.canCancel)
    }

    @Test
    fun `fulfilled cannot reschedule`() {
        assertFalse(AppointmentStatus.FULFILLED.canReschedule)
    }

    @Test
    fun `fulfilled can leave feedback`() {
        assertTrue(AppointmentStatus.FULFILLED.canLeaveFeedback)
    }

    @Test
    fun `cancelled cannot cancel`() {
        assertFalse(AppointmentStatus.CANCELLED.canCancel)
    }

    @Test
    fun `cancelled cannot reschedule`() {
        assertFalse(AppointmentStatus.CANCELLED.canReschedule)
    }

    @Test
    fun `cancelled cannot leave feedback`() {
        assertFalse(AppointmentStatus.CANCELLED.canLeaveFeedback)
    }

    @Test
    fun `no_show cannot cancel`() {
        assertFalse(AppointmentStatus.NO_SHOW.canCancel)
    }

    @Test
    fun `no_show cannot reschedule`() {
        assertFalse(AppointmentStatus.NO_SHOW.canReschedule)
    }

    @Test
    fun `no_show cannot leave feedback`() {
        assertFalse(AppointmentStatus.NO_SHOW.canLeaveFeedback)
    }

    @Test
    fun `unknown cannot cancel`() {
        assertFalse(AppointmentStatus.UNKNOWN.canCancel)
    }

    @Test
    fun `unknown cannot reschedule`() {
        assertFalse(AppointmentStatus.UNKNOWN.canReschedule)
    }

    @Test
    fun `unknown cannot leave feedback`() {
        assertFalse(AppointmentStatus.UNKNOWN.canLeaveFeedback)
    }

    @Test
    fun `active statuses are scheduled and checked_in`() {
        assertTrue(AppointmentStatus.SCHEDULED.isActive)
        assertTrue(AppointmentStatus.CHECKED_IN.isActive)
        assertFalse(AppointmentStatus.FULFILLED.isActive)
        assertFalse(AppointmentStatus.CANCELLED.isActive)
        assertFalse(AppointmentStatus.NO_SHOW.isActive)
        assertFalse(AppointmentStatus.UNKNOWN.isActive)
    }

    @Test
    fun `terminal statuses are fulfilled cancelled no_show and unknown`() {
        assertFalse(AppointmentStatus.SCHEDULED.isTerminal)
        assertFalse(AppointmentStatus.CHECKED_IN.isTerminal)
        assertTrue(AppointmentStatus.FULFILLED.isTerminal)
        assertTrue(AppointmentStatus.CANCELLED.isTerminal)
        assertTrue(AppointmentStatus.NO_SHOW.isTerminal)
        assertTrue(AppointmentStatus.UNKNOWN.isTerminal)
    }

    @Test
    fun `patient label for scheduled`() {
        assertEquals("Scheduled", AppointmentStatus.SCHEDULED.patientLabel)
    }

    @Test
    fun `patient label for checked_in`() {
        assertEquals("Checked in", AppointmentStatus.CHECKED_IN.patientLabel)
    }

    @Test
    fun `patient label for fulfilled is Completed`() {
        assertEquals("Completed", AppointmentStatus.FULFILLED.patientLabel)
    }

    @Test
    fun `patient label for cancelled`() {
        assertEquals("Cancelled", AppointmentStatus.CANCELLED.patientLabel)
    }

    @Test
    fun `patient label for no_show`() {
        assertEquals("No show", AppointmentStatus.NO_SHOW.patientLabel)
    }

    @Test
    fun `patient label for unknown`() {
        assertEquals("Unknown", AppointmentStatus.UNKNOWN.patientLabel)
    }

    @Test
    fun `enum contains exactly six values`() {
        assertEquals(6, AppointmentStatus.entries.size)
    }
}
