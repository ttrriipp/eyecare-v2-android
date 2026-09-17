package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AppointmentCurrentActionPolicyTest {

    @Test
    fun `future scheduled appointment exposes cancel and time change`() {
        val policy = appointmentCurrentActionPolicy(
            appointment = appointment(
                status = AppointmentStatus.SCHEDULED,
                scheduledAt = "2030-09-25T09:00:00+08:00",
            ),
            hasPendingReschedule = false,
        )

        assertTrue(policy.canCancel)
        assertTrue(policy.canRequestDifferentTime)
        assertFalse(policy.sameDayCancellationBlocked)
    }

    @Test
    fun `pending time change blocks another request`() {
        val policy = appointmentCurrentActionPolicy(
            appointment = appointment(
                status = AppointmentStatus.SCHEDULED,
                scheduledAt = "2030-09-25T09:00:00+08:00",
            ),
            hasPendingReschedule = true,
        )

        assertFalse(policy.canRequestDifferentTime)
        assertTrue(policy.canCancel)
    }

    @Test
    fun `checked in appointment can be cancelled but not time changed`() {
        val policy = appointmentCurrentActionPolicy(
            appointment = appointment(
                status = AppointmentStatus.CHECKED_IN,
                scheduledAt = "2030-09-25T09:00:00+08:00",
            ),
            hasPendingReschedule = false,
        )

        assertTrue(policy.canCancel)
        assertFalse(policy.canRequestDifferentTime)
    }

    @Test
    fun `same day appointment keeps cancellation guidance but hides cancel action`() {
        val policy = appointmentCurrentActionPolicy(
            appointment = appointment(
                status = AppointmentStatus.SCHEDULED,
                scheduledAt = "${LocalDate.now(CLINIC_TIME_ZONE)}T09:00:00+08:00",
            ),
            hasPendingReschedule = false,
        )

        assertFalse(policy.canCancel)
        assertTrue(policy.sameDayCancellationBlocked)
        assertTrue(policy.canRequestDifferentTime)
    }

    @Test
    fun `terminal appointment exposes no current actions`() {
        val policy = appointmentCurrentActionPolicy(
            appointment = appointment(
                status = AppointmentStatus.FULFILLED,
                scheduledAt = "2030-09-25T09:00:00+08:00",
            ),
            hasPendingReschedule = false,
        )

        assertFalse(policy.canCancel)
        assertFalse(policy.canRequestDifferentTime)
        assertFalse(policy.sameDayCancellationBlocked)
    }

    private fun appointment(status: AppointmentStatus, scheduledAt: String) = AppointmentV1(
        id = 42,
        appointmentNumber = "APT-42",
        appointmentType = "Eye examination",
        durationMinutes = 30,
        referringSource = null,
        status = status,
        scheduledAt = scheduledAt,
        contactNotes = null,
        reasonForVisit = null,
        lastRescheduleReason = null,
        source = "mobile",
        assignedOptometrist = null,
    )
}
