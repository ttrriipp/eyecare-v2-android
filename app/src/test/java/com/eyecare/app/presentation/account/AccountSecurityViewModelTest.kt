package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.AccountProfilePatch
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.ProfileFieldChange
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.model.StepUpProof
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepo: AccountRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var vm: AccountSecurityViewModel

    private val fakeAccount = PatientAccount(
        id = 1,
        name = "Alex Rivera",
        firstName = "Alex",
        middleName = "M.",
        lastName = "Rivera",
        email = "alex@example.com",
        phone = "09171234567",
        role = "patient",
        dateOfBirth = "1990-05-15",
        linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = "2026-08",
        privacyAcceptedAt = "2026-08-01T10:00:00+08:00",
        linkedPatient = null,
    )

    private val fakeContacts = listOf(
        AccountContact(
            id = 1,
            type = ContactType.EMAIL,
            maskedValue = "a***@example.com",
            isPrimary = true,
            verifiedAt = "2026-08-01T10:00:00+08:00",
        ),
        AccountContact(
            id = 2,
            type = ContactType.PHONE,
            maskedValue = "0917***4567",
            isPrimary = false,
            verifiedAt = null,
        ),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        accountRepo = mockk()
        authRepo = mockk(relaxed = true)
        coEvery { authRepo.getMe() } returns Result.success(fakeAccount)
        coEvery { accountRepo.getContacts() } returns Result.success(fakeContacts)
        vm = AccountSecurityViewModel(accountRepo, authRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loadAccount success shows overview`() {
        vm.loadAccount()
        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(fakeAccount, state.account)
        assertEquals(fakeContacts, state.contacts)
        assertEquals(null, state.contactsError)
    }

    @Test
    fun `loadAccount contact failure keeps account and exposes contact error`() {
        coEvery { accountRepo.getContacts() } returns Result.failure(Exception("Contacts unavailable"))

        vm.loadAccount()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(fakeAccount, state.account)
        assertTrue(state.contacts.isEmpty())
        assertEquals("Contacts unavailable", state.contactsError)
    }

    @Test
    fun `startAddContact can preselect the requested contact type`() {
        vm.startAddContact(ContactType.PHONE)

        val state = vm.state.value as AccountSecurityState.EnterNewContact
        assertEquals(ContactType.PHONE, state.contactType)
    }

    @Test
    fun `contact step-up failure keeps loaded contacts visible`() {
        coEvery { accountRepo.requestStepUpOtp() } returns Result.failure(Exception("Verification unavailable"))

        vm.loadAccount()
        vm.startStepUp(StepUpAction.MakePrimary(contactId = 2))

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(fakeContacts, state.contacts)
        assertEquals("Verification unavailable", state.error)
    }

    @Test
    fun `verified contact remains visible when the follow-up refresh fails`() {
        val verifiedContact = AccountContact(
            id = 3,
            type = ContactType.EMAIL,
            maskedValue = "n***@example.com",
            isPrimary = false,
            verifiedAt = "2026-08-02T10:00:00+08:00",
        )
        coEvery { accountRepo.getContacts() } returnsMany listOf(
            Result.success(fakeContacts),
            Result.failure(Exception("Refresh unavailable")),
        )
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("step-up", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))
        coEvery { accountRepo.verifyStepUpOtp("step-up", "123456") } returns
            Result.success(StepUpProof("proof", 900))
        coEvery { accountRepo.requestContactOtp("proof", "email", "new@example.com") } returns
            Result.success(com.eyecare.app.domain.model.OtpChallenge("contact", "2026-08-01T10:30:00"))
        coEvery { accountRepo.verifyContactOtp("contact", "654321") } returns Result.success(verifiedContact)

        vm.loadAccount()
        vm.startAddContact(ContactType.EMAIL)
        vm.updateNewContactValue("new@example.com")
        vm.submitNewContact()
        vm.updateStepUpCode("123456")
        vm.verifyStepUp()
        vm.updateAddContactOtpCode("654321")
        vm.verifyAddContactOtp()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("Refresh unavailable", state.contactsError)
        assertEquals(verifiedContact, state.contacts.first { it.type == ContactType.EMAIL })
    }

    @Test
    fun `contact mutations are ignored while the account editor is active`() {
        vm.loadAccount()
        vm.startAccountEditing()

        vm.startAddContact(ContactType.PHONE)
        vm.startStepUp(StepUpAction.MakePrimary(contactId = 2))

        val state = vm.state.value as AccountSecurityState.Overview
        assertTrue(state.isEditingAccount)
        assertEquals("Alex", state.editFirstName)
        coVerify(exactly = 0) { accountRepo.requestStepUpOtp() }
    }

    @Test
    fun `loadAccount failure shows error`() {
        coEvery { authRepo.getMe() } returns Result.failure(Exception("Network error"))
        vm.loadAccount()
        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("Network error", state.error)
    }

    @Test
    fun `startAccountEditing populates editable account fields`() {
        vm.loadAccount()
        vm.startAccountEditing()

        val state = vm.state.value as AccountSecurityState.Overview
        assertTrue(state.isEditingAccount)
        assertEquals("Alex", state.editFirstName)
        assertEquals("M.", state.editMiddleName)
        assertEquals("Rivera", state.editLastName)
        assertEquals("1990-05-15", state.editDateOfBirth)
    }

    @Test
    fun `startStepUp transitions to StepUpOtp`() {
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))

        vm.loadAccount()
        vm.startStepUp(StepUpAction.ChangePassword)

        val state = vm.state.value as AccountSecurityState.StepUpOtp
        assertEquals("ch-1", state.challenge.challengeId)
        assertTrue(state.pendingAction is StepUpAction.ChangePassword)
    }

    @Test
    fun `saveAccountDetails name-only patch uses updateAccountProfile without step-up`() {
        val updatedAccount = fakeAccount.copy(name = "Jamie Rivera", firstName = "Jamie")
        coEvery { authRepo.updateAccountProfile(any(), null) } returns Result.success(updatedAccount)

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(updatedAccount, state.account)
        assertEquals(fakeContacts, state.contacts)
        assertFalse(state.isEditingAccount)
        assertFalse(state.isSavingAccount)
    }

    @Test
    fun `saveAccountDetails validation errors map to field errors`() {
        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("First name is required", state.fieldErrors["first_name"])
        assertTrue(state.isEditingAccount)
    }

    @Test
    fun `saveAccountDetails middle validation error maps to middle field`() {
        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountMiddleName("A".repeat(256))
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("Middle name must be at most 255 characters", state.fieldErrors["middle_name"])
        assertTrue(state.isEditingAccount)
    }

    @Test
    fun `saveAccountDetails non-validation failure preserves draft`() {
        coEvery { authRepo.updateAccountProfile(any(), any()) } returns Result.failure(
            ApiDomainError(500, "SERVER_ERROR", "Something went wrong")
        )

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertTrue(state.isEditingAccount)
        assertFalse(state.isSavingAccount)
        assertEquals("Something went wrong", state.accountSaveError)
        assertEquals("Jamie", state.editFirstName)
    }

    @Test
    fun `saveAccountDetails keeps editor busy until direct patch completes`() {
        val response = CompletableDeferred<Result<PatientAccount>>()
        coEvery { authRepo.updateAccountProfile(any(), null) } coAnswers { response.await() }

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()
        vm.saveAccountDetails()

        val savingState = vm.state.value as AccountSecurityState.Overview
        assertTrue(savingState.isEditingAccount)
        assertTrue(savingState.isSavingAccount)
        assertEquals("Jamie", savingState.editFirstName)
        coVerify(exactly = 1) { authRepo.updateAccountProfile(any(), null) }

        response.complete(Result.success(fakeAccount.copy(name = "Jamie Rivera", firstName = "Jamie")))
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse((vm.state.value as AccountSecurityState.Overview).isEditingAccount)
    }

    @Test
    fun `profile save response cannot overwrite a signed out state`() {
        val response = CompletableDeferred<Result<PatientAccount>>()
        coEvery { authRepo.updateAccountProfile(any(), null) } coAnswers { response.await() }
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()
        vm.logout()

        response.complete(Result.success(fakeAccount.copy(name = "Jamie Rivera", firstName = "Jamie")))
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.state.value is AccountSecurityState.SignedOut)
    }

    @Test
    fun `profile field errors with unknown keys also expose a safe form error`() {
        coEvery { authRepo.updateAccountProfile(any(), null) } returns Result.failure(
            ApiDomainError(
                422,
                "VALIDATION_ERROR",
                "The profile could not be updated.",
                fieldErrors = mapOf("first_name" to listOf("Use your legal name"), "profile" to listOf("Profile is locked")),
            ),
        )

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("Use your legal name", state.fieldErrors["first_name"])
        assertEquals("The profile could not be updated.", state.accountSaveError)
        assertTrue(state.isEditingAccount)
    }

    @Test
    fun `saveAccountDetails normalized no-op exits edit mode`() {
        vm.loadAccount()
        vm.startAccountEditing()
        // Don't change anything — same values as account
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertFalse(state.isEditingAccount)
    }

    @Test
    fun `saveAccountDetails DOB change triggers step-up`() {
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.StepUpOtp
        assertTrue(state.pendingAction is StepUpAction.UpdateProfile)
    }

    @Test
    fun `DOB step-up request is single-flight and cancellation ignores late response`() {
        val response = CompletableDeferred<Result<StepUpChallenge>>()
        coEvery { accountRepo.requestStepUpOtp() } coAnswers { response.await() }

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()
        vm.saveAccountDetails()

        val requestingState = vm.state.value as AccountSecurityState.Overview
        assertTrue(requestingState.isRequestingStepUp)
        coVerify(exactly = 1) { accountRepo.requestStepUpOtp() }

        vm.cancelAccountEditing()
        response.complete(Result.success(StepUpChallenge("late", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com")))
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.state.value as AccountSecurityState.Overview
        assertFalse(state.isEditingAccount)
        assertFalse(state.isRequestingStepUp)
    }

    @Test
    fun `busy profile step-up cannot be interrupted by another step-up action`() {
        val response = CompletableDeferred<Result<StepUpChallenge>>()
        coEvery { accountRepo.requestStepUpOtp() } coAnswers { response.await() }

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()
        vm.startStepUp(StepUpAction.ChangePassword)

        assertTrue((vm.state.value as AccountSecurityState.Overview).isRequestingStepUp)
        coVerify(exactly = 1) { accountRepo.requestStepUpOtp() }
        vm.cancelAccountEditing()
        response.cancel()
    }

    @Test
    fun `cancelStepUp from UpdateProfile restores editing state with draft`() {
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()

        vm.cancelStepUp()

        val state = vm.state.value as AccountSecurityState.Overview
        assertTrue(state.isEditingAccount)
        assertEquals("Jamie", state.editFirstName)
        assertEquals("1995-01-01", state.editDateOfBirth)
    }

    @Test
    fun `step-up OTP failure preserves draft for UpdateProfile`() {
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))
        coEvery { accountRepo.verifyStepUpOtp(any(), any()) } returns Result.failure(Exception("Invalid code"))

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()

        vm.updateStepUpCode("999999")
        vm.verifyStepUp()

        val state = vm.state.value as AccountSecurityState.StepUpOtp
        assertEquals("Invalid code", state.error)
        assertTrue(state.pendingAction is StepUpAction.UpdateProfile)
    }

    @Test
    fun `step-up verification is single-flight`() {
        val verification = CompletableDeferred<Result<StepUpProof>>()
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))
        coEvery { accountRepo.verifyStepUpOtp("ch-1", "123456") } coAnswers { verification.await() }

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()
        vm.updateStepUpCode("123456")
        vm.verifyStepUp()
        vm.verifyStepUp()

        assertTrue((vm.state.value as AccountSecurityState.StepUpOtp).isVerifying)
        coVerify(exactly = 1) { accountRepo.verifyStepUpOtp("ch-1", "123456") }

        verification.complete(Result.failure(Exception("Invalid code")))
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.state.value as AccountSecurityState.StepUpOtp
        assertFalse(state.isVerifying)
        assertEquals("Invalid code", state.error)
    }

    @Test
    fun `step-up request failure restores editing with draft preserved`() {
        coEvery { accountRepo.requestStepUpOtp() } returns Result.failure(Exception("Network error"))

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertTrue(state.isEditingAccount)
        assertEquals("Jamie", state.editFirstName)
        assertEquals("1995-01-01", state.editDateOfBirth)
        assertEquals("Network error", state.accountSaveError)
    }

    @Test
    fun `DOB step-up success sends patch with proof token`() {
        val updatedAccount = fakeAccount.copy(dateOfBirth = "1995-01-01")
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))
        coEvery { accountRepo.verifyStepUpOtp("ch-1", "123456") } returns Result.success(StepUpProof("proof-token", 900))
        coEvery { authRepo.updateAccountProfile(any(), "proof-token") } returns Result.success(updatedAccount)

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountDateOfBirth("1995-01-01")
        vm.saveAccountDetails()

        vm.updateStepUpCode("123456")
        vm.verifyStepUp()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(updatedAccount, state.account)
        assertFalse(state.isEditingAccount)
    }

    @Test
    fun `logout calls repository`() {
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)
        vm.logout()
        assertTrue(vm.state.value is AccountSecurityState.SignedOut)
    }
}
