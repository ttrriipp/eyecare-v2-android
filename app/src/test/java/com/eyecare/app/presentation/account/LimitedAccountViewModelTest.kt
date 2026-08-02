package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.domain.model.PatientLinkRequestStatus
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LimitedAccountViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var vm: LimitedAccountViewModel

    private val unlinkedAccount = PatientAccount(1, "Test", "Test", null, "User", "t@t.com", null, "patient", null, PatientLinkStatus.UNLINKED, null, null, null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        accountRepo = mockk()
        vm = LimitedAccountViewModel(authRepo, accountRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Overview`() {
        assertTrue(vm.state.value is LimitedAccountState.Overview)
    }

    @Test
    fun `load sets account and refreshes link state`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        vm.load(unlinkedAccount)
        val state = vm.state.value as LimitedAccountState.Overview
        assertEquals(unlinkedAccount, state.account)
    }

    @Test
    fun `startInvitationEntry transitions to EnterInvitationCode`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        assertTrue(vm.state.value is LimitedAccountState.EnterInvitationCode)
    }

    @Test
    fun `requestInvitationOtp success transitions to VerifyInvitationOtp`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("INV-123") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()

        val state = vm.state.value as LimitedAccountState.VerifyInvitationOtp
        assertEquals("ch-1", state.challengeId)
    }

    @Test
    fun `requestInvitationOtp failure shows error`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("BAD") } returns
            Result.failure(Exception("Invalid invitation"))

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("BAD")
        vm.requestInvitationOtp()

        val state = vm.state.value as LimitedAccountState.EnterInvitationCode
        assertTrue(state.error != null)
    }

    @Test
    fun `invalid invitation code uses a specific recovery message`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("BAD") } returns Result.failure(
            ApiDomainError(
                httpStatus = 422,
                code = AuthApiCodes.INVITATION_INVALID,
                message = "Invitation is invalid.",
            ),
        )

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("BAD")
        vm.requestInvitationOtp()

        val state = vm.state.value as LimitedAccountState.EnterInvitationCode
        assertEquals(
            "That invitation code is invalid or expired. Check it and try again.",
            state.error,
        )
    }

    @Test
    fun `resending invitation otp replaces the challenge`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("INV-123") } returnsMany listOf(
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00")),
            Result.success(OtpChallenge("ch-2", "2026-08-01T10:15:00")),
        )

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()
        vm.resendInvitationOtp()

        val state = vm.state.value as LimitedAccountState.VerifyInvitationOtp
        assertEquals("ch-2", state.challengeId)
        assertEquals("2026-08-01T10:15:00", state.expiresAt)
        assertEquals(false, state.isResending)
    }

    @Test
    fun `submit clinic link request marks account as pending review`() {
        val request = PatientLinkRequest(
            requestNumber = "PLR-2026-000001",
            status = PatientLinkRequestStatus.PENDING,
            submittedAt = "2026-08-01T10:00:00+08:00",
            reviewedAt = null,
        )
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.submitPatientLinkRequest() } returns Result.success(request)

        vm.load(unlinkedAccount)
        vm.submitClinicLinkRequest()

        val state = vm.state.value as LimitedAccountState.Overview
        assertEquals(LinkState.PendingReview, state.linkState)
        assertEquals(request, state.currentLinkRequest)
    }

    @Test
    fun `submit clinic link request shows specific API error`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.submitPatientLinkRequest() } returns
            Result.failure(Exception("A link request is already pending."))

        vm.load(unlinkedAccount)
        vm.submitClinicLinkRequest()

        val state = vm.state.value as LimitedAccountState.Overview
        assertEquals("A link request is already pending.", state.requestError)
    }
}
