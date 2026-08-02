package com.eyecare.app.presentation.reservations

import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

class FrameReservationEligibilityTest {

    private fun createAppointment(
        status: AppointmentStatus = AppointmentStatus.SCHEDULED,
        scheduledAt: String = "2026-08-01T10:00:00+08:00",
        durationMinutes: Int = 30,
    ) = AppointmentV1(
        id = 1,
        appointmentNumber = "APT-001",
        appointmentType = "New Patient",
        durationMinutes = durationMinutes,
        referringSource = null,
        status = status,
        scheduledAt = scheduledAt,
        contactNotes = null,
        reasonForVisit = null,
        lastRescheduleReason = null,
        source = "mobile",
        assignedOptometrist = null,
    )

    private fun instantOf(year: Int, month: Int, day: Int, hour: Int, minute: Int): Instant {
        return OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(8)).toInstant()
    }

    @Test
    fun `scheduled future appointment is eligible`() {
        val appointment = createAppointment(
            scheduledAt = "2026-08-01T10:00:00+08:00",
            durationMinutes = 30,
        )
        val now = instantOf(2026, 8, 1, 9, 0)
        assertTrue(isReservationEligible(appointment, now))
    }

    @Test
    fun `scheduled appointment whose end equals now is eligible`() {
        val appointment = createAppointment(
            scheduledAt = "2026-08-01T10:00:00+08:00",
            durationMinutes = 30,
        )
        val now = instantOf(2026, 8, 1, 10, 30)
        assertTrue(isReservationEligible(appointment, now))
    }

    @Test
    fun `scheduled appointment whose end is before now is ineligible`() {
        val appointment = createAppointment(
            scheduledAt = "2026-08-01T10:00:00+08:00",
            durationMinutes = 30,
        )
        val now = instantOf(2026, 8, 1, 10, 31)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `checked-in appointment is ineligible`() {
        val appointment = createAppointment(status = AppointmentStatus.CHECKED_IN)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `fulfilled appointment is ineligible`() {
        val appointment = createAppointment(status = AppointmentStatus.FULFILLED)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `cancelled appointment is ineligible`() {
        val appointment = createAppointment(status = AppointmentStatus.CANCELLED)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `no_show appointment is ineligible`() {
        val appointment = createAppointment(status = AppointmentStatus.NO_SHOW)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `unknown status appointment is ineligible`() {
        val appointment = createAppointment(status = AppointmentStatus.UNKNOWN)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `malformed schedule fails closed`() {
        val appointment = createAppointment(scheduledAt = "not-a-date")
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `negative duration fails closed`() {
        val appointment = createAppointment(durationMinutes = -1)
        val now = instantOf(2026, 8, 1, 9, 0)
        assertFalse(isReservationEligible(appointment, now))
    }

    @Test
    fun `zero duration appointment end equals start`() {
        val appointment = createAppointment(
            scheduledAt = "2026-08-01T10:00:00+08:00",
            durationMinutes = 0,
        )
        val now = instantOf(2026, 8, 1, 10, 0)
        assertTrue(isReservationEligible(appointment, now))
    }
}
