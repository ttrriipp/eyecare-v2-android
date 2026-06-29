package com.eyecare.app.presentation.auth

import app.cash.turbine.test
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.domain.model.AuthError
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repo: AuthRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var vm: AuthViewModel

    private val fakeUser = User(1, "Jane", "jane@example.com", "09171234567", "customer")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk()
        tokenManager = mockk(relaxed = true)
        vm = AuthViewModel(repo, tokenManager)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Idle`() = runTest {
        vm.uiState.test {
            assertInstanceOf(AuthUiState.Idle::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login success emits Success state`() = runTest {
        coEvery { repo.login(any(), any()) } returns Result.success(fakeUser)

        vm.uiState.test {
            assertInstanceOf(AuthUiState.Idle::class.java, awaitItem())
            vm.login("jane@example.com", "password1")
            assertInstanceOf(AuthUiState.Loading::class.java, awaitItem())
            testDispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AuthUiState.Success::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login validation error emits ValidationError state`() = runTest {
        val errors = mapOf("email" to listOf("Invalid email"))
        coEvery { repo.login(any(), any()) } returns
            Result.failure(AuthError.ValidationError(errors))

        vm.uiState.test {
            awaitItem() // Idle
            vm.login("bad@x.com", "password1")
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertInstanceOf(AuthUiState.ValidationError::class.java, state)
            assertEquals(errors, (state as AuthUiState.ValidationError).fieldErrors)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login rate limit emits RateLimit state`() = runTest {
        coEvery { repo.login(any(), any()) } returns
            Result.failure(AuthError.RateLimitError)

        vm.uiState.test {
            awaitItem() // Idle
            vm.login("jane@example.com", "password1")
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AuthUiState.RateLimit::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login with empty email emits local ValidationError without API call`() = runTest {
        vm.uiState.test {
            awaitItem() // Idle
            vm.login("", "password1")
            val state = awaitItem()
            assertInstanceOf(AuthUiState.ValidationError::class.java, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `login with short password emits local ValidationError without API call`() = runTest {
        vm.uiState.test {
            awaitItem() // Idle
            vm.login("jane@example.com", "abc")
            val state = awaitItem()
            assertInstanceOf(AuthUiState.ValidationError::class.java, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `register with mismatched passwords emits local ValidationError`() = runTest {
        vm.uiState.test {
            awaitItem() // Idle
            vm.register("Jane", "jane@example.com", null, "password1", "different")
            val state = awaitItem()
            assertInstanceOf(AuthUiState.ValidationError::class.java, state)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `register success emits Success state`() = runTest {
        coEvery { repo.register(any(), any(), anyNullable(), any(), any()) } returns Result.success(fakeUser)

        vm.uiState.test {
            awaitItem() // Idle
            vm.register("Jane", "jane@example.com", null, "password1", "password1")
            awaitItem() // Loading
            testDispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AuthUiState.Success::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
