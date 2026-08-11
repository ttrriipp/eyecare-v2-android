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
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
    fun `requesting invitation otp is single flight`() = runTest {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        val response = CompletableDeferred<Result<OtpChallenge>>()
        coEvery { accountRepo.requestInvitationOtp("INV-123") } coAnswers { response.await() }

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()
        vm.requestInvitationOtp()

        assertTrue((vm.state.value as LimitedAccountState.EnterInvitationCode).isRequesting)
        coVerify(exactly = 1) { accountRepo.requestInvitationOtp("INV-123") }

        response.complete(Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00")))
        advanceUntilIdle()

        assertTrue(vm.state.value is LimitedAccountState.VerifyInvitationOtp)
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
    fun `invitation rate limit uses retry-later copy`() {
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("INV-123") } returns Result.failure(
            ApiDomainError(
                httpStatus = 429,
                code = "UNKNOWN_ERROR",
                message = "Too Many Attempts.",
            ),
        )

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()

        val state = vm.state.value as LimitedAccountState.EnterInvitationCode
        assertEquals("Too many requests. Please wait before trying again.", state.error)
        assertEquals(false, state.isRequesting)
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
    fun `verifying invitation is single flight`() = runTest {
        val linkedAccount = unlinkedAccount.copy(
            linkStatus = PatientLinkStatus.LINKED,
        )
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("INV-123") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))
        val response = CompletableDeferred<Result<LinkState>>()
        coEvery {
            accountRepo.acceptInvitation("INV-123", "ch-1", "123456")
        } coAnswers { response.await() }
        coEvery { authRepo.getMe() } returns Result.success(linkedAccount)

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()
        vm.updateOtpCode("123456")
        vm.verifyInvitationOtp()
        vm.verifyInvitationOtp()

        assertTrue((vm.state.value as LimitedAccountState.VerifyInvitationOtp).isVerifying)
        coVerify(exactly = 1) { accountRepo.acceptInvitation("INV-123", "ch-1", "123456") }

        response.complete(Result.success(LinkState.Linked))
        advanceUntilIdle()

        assertTrue(vm.state.value is LimitedAccountState.Linked)
    }

    @Test
    fun `reloading the linked account during navigation does not reset the completed handoff`() = runTest {
        val linkedAccount = unlinkedAccount.copy(
            linkStatus = PatientLinkStatus.LINKED,
        )
        coEvery { accountRepo.getLinkState() } returns Result.success(LinkState.Unlinked)
        coEvery { accountRepo.getCurrentPatientLinkRequest() } returns Result.success(null)
        coEvery { accountRepo.requestInvitationOtp("INV-123") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))
        coEvery { accountRepo.acceptInvitation("INV-123", "ch-1", "123456") } returns
            Result.success(LinkState.Linked)
        coEvery { authRepo.getMe() } returns Result.success(linkedAccount)

        vm.load(unlinkedAccount)
        vm.startInvitationEntry()
        vm.updateInvitationCode("INV-123")
        vm.requestInvitationOtp()
        vm.updateOtpCode("123456")
        vm.verifyInvitationOtp()
        advanceUntilIdle()

        assertEquals(LimitedAccountState.Linked(linkedAccount), vm.state.value)

        // The parent session account changes to LINKED before the navigation pop finishes.
        vm.load(linkedAccount)

        assertEquals(LimitedAccountState.Linked(linkedAccount), vm.state.value)
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
