package com.eyecare.app.presentation.auth

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: AuthRepository
    private lateinit var tokenManager: TokenManager

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = mockk()
        tokenManager = mockk()
        every { tokenManager.getToken() } returns null
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `setLinkedAccount updates session without another me request`() {
        val linkedAccount = PatientAccount(
            id = 1,
            name = "Test User",
            firstName = "Test",
            middleName = null,
            lastName = "User",
            email = "test@example.com",
            phone = "09171234567",
            role = "patient",
            dateOfBirth = null,
            linkStatus = PatientLinkStatus.LINKED,
            privacyPolicyVersion = null,
            privacyAcceptedAt = null,
            linkedPatient = null,
        )
        val viewModel = SessionViewModel(authRepository, tokenManager)

        viewModel.setLinkedAccount(linkedAccount)

        assertEquals(SessionState.Linked(linkedAccount), viewModel.state.value)
        coVerify(exactly = 0) { authRepository.getMe() }
    }

    @Test
    fun `session rate limit uses retry-later copy`() {
        every { tokenManager.getToken() } returns "session-token"
        coEvery { authRepository.getMe() } returns Result.failure(
            ApiDomainError(
                httpStatus = 429,
                code = "UNKNOWN_ERROR",
                message = "Too Many Attempts.",
            ),
        )

        val viewModel = SessionViewModel(authRepository, tokenManager)

        assertEquals(
            SessionState.TransientFailure("Too many requests. Please wait before trying again."),
            viewModel.state.value,
        )
    }
}
