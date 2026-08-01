package com.eyecare.app.presentation.profile

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        tokenManager = mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads account on init`() = runTest {
        coEvery { authRepo.getMe() } returns Result.success(testAccount())
        val vm = ProfileViewModel(authRepo, tokenManager)
        assertEquals("Alex", (vm.uiState.value as? ProfileUiState.Success)?.account?.name)
    }

    @Test
    fun `logout clears token and signals event`() = runTest {
        coEvery { authRepo.getMe() } returns Result.success(testAccount())
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.logout()
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `startEditing populates edit fields`() = runTest {
        coEvery { authRepo.getMe() } returns Result.success(testAccount())
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.startEditing()
        val state = vm.uiState.value as ProfileUiState.Success
        assertEquals("Alex", state.editFirstName)
        assertEquals("Rivera", state.editLastName)
    }

    @Test
    fun `save failure preserves draft`() = runTest {
        coEvery { authRepo.getMe() } returns Result.success(testAccount())
        coEvery { authRepo.updateAccountName(any(), any()) } returns
            Result.failure(IllegalStateException("Network"))
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.startEditing()
        vm.updateFirstName("New Name")
        vm.saveProfile()
        val state = vm.uiState.value as ProfileUiState.Success
        assertEquals("New Name", state.editFirstName)
        assertEquals("We couldn't save your changes. Please try again.", state.saveError)
    }

    private fun testAccount() = PatientAccount(
        id = 1, name = "Alex", firstName = "Alex", middleName = null, lastName = "Rivera",
        email = "alex@example.com", phone = "09171234567", role = "patient",
        dateOfBirth = null, linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = null, privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
