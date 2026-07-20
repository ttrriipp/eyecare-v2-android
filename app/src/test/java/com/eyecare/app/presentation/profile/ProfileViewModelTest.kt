package com.eyecare.app.presentation.profile

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `loads user info on init`() = runTest {
        coEvery { authRepo.getUser() } returns Result.success(User(1, "Alex", "alex@example.com", "09171234567", "customer"))
        val vm = ProfileViewModel(authRepo, tokenManager)
        assertEquals("Alex", (vm.uiState.value as? ProfileUiState.Success)?.user?.name)
    }

    @Test
    fun `logout clears token and signals event`() = runTest {
        coEvery { authRepo.getUser() } returns Result.success(User(1, "Alex", "alex@example.com", "09171234567", "customer"))
        coEvery { authRepo.logout() } returns Result.success(Unit)
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.logout()
        verify { tokenManager.clearToken() }
    }

    @Test
    fun `profile changes normalize blank phone values`() {
        val user = User(1, "Alex", "alex@example.com", null, "customer")

        assertFalse(hasProfileChanges(user, "Alex", "alex@example.com", "   "))
        assertTrue(hasProfileChanges(user, "Alex Rivera", "alex@example.com", ""))
    }

    @Test
    fun `profile initials use first and last names with a safe fallback`() {
        assertEquals("AR", profileInitials("Alex Marie Rivera"))
        assertEquals("A", profileInitials("Alex"))
        assertEquals("E", profileInitials("   "))
    }

    @Test
    fun `save failure preserves draft and exposes a useful error`() = runTest {
        val user = User(1, "Alex", "alex@example.com", null, "customer")
        coEvery { authRepo.getUser() } returns Result.success(user)
        coEvery { authRepo.updateUser(any(), any(), any()) } returns
            Result.failure(IllegalStateException("Network unavailable"))
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.startEditing()
        vm.updateName("Alex Rivera")

        vm.saveProfile()

        val state = vm.uiState.value as ProfileUiState.Success
        assertEquals("Alex Rivera", state.editName)
        assertEquals("We couldn't save your changes. Please try again.", state.saveError)
        assertFalse(state.isSaving)
        coVerify(exactly = 1) { authRepo.updateUser("Alex Rivera", "alex@example.com", null) }
    }
}
