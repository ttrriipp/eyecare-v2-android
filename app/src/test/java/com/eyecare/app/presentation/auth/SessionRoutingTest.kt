package com.eyecare.app.presentation.auth

import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SessionRoutingTest {

    private fun account(linkStatus: PatientLinkStatus) = PatientAccount(
        id = 1, name = "Test", firstName = null, middleName = null, lastName = null,
        email = null, phone = null, role = "patient", dateOfBirth = null,
        linkStatus = linkStatus, privacyPolicyVersion = null, privacyAcceptedAt = null, linkedPatient = null,
    )

    @Test
    fun `Checking resolves to SessionGate`() {
        assertEquals(AuthDestination.SessionGate, resolveDestination(SessionState.Checking))
    }

    @Test
    fun `Unauthenticated resolves to Welcome`() {
        assertEquals(AuthDestination.Welcome, resolveDestination(SessionState.Unauthenticated))
    }

    @Test
    fun `Linked resolves to Main`() {
        assertEquals(AuthDestination.Main, resolveDestination(SessionState.Linked(account(PatientLinkStatus.LINKED))))
    }

    @Test
    fun `Limited unlinked resolves to Main`() {
        assertEquals(AuthDestination.Main, resolveDestination(SessionState.Limited(account(PatientLinkStatus.UNLINKED))))
    }

    @Test
    fun `Limited pending_review resolves to Main`() {
        assertEquals(AuthDestination.Main, resolveDestination(SessionState.Limited(account(PatientLinkStatus.PENDING_REVIEW))))
    }

    @Test
    fun `Limited unknown resolves to Main`() {
        assertEquals(AuthDestination.Main, resolveDestination(SessionState.Limited(account(PatientLinkStatus.UNKNOWN))))
    }

    @Test
    fun `TransientFailure resolves to SessionGate`() {
        assertEquals(AuthDestination.SessionGate, resolveDestination(SessionState.TransientFailure("offline")))
    }

    @Test
    fun `bottom nav hidden for all non-Main destinations`() {
        assertEquals(true, shouldHideBottomNav(AuthDestination.SessionGate))
        assertEquals(true, shouldHideBottomNav(AuthDestination.Welcome))
        assertEquals(true, shouldHideBottomNav(AuthDestination.SignIn))
        assertEquals(true, shouldHideBottomNav(AuthDestination.LimitedAccount))
        assertEquals(false, shouldHideBottomNav(AuthDestination.Main))
    }
}
