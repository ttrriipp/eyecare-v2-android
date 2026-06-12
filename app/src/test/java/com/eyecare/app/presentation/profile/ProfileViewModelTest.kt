package com.eyecare.app.presentation.profile

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.User
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
        coEvery { authRepo.getUser() } returns Result.success(User(1, "Alex", "alex@example.com", "customer"))
        val vm = ProfileViewModel(authRepo, tokenManager)
        assertEquals("Alex", (vm.uiState.value as? ProfileUiState.Success)?.user?.name)
    }

    @Test
    fun `logout clears token and signals event`() = runTest {
        coEvery { authRepo.getUser() } returns Result.success(User(1, "Alex", "alex@example.com", "customer"))
        coEvery { authRepo.logout() } returns Result.success(Unit)
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.logout()
        verify { tokenManager.clearToken() }
    }
}
