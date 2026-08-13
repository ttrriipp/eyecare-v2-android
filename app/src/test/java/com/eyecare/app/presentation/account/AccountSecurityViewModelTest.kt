package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.model.StepUpProof
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

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        accountRepo = mockk()
        authRepo = mockk(relaxed = true)
        coEvery { authRepo.getMe() } returns Result.success(fakeAccount)
        vm = AccountSecurityViewModel(accountRepo, authRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loadAccount success shows overview`() {
        vm.loadAccount()
        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(fakeAccount, state.account)
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
        assertEquals("Rivera", state.editLastName)
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
    fun `saveAccountDetails updates account and exits edit mode`() {
        val updatedAccount = fakeAccount.copy(name = "Jamie Rivera", firstName = "Jamie")
        coEvery { authRepo.updateAccountName("Jamie", "Rivera") } returns Result.success(updatedAccount)

        vm.loadAccount()
        vm.startAccountEditing()
        vm.updateAccountFirstName("Jamie")
        vm.saveAccountDetails()

        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(updatedAccount, state.account)
        assertFalse(state.isEditingAccount)
        assertFalse(state.isSavingAccount)
    }

    @Test
    fun `logout calls repository`() {
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)
        vm.logout()
        assertTrue(vm.state.value is AccountSecurityState.SignedOut)
    }
}
