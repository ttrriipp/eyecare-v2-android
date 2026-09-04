package com.eyecare.app.presentation.auth

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.LoginOutcome
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var deviceIdentity: DeviceIdentityProvider
    private lateinit var vm: SignInViewModel

    private val fakeAccount = PatientAccount(1, "Test", "Test", null, "User", "t@t.com", null, "patient", null, PatientLinkStatus.LINKED, null, null, null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        deviceIdentity = mockk {
            every { deviceName() } returns "Test Device"
            every { getOrCreateInstallationId() } returns "test-id"
        }
        vm = SignInViewModel(authRepo, deviceIdentity)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is EnterCredentials`() {
        assertTrue(vm.state.value is SignInState.EnterCredentials)
    }

    @Test
    fun `trusted login returns Success`() {
        coEvery { authRepo.beginLogin(any(), any(), any(), any()) } returns
            Result.success(LoginOutcome.Authenticated("token", fakeAccount))

        vm.updatePhone("+639171234567")
        vm.updatePassword("password123")
        vm.signIn()

        assertTrue(vm.state.value is SignInState.Success)
    }

    @Test
    fun `untrusted login transitions to VerifyOtp`() {
        coEvery { authRepo.beginLogin(any(), any(), any(), any()) } returns
            Result.success(LoginOutcome.OtpRequired("ch-1", "2026-08-01T10:10:00"))

        vm.updatePhone("+639171234567")
        vm.updatePassword("password123")
        vm.signIn()

        val state = vm.state.value as SignInState.VerifyOtp
        assertEquals("ch-1", state.challengeId)
    }

    @Test
    fun `verifyOtp ignores repeated submissions while verification is pending`() = runTest {
        coEvery { authRepo.beginLogin(any(), any(), any(), any()) } returns
            Result.success(LoginOutcome.OtpRequired("ch-1", "2026-08-01T10:10:00"))
        val verification = CompletableDeferred<Result<AuthenticatedSession>>()
        coEvery { authRepo.verifyLogin("ch-1", "123456", "Test Device", "test-id") } coAnswers {
            verification.await()
        }

        vm.updatePhone("+639171234567")
        vm.updatePassword("password123")
        vm.signIn()
        vm.updateOtpCode("123456")
        vm.verifyOtp()
        vm.verifyOtp()

        coVerify(exactly = 1) {
            authRepo.verifyLogin("ch-1", "123456", "Test Device", "test-id")
        }
        verification.complete(Result.failure(IllegalStateException("verification failed")))
    }

    @Test
    fun `login failure shows error`() {
        coEvery { authRepo.beginLogin(any(), any(), any(), any()) } returns
            Result.failure(Exception("Invalid credentials"))

        vm.updatePhone("+639171234567")
        vm.updatePassword("wrong")
        vm.signIn()

        val state = vm.state.value as SignInState.EnterCredentials
        assertTrue(state.error != null)
    }

    @Test
    fun `empty contact shows validation error`() {
        vm.updatePassword("password123")
        vm.signIn()

        val state = vm.state.value as SignInState.EnterCredentials
        assertEquals("Phone number is required", state.error)
    }

    @Test
    fun `back from OTP clears password`() {
        coEvery { authRepo.beginLogin(any(), any(), any(), any()) } returns
            Result.success(LoginOutcome.OtpRequired("ch-1", "2026-08-01T10:10:00"))

        vm.updatePhone("+639171234567")
        vm.updatePassword("password123")
        vm.signIn()
        vm.back()

        val state = vm.state.value as SignInState.EnterCredentials
        assertEquals("+639171234567", state.phoneNumber)
        assertEquals("", state.password)
    }
}
