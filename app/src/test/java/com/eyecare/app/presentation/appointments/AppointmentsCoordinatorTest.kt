package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.PatientLinkStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentsCoordinatorTest {

    @Test
    fun `linked defaults to Confirmed`() {
        val state = resolveAppointmentsCoordinator(PatientLinkStatus.LINKED)
        assertEquals(AppointmentsTab.CONFIRMED, state.selectedTab)
        assertTrue(state.isLinked)
        assertFalse(state.showLinkRequired)
    }

    @Test
    fun `unlinked defaults to Requests`() {
        val state = resolveAppointmentsCoordinator(PatientLinkStatus.UNLINKED)
        assertEquals(AppointmentsTab.REQUESTS, state.selectedTab)
        assertFalse(state.isLinked)
        assertTrue(state.showLinkRequired)
    }

    @Test
    fun `pending_review defaults to Requests`() {
        val state = resolveAppointmentsCoordinator(PatientLinkStatus.PENDING_REVIEW)
        assertEquals(AppointmentsTab.REQUESTS, state.selectedTab)
        assertFalse(state.isLinked)
        assertTrue(state.showLinkRequired)
    }

    @Test
    fun `unknown defaults to Requests`() {
        val state = resolveAppointmentsCoordinator(PatientLinkStatus.UNKNOWN)
        assertEquals(AppointmentsTab.REQUESTS, state.selectedTab)
        assertFalse(state.isLinked)
        assertTrue(state.showLinkRequired)
    }
}
