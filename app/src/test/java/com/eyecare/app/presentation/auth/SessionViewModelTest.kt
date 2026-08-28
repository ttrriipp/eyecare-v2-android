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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
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
    fun `stale session lookup cannot replace linked handoff`() = runTest {
        every { tokenManager.getToken() } returns "session-token"
        val staleResponse = CompletableDeferred<Result<PatientAccount>>()
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
        coEvery { authRepository.getMe() } coAnswers { staleResponse.await() }

        val viewModel = SessionViewModel(authRepository, tokenManager)
        viewModel.setLinkedAccount(linkedAccount)
        staleResponse.complete(Result.success(linkedAccount.copy(linkStatus = PatientLinkStatus.UNLINKED)))
        advanceUntilIdle()

        assertEquals(SessionState.Linked(linkedAccount), viewModel.state.value)
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

    @Test
    fun `adoptAccount linked account maps to Linked state`() {
        val account = PatientAccount(
            id = 1, name = "Test", firstName = "Test", middleName = null, lastName = "User",
            email = "t@e.com", phone = "09171234567", role = "patient", dateOfBirth = null,
            linkStatus = PatientLinkStatus.LINKED, privacyPolicyVersion = null, privacyAcceptedAt = null,
            linkedPatient = null,
        )
        val viewModel = SessionViewModel(authRepository, tokenManager)
        viewModel.adoptAccount(account)
        assertEquals(SessionState.Linked(account), viewModel.state.value)
    }

    @Test
    fun `adoptAccount unlinked account maps to Limited state`() {
        val account = PatientAccount(
            id = 1, name = "Test", firstName = "Test", middleName = null, lastName = "User",
            email = "t@e.com", phone = "09171234567", role = "patient", dateOfBirth = null,
            linkStatus = PatientLinkStatus.UNLINKED, privacyPolicyVersion = null, privacyAcceptedAt = null,
            linkedPatient = null,
        )
        val viewModel = SessionViewModel(authRepository, tokenManager)
        viewModel.adoptAccount(account)
        assertEquals(SessionState.Limited(account), viewModel.state.value)
    }

    @Test
    fun `adoptAccount pending review maps to Limited state`() {
        val account = PatientAccount(
            id = 1, name = "Test", firstName = "Test", middleName = null, lastName = "User",
            email = "t@e.com", phone = "09171234567", role = "patient", dateOfBirth = null,
            linkStatus = PatientLinkStatus.PENDING_REVIEW, privacyPolicyVersion = null, privacyAcceptedAt = null,
            linkedPatient = null,
        )
        val viewModel = SessionViewModel(authRepository, tokenManager)
        viewModel.adoptAccount(account)
        assertEquals(SessionState.Limited(account), viewModel.state.value)
    }

    @Test
    fun `adoptAccount cancels stale session job`() = runTest {
        every { tokenManager.getToken() } returns "session-token"
        val staleResponse = CompletableDeferred<Result<PatientAccount>>()
        coEvery { authRepository.getMe() } coAnswers { staleResponse.await() }

        val updatedAccount = PatientAccount(
            id = 1, name = "Updated", firstName = "Updated", middleName = null, lastName = "Name",
            email = "t@e.com", phone = "09171234567", role = "patient", dateOfBirth = "1995-01-01",
            linkStatus = PatientLinkStatus.LINKED, privacyPolicyVersion = null, privacyAcceptedAt = null,
            linkedPatient = null,
        )
        val viewModel = SessionViewModel(authRepository, tokenManager)
        viewModel.adoptAccount(updatedAccount)
        staleResponse.complete(Result.success(updatedAccount.copy(firstName = "Stale")))
        advanceUntilIdle()

        assertEquals(SessionState.Linked(updatedAccount), viewModel.state.value)
    }
}
