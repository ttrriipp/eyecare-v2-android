package com.eyecare.app.presentation.navigation

import com.eyecare.app.domain.model.PatientLinkStatus
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
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("PrescriptionList"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("EyewearList"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("FrameReservationList"))
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("Chat"))
    }

    @Test
    fun `unknown routes fail closed`() {
        assertEquals(PatientRouteAccess.ActiveLinkRequired, classifyRouteAccess("UnknownRoute"))
    }

    @Test
    fun `linked account can access all routes`() {
        assertTrue(canAccessRoute("RequestAppointment", PatientLinkStatus.LINKED))
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
}
