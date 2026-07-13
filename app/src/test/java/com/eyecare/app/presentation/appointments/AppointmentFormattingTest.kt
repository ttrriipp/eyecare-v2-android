package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
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
    fun `appointmentWeekDays returns monday through saturday for selected week`() {
        val days = appointmentWeekDays(LocalDate.of(2026, 10, 24))

        assertEquals(LocalDate.of(2026, 10, 19), days.first())
        assertEquals(LocalDate.of(2026, 10, 24), days.last())
        assertEquals(6, days.size)
    }

    @Test
    fun `appointmentOccursOnDate matches appointment scheduled day`() {
        val scheduledAt = "2026-10-24T17:00:00Z"

        assertTrue(appointmentOccursOnDate(scheduledAt, LocalDate.of(2026, 10, 25)))
        assertFalse(appointmentOccursOnDate(scheduledAt, LocalDate.of(2026, 10, 24)))
    }

    @Test
    fun `upcoming tab contains future active appointments ordered soonest first`() {
        val appointments = listOf(
            appointment(2, AppointmentStatus.CONFIRMED, "2026-10-26T10:00:00Z"),
            appointment(1, AppointmentStatus.PENDING, "2026-10-25T10:00:00Z"),
            appointment(3, AppointmentStatus.COMPLETED, "2026-10-27T10:00:00Z"),
        )

        val result = appointmentsForTab(
            appointments = appointments,
            tab = AppointmentListTab.UPCOMING,
            now = LocalDateTime.of(2026, 10, 24, 10, 0),
        )

        assertEquals(listOf(1, 2), result.map { it.id })
    }

    @Test
    fun `history tab contains terminal and past appointments newest first`() {
        val appointments = listOf(
            appointment(1, AppointmentStatus.CONFIRMED, "2026-10-23T10:00:00Z"),
            appointment(2, AppointmentStatus.CANCELLED, "2026-10-26T10:00:00Z"),
            appointment(3, AppointmentStatus.NO_SHOW, "2026-10-22T10:00:00Z"),
        )

        val result = appointmentsForTab(
            appointments = appointments,
            tab = AppointmentListTab.HISTORY,
            now = LocalDateTime.of(2026, 10, 24, 10, 0),
        )

        assertEquals(listOf(2, 1, 3), result.map { it.id })
    }

    private fun appointment(id: Int, status: AppointmentStatus, scheduledAt: String) = Appointment(
        id = id,
        visitReason = "Eye Exam",
        status = status,
        scheduledAt = scheduledAt,
        contactNotes = null,
        staffNotes = null,
    )
}
