package com.eyecare.app.presentation.account

import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ContactType
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSecurityViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var accountRepo: AccountRepository
    private lateinit var authRepo: AuthRepository
    private lateinit var vm: AccountSecurityViewModel

    private val fakeContacts = listOf(
        AccountContact(1, ContactType.EMAIL, "a***@example.com", true, "2026-08-01"),
        AccountContact(2, ContactType.PHONE, "0917***4567", false, null),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        accountRepo = mockk()
        authRepo = mockk(relaxed = true)
        vm = AccountSecurityViewModel(accountRepo, authRepo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loadContacts success shows overview`() {
        coEvery { accountRepo.getContacts() } returns Result.success(fakeContacts)
        vm.loadContacts()
        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals(2, state.contacts.size)
    }

    @Test
    fun `loadContacts failure shows error`() {
        coEvery { accountRepo.getContacts() } returns Result.failure(Exception("Network error"))
        vm.loadContacts()
        val state = vm.state.value as AccountSecurityState.Overview
        assertEquals("Network error", state.error)
    }

    @Test
    fun `startStepUp transitions to StepUpOtp`() {
        coEvery { accountRepo.getContacts() } returns Result.success(fakeContacts)
        coEvery { accountRepo.requestStepUpOtp() } returns
            Result.success(StepUpChallenge("ch-1", "2026-08-01T10:15:00", ContactType.EMAIL, "a***@example.com"))

        vm.loadContacts()
        vm.startStepUp(StepUpAction.ChangePassword)

        val state = vm.state.value as AccountSecurityState.StepUpOtp
        assertEquals("ch-1", state.challenge.challengeId)
        assertTrue(state.pendingAction is StepUpAction.ChangePassword)
    }

    @Test
    fun `logout calls repository`() {
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)
        vm.logout()
        assertTrue(vm.state.value is AccountSecurityState.SignedOut)
    }
}
