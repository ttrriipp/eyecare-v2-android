package com.eyecare.app.presentation.appointments

import java.time.LocalDate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentFormattingTest {

    @Test
    fun `formatAppointmentTitle converts backend visit reason to readable title`() {
        assertEquals("Comprehensive Eye Exam", formatAppointmentTitle("comprehensive_eye_exam"))
    }

    @Test
    fun `formatAppointmentDate formats iso timestamp`() {
        assertEquals("Oct 24, 2026", formatAppointmentDate("2026-10-24T10:00:00Z"))
    }

    @Test
    fun `formatAppointmentTime formats iso timestamp`() {
        assertEquals("10:00 AM", formatAppointmentTime("2026-10-24T10:00:00Z"))
    }

    @Test
    fun `appointmentWeekDays returns monday through sunday for selected week`() {
        val days = appointmentWeekDays(LocalDate.of(2026, 10, 24))

        assertEquals(LocalDate.of(2026, 10, 19), days.first())
        assertEquals(LocalDate.of(2026, 10, 25), days.last())
        assertEquals(7, days.size)
    }

    @Test
    fun `appointmentOccursOnDate matches appointment scheduled day`() {
        val scheduledAt = "2026-10-24T10:00:00Z"

        assertTrue(appointmentOccursOnDate(scheduledAt, LocalDate.of(2026, 10, 24)))
        assertFalse(appointmentOccursOnDate(scheduledAt, LocalDate.of(2026, 10, 25)))
    }
}