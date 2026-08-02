package com.eyecare.app.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountAccessTest {

    @Test
    fun `PatientLinkStatus fromRaw maps known values`() {
        assertEquals(PatientLinkStatus.LINKED, PatientLinkStatus.fromRaw("linked"))
        assertEquals(PatientLinkStatus.PENDING_REVIEW, PatientLinkStatus.fromRaw("pending_review"))
        assertEquals(PatientLinkStatus.UNLINKED, PatientLinkStatus.fromRaw("unlinked"))
    }

    @Test
    fun `PatientLinkStatus fromRaw maps unknown and null to UNKNOWN`() {
        assertEquals(PatientLinkStatus.UNKNOWN, PatientLinkStatus.fromRaw(null))
        assertEquals(PatientLinkStatus.UNKNOWN, PatientLinkStatus.fromRaw(""))
        assertEquals(PatientLinkStatus.UNKNOWN, PatientLinkStatus.fromRaw("bogus"))
    }

    @Test
    fun `ContactType fromRaw maps known values`() {
        assertEquals(ContactType.EMAIL, ContactType.fromRaw("email"))
        assertEquals(ContactType.PHONE, ContactType.fromRaw("phone"))
    }

    @Test
    fun `resolveSessionState null account is Unauthenticated`() {
        assertEquals(SessionState.Unauthenticated, resolveSessionState(null))
    }

    @Test
    fun `resolveSessionState linked account is Linked`() {
        val account = testAccount(PatientLinkStatus.LINKED)
        val state = resolveSessionState(account)
        assertTrue(state is SessionState.Linked && state.account === account)
    }

    @Test
    fun `resolveSessionState unlinked account is Limited`() {
        val account = testAccount(PatientLinkStatus.UNLINKED)
        assertTrue(resolveSessionState(account) is SessionState.Limited)
    }

    @Test
    fun `resolveSessionState pending_review account is Limited`() {
        assertTrue(resolveSessionState(testAccount(PatientLinkStatus.PENDING_REVIEW)) is SessionState.Limited)
    }

    @Test
    fun `resolveSessionState unknown account is Limited`() {
        assertTrue(resolveSessionState(testAccount(PatientLinkStatus.UNKNOWN)) is SessionState.Limited)
    }

    @Test
    fun `routeFromLinkStatus maps LINKED to MainGraph`() {
        assertEquals(RouteDestination.MainGraph, routeFromLinkStatus(PatientLinkStatus.LINKED))
    }

    @Test
    fun `routeFromLinkStatus maps non-LINKED to MainGraph with gated features`() {
        assertEquals(RouteDestination.MainGraph, routeFromLinkStatus(PatientLinkStatus.UNLINKED))
        assertEquals(RouteDestination.MainGraph, routeFromLinkStatus(PatientLinkStatus.PENDING_REVIEW))
        assertEquals(RouteDestination.MainGraph, routeFromLinkStatus(PatientLinkStatus.UNKNOWN))
    }

    @Test
    fun `only linked sessions can access patient features`() {
        assertTrue(canAccessPatientFeatures(SessionState.Linked(testAccount(PatientLinkStatus.LINKED))))
        assertTrue(!canAccessPatientFeatures(SessionState.Limited(testAccount(PatientLinkStatus.UNLINKED))))
        assertTrue(!canAccessPatientFeatures(SessionState.Limited(testAccount(PatientLinkStatus.PENDING_REVIEW))))
        assertTrue(!canAccessPatientFeatures(SessionState.Limited(testAccount(PatientLinkStatus.UNKNOWN))))
    }

    @Test
    fun `LinkState fromStatus maps correctly`() {
        assertTrue(LinkState.fromStatus(PatientLinkStatus.LINKED) is LinkState.Linked)
        assertTrue(LinkState.fromStatus(PatientLinkStatus.PENDING_REVIEW) is LinkState.PendingReview)
        assertTrue(LinkState.fromStatus(PatientLinkStatus.UNLINKED) is LinkState.Unlinked)
        assertTrue(LinkState.fromStatus(PatientLinkStatus.UNKNOWN) is LinkState.Unknown)
    }

    private fun testAccount(linkStatus: PatientLinkStatus) = PatientAccount(
        id = 1,
        name = "Test User",
        firstName = "Test",
        middleName = null,
        lastName = "User",
        email = "test@example.com",
        phone = null,
        role = "patient",
        dateOfBirth = null,
        linkStatus = linkStatus,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
