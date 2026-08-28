package com.eyecare.app.presentation.profile

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.LinkedPatient
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    fun `adopts the linked session account over stale profile data`() = runTest {
        val staleAccount = testAccount().copy(linkStatus = PatientLinkStatus.UNLINKED)
        val linkedAccount = testAccount().copy(
            linkedPatient = LinkedPatient(
                patientNumber = "PAT-2026-000001",
                fullName = "Alex Rivera",
                dateOfBirth = "1990-05-15",
                gender = null,
                occupation = null,
                address = null,
                phone = "09171234567",
                contactEmail = "alex@example.com",
            ),
        )
        coEvery { authRepo.getMe() } returns Result.success(staleAccount)

        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.adoptAccount(linkedAccount)

        assertEquals(linkedAccount, (vm.uiState.value as ProfileUiState.Success).account)
    }

    @Test
    fun `adopting linked session account cancels an in flight stale profile request`() = runTest {
        val staleResponse = CompletableDeferred<Result<PatientAccount>>()
        val staleAccount = testAccount().copy(linkStatus = PatientLinkStatus.UNLINKED)
        val linkedAccount = testAccount().copy(
            linkedPatient = LinkedPatient(
                patientNumber = "PAT-2026-000001",
                fullName = "Alex Rivera",
                dateOfBirth = "1990-05-15",
                gender = null,
                occupation = null,
                address = null,
                phone = "09171234567",
                contactEmail = "alex@example.com",
            ),
        )
        coEvery { authRepo.getMe() } coAnswers { staleResponse.await() }

        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.adoptAccount(linkedAccount)
        staleResponse.complete(Result.success(staleAccount))
        advanceUntilIdle()

        assertEquals(linkedAccount, (vm.uiState.value as ProfileUiState.Success).account)
    }

    @Test
    fun `limited session snapshot does not cancel fresh profile resolution`() = runTest {
        val profileResponse = CompletableDeferred<Result<PatientAccount>>()
        val limitedSnapshot = testAccount().copy(linkStatus = PatientLinkStatus.UNLINKED)
        val linkedAccount = testAccount().copy(
            linkedPatient = LinkedPatient(
                patientNumber = "PAT-2026-000001",
                fullName = "Alex Rivera",
                dateOfBirth = "1990-05-15",
                gender = null,
                occupation = null,
                address = null,
                phone = "09171234567",
                contactEmail = "alex@example.com",
            ),
        )
        coEvery { authRepo.getMe() } coAnswers { profileResponse.await() }

        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.adoptAccount(limitedSnapshot)
        profileResponse.complete(Result.success(linkedAccount))
        advanceUntilIdle()

        assertEquals(linkedAccount, (vm.uiState.value as ProfileUiState.Success).account)
    }

    @Test
    fun `logout clears token and signals event`() = runTest {
        coEvery { authRepo.getMe() } returns Result.success(testAccount())
        coEvery { authRepo.logoutCurrent() } returns Result.success(Unit)
        val vm = ProfileViewModel(authRepo, tokenManager)
        vm.logout()
        verify { tokenManager.clearToken() }
    }

    private fun testAccount() = PatientAccount(
        id = 1, name = "Alex", firstName = "Alex", middleName = null, lastName = "Rivera",
        email = "alex@example.com", phone = "09171234567", role = "patient",
        dateOfBirth = null, linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = null, privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
