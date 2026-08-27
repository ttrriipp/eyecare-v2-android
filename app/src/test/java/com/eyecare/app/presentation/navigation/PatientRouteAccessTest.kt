package com.eyecare.app.presentation.navigation

import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PatientRouteAccessTest {

    @Test
    fun `request routes are account-only`() {
        assertEquals(PatientRouteAccess.AccountOnly, classifyRouteAccess("RequestAppointment"))
        assertEquals(PatientRouteAccess.AccountOnly, classifyRouteAccess("AppointmentRequestList"))
        assertEquals(PatientRouteAccess.AccountOnly, classifyRouteAccess("AppointmentRequestDetail"))
    }

    @Test
    fun `confirmed appointment routes require active link`() {
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("AppointmentDetail"))
    }

    @Test
    fun `clinical routes require active link`() {
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("PatientProfile"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("PrescriptionList"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("EyewearList"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("FrameReservationList"))
    }

    @Test
    fun `reservation detail requires active link and is not mistaken for frame detail`() {
        assertEquals(
            PatientRouteAccess.ActiveLinkRequired,
            classifyRouteAccess("com.eyecare.app.presentation.navigation.FrameReservationDetail/{reservationId}"),
        )
        assertEquals(
            PatientRouteAccess.AccountOnly,
            classifyRouteAccess("com.eyecare.app.presentation.navigation.FrameDetail/{frameId}"),
        )
    }

    @Test
    fun `notifications are account-only`() {
        assertEquals(PatientRouteAccess.AccountOnly, classifyRouteAccess("Notifications"))
    }

    @Test
    fun `chat is account-only`() {
        assertEquals(PatientRouteAccess.AccountOnly, classifyRouteAccess("Chat"))
    }

    @Test
    fun `unknown routes fail closed`() {
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("UnknownRoute"))
    }

    @Test
    fun `linked account can access all routes`() {
        assertTrue(canAccessRoute("RequestAppointment", PatientLinkStatus.LINKED))
        assertTrue(canAccessRoute(PatientProfile, PatientLinkStatus.LINKED))
        assertTrue(canAccessRoute("AppointmentDetail", PatientLinkStatus.LINKED))
        assertTrue(canAccessRoute("PrescriptionList", PatientLinkStatus.LINKED))
    }

    @Test
    fun `unlinked account can access request routes only`() {
        assertTrue(canAccessRoute("RequestAppointment", PatientLinkStatus.UNLINKED))
        assertFalse(canAccessRoute("AppointmentDetail", PatientLinkStatus.UNLINKED))
        assertFalse(canAccessRoute("PrescriptionList", PatientLinkStatus.UNLINKED))
    }

    @Test
    fun `pending review can access request routes only`() {
        assertTrue(canAccessRoute("RequestAppointment", PatientLinkStatus.PENDING_REVIEW))
        assertFalse(canAccessRoute("AppointmentDetail", PatientLinkStatus.PENDING_REVIEW))
    }

    @Test
    fun `unlinked account can browse frames and request an appointment`() {
        val linkStatus = PatientLinkStatus.UNLINKED

        assertTrue(canAccessRoute(Frames, linkStatus))
        assertTrue(canAccessRoute(FrameDetail(frameId = 7), linkStatus))
        assertTrue(canAccessRoute(ArTryOn(frameId = 7, variantId = 3), linkStatus))
        assertTrue(canAccessRoute(Appointments, linkStatus))
        assertTrue(canAccessRoute(RequestAppointment, linkStatus))
    }

    @Test
    fun `unlinked account can access chat and notifications but not clinical records`() {
        val linkStatus = PatientLinkStatus.UNLINKED

        assertFalse(canAccessRoute(PatientProfile, linkStatus))
        assertFalse(canAccessRoute(AppointmentDetail(42), linkStatus))
        assertFalse(canAccessRoute(PrescriptionList, linkStatus))
        assertFalse(canAccessRoute(CreateFrameReservation(frameId = 7, variantId = 3), linkStatus))
        assertTrue(canAccessRoute(Chat, linkStatus))
        assertTrue(canAccessRoute(Notifications, linkStatus))
    }

    @Test
    fun `limited session redirects active-link destinations to the link hub`() {
        val sessionState = SessionState.Limited(testAccount())

        assertTrue(shouldRedirectToLimitedAccount("PrescriptionList", sessionState))
        assertTrue(shouldRedirectToLimitedAccount("MyOrders", sessionState))
    }

    @Test
    fun `limited session does not redirect chat`() {
        val sessionState = SessionState.Limited(testAccount())
        assertFalse(shouldRedirectToLimitedAccount("Chat", sessionState))
    }

    @Test
    fun `limited session does not redirect notifications`() {
        val sessionState = SessionState.Limited(testAccount())
        assertFalse(shouldRedirectToLimitedAccount("Notifications", sessionState))
    }

    @Test
    fun `limited session keeps account-safe destinations in the main shell`() {
        val sessionState = SessionState.Limited(testAccount())

        assertFalse(shouldRedirectToLimitedAccount("Home", sessionState))
        assertFalse(shouldRedirectToLimitedAccount("Frames", sessionState))
        assertFalse(shouldRedirectToLimitedAccount("Profile", sessionState))
    }

    @Test
    fun `linked session is not redirected from active-link destinations`() {
        assertFalse(
            shouldRedirectToLimitedAccount(
                "PrescriptionList",
                SessionState.Linked(testAccount(PatientLinkStatus.LINKED)),
            ),
        )
    }

    private fun testAccount(linkStatus: PatientLinkStatus = PatientLinkStatus.UNLINKED) = PatientAccount(
        id = 1,
        name = "Test Account",
        firstName = "Test",
        middleName = null,
        lastName = "Account",
        email = null,
        phone = "+639171234567",
        role = "patient",
        dateOfBirth = null,
        linkStatus = linkStatus,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
