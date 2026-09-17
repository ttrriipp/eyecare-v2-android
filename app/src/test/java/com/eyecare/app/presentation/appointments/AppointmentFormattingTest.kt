package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentFormattingTest {

    @Test
    fun `formatClinicScheduledAt preserves Philippine clinic time`() {
        assertEquals(
            "2026-07-13T09:00:00+08:00",
            formatClinicScheduledAt("2026-07-13", "09:00"),
        )
    }

    @Test
    fun `nextClinicSlot rounds up to the next quarter hour`() {
        assertEquals(LocalTime.of(10, 30), nextClinicSlot(LocalTime.of(10, 19)))
        assertEquals(LocalTime.of(10, 30), nextClinicSlot(LocalTime.of(10, 30)))
    }

    @Test
    fun `reschedule validation rejects past and unchanged selections`() {
        val now = LocalDateTime.of(2026, 7, 14, 10, 19)
        val current = LocalDateTime.of(2026, 7, 15, 9, 0)

        assertEquals(
            RescheduleSelectionError.PAST,
            validateRescheduleSelection(LocalDateTime.of(2026, 7, 14, 9, 0), current, now),
        )
        assertEquals(RescheduleSelectionError.UNCHANGED, validateRescheduleSelection(current, current, now))
        assertEquals(null, validateRescheduleSelection(current.plusMinutes(15), current, now))
    }

    @Test
    fun `earliest booking time respects now and visit duration`() {
        val now = LocalDateTime.of(2026, 7, 13, 10, 19)

        assertEquals(LocalTime.of(10, 30), earliestBookingTime(LocalDate.of(2026, 7, 13), 30, now))
        assertEquals(LocalTime.of(9, 0), earliestBookingTime(LocalDate.of(2026, 7, 14), 60, now))
        assertEquals(null, earliestBookingTime(LocalDate.of(2026, 7, 13), 60, now.withHour(16).withMinute(30)))
    }

    @Test
    fun `booking selection must be in the future and fit clinic hours`() {
        val now = LocalDateTime.of(2026, 7, 13, 10, 19)

        assertFalse(isBookableAppointmentTime(LocalDateTime.of(2026, 7, 13, 10, 15), 30, now))
        assertTrue(isBookableAppointmentTime(LocalDateTime.of(2026, 7, 13, 10, 30), 30, now))
        assertFalse(isBookableAppointmentTime(LocalDateTime.of(2026, 7, 13, 16, 45), 30, now))
    }

    @Test
    fun `formatAppointmentTitle converts backend visit reason to readable title`() {
        assertEquals("Comprehensive Eye Exam", formatAppointmentTitle("comprehensive_eye_exam"))
    }

    @Test
    fun `formatAppointmentDate formats iso timestamp`() {
        assertEquals("Oct 25, 2026", formatAppointmentDate("2026-10-24T17:00:00Z"))
    }

    @Test
    fun `formatAppointmentTime formats iso timestamp`() {
        assertEquals("9:00 AM", formatAppointmentTime("2026-10-24T01:00:00Z"))
    }

    @Test
    fun `appointment formatting uses patient safe fallbacks for incomplete data`() {
        assertEquals("Appointment", formatAppointmentTitle("   "))
        assertEquals("Date TBD", formatAppointmentDate(""))
        assertEquals("Time TBD", formatAppointmentTime(""))
    }

    @Test
    fun `displayable reschedule reason trims content and hides blanks`() {
        assertEquals(
            "Doctor availability changed",
            displayableRescheduleReason("  Doctor availability changed  "),
        )
        assertEquals(null, displayableRescheduleReason("   "))
        assertEquals(null, displayableRescheduleReason(null))
    }

}
