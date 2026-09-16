package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentRequestTypeSummary
import com.eyecare.app.presentation.appointments.requests.hasReachedActiveAppointmentRequestLimit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class AppointmentRequestListPresentationTest {

    private val now = LocalDateTime.of(2026, 8, 3, 12, 0)

    @Test
    fun `pending request appears in upcoming requests`() {
        val request = request(
            id = 1,
            status = AppointmentRequestStatus.PENDING,
            scheduledAt = "2026-08-04T10:00:00+08:00",
        )

        assertEquals(
            listOf(request),
            appointmentRequestsForTab(listOf(request), AppointmentListTab.UPCOMING, now = now),
        )
    }

    @Test
    fun `terminal requests appear in history`() {
        val request = request(
            id = 2,
            status = AppointmentRequestStatus.CANCELLED,
            scheduledAt = "2026-08-04T10:00:00+08:00",
        )

        assertEquals(
            listOf(request),
            appointmentRequestsForTab(listOf(request), AppointmentListTab.HISTORY, now = now),
        )
    }

    @Test
    fun `accepted request is omitted when its confirmed appointment is already listed`() {
        val request = request(
            id = 3,
            status = AppointmentRequestStatus.ACCEPTED,
            scheduledAt = "2026-08-04T10:00:00+08:00",
            appointmentId = 44,
        )

        assertEquals(
            emptyList<AppointmentRequest>(),
            appointmentRequestsForTab(
                requests = listOf(request),
                tab = AppointmentListTab.UPCOMING,
                confirmedAppointmentIds = setOf(44),
                now = now,
            ),
        )
    }

    @Test
    fun `pending rebooking is shown from its scheduled appointment detail`() {
        val request = request(
            id = 5,
            status = AppointmentRequestStatus.PENDING,
            requestType = AppointmentRequestType.RESCHEDULE,
            scheduledAt = "2026-08-04T10:00:00+08:00",
            appointmentId = 44,
        )

        assertEquals(
            emptyList<AppointmentRequest>(),
            appointmentRequestsForTab(
                requests = listOf(request),
                tab = AppointmentListTab.UPCOMING,
                confirmedAppointmentIds = setOf(44),
                now = now,
            ),
        )
    }

    @Test
    fun `active request limit is reached after one pending request`() {
        val requests = listOf(
            request(1, AppointmentRequestStatus.PENDING, "2026-08-04T10:00:00+08:00"),
        )

        assertTrue(hasReachedActiveAppointmentRequestLimit(requests))
    }

    @Test
    fun `terminal requests do not count toward active request limit`() {
        val requests = listOf(
            request(2, AppointmentRequestStatus.CANCELLED, "2026-08-05T10:00:00+08:00"),
            request(3, AppointmentRequestStatus.REJECTED, "2026-08-06T10:00:00+08:00"),
        )

        assertFalse(hasReachedActiveAppointmentRequestLimit(requests))
    }

    @Test
    fun `request card prioritizes visit type and keeps request metadata concise`() {
        val request = request(
            id = 4,
            status = AppointmentRequestStatus.PENDING,
            scheduledAt = "2026-08-07T10:00:00+08:00",
            appointmentType = AppointmentRequestTypeSummary(
                id = 1,
                name = "First eye examination",
                durationMinutes = 45,
            ),
        )

        assertEquals("First eye examination", appointmentRequestTitle(request))
        assertEquals("45 min visit", appointmentRequestDurationLabel(request))
    }

    @Test
    fun `rebooking request uses reschedule title`() {
        val request = request(
            id = 6,
            status = AppointmentRequestStatus.PENDING,
            requestType = AppointmentRequestType.RESCHEDULE,
            scheduledAt = "2026-08-07T10:00:00+08:00",
        )

        assertEquals("Reschedule request", appointmentRequestTitle(request))
    }

    private fun request(
        id: Int,
        status: AppointmentRequestStatus,
        scheduledAt: String,
        requestType: AppointmentRequestType = AppointmentRequestType.NEW,
        appointmentId: Int? = null,
        appointmentType: AppointmentRequestTypeSummary? = null,
    ) = AppointmentRequest(
        id = id,
        requestNumber = "APR-2026-${id.toString().padStart(6, '0')}",
        status = status,
        requestType = requestType,
        patientId = null,
        appointmentType = appointmentType,
        scheduledAt = scheduledAt,
        alternativeScheduledTimes = emptyList(),
        provisionalDurationMinutes = null,
        reasonForVisit = "Blurred vision",
        referringSource = null,
        timePreferencesAreReserved = false,
        expiresAt = null,
        cancelledAt = null,
        rejectionReason = null,
        createdAt = "2026-08-03T10:00:00+08:00",
        appointmentId = appointmentId,
    )
}
