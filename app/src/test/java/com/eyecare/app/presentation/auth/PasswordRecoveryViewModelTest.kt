package com.eyecare.app.presentation.auth

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
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
class PasswordRecoveryViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var deviceIdentity: DeviceIdentityProvider
    private lateinit var vm: PasswordRecoveryViewModel

    private val fakeAccount = PatientAccount(1, "Test", "Test", null, "User", "t@t.com", null, "patient", null, PatientLinkStatus.LINKED, null, null, null)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        deviceIdentity = mockk {
            every { deviceName() } returns "Test Device"
            every { getOrCreateInstallationId() } returns "test-id"
        }
        vm = PasswordRecoveryViewModel(authRepo, deviceIdentity)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is EnterContact`() {
        assertTrue(vm.state.value is RecoveryState.EnterContact)
    }

    @Test
    fun `requestOtp success transitions to EnterOtp`() {
        coEvery { authRepo.requestPasswordRecoveryOtp("test@example.com") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.updateContact("test@example.com")
        vm.requestOtp()

        val state = vm.state.value as RecoveryState.EnterOtp
        assertEquals("ch-1", state.challengeId)
    }

    @Test
    fun `verifyOtp transitions to EnterNewPassword`() {
        coEvery { authRepo.requestPasswordRecoveryOtp(any()) } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.updateContact("test@example.com")
        vm.requestOtp()
        vm.updateOtpCode("123456")
        vm.verifyOtp()

        assertTrue(vm.state.value is RecoveryState.EnterNewPassword)
    }

    @Test
    fun `resetPassword validates password length`() {
        coEvery { authRepo.requestPasswordRecoveryOtp(any()) } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.updateContact("test@example.com")
        vm.requestOtp()
        vm.updateOtpCode("123456")
        vm.verifyOtp()
        vm.updatePassword("short")
        vm.updatePasswordConfirmation("short")
        vm.resetPassword()

        val state = vm.state.value as RecoveryState.EnterNewPassword
        assertTrue(state.errors.containsKey("password"))
    }

    @Test
    fun `resetPassword validates password match`() {
        coEvery { authRepo.requestPasswordRecoveryOtp(any()) } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.updateContact("test@example.com")
        vm.requestOtp()
        vm.updateOtpCode("123456")
        vm.verifyOtp()
        vm.updatePassword("longpassword123")
        vm.updatePasswordConfirmation("different12345")
        vm.resetPassword()

        val state = vm.state.value as RecoveryState.EnterNewPassword
        assertTrue(state.errors.containsKey("passwordConfirmation"))
    }

    @Test
    fun `resetPassword success returns session`() {
        coEvery { authRepo.requestPasswordRecoveryOtp(any()) } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))
        coEvery { authRepo.recoverPassword(any(), any(), any(), any(), any(), any()) } returns
            Result.success(AuthenticatedSession("token", fakeAccount))

        vm.updateContact("test@example.com")
        vm.requestOtp()
        vm.updateOtpCode("123456")
        vm.verifyOtp()
        vm.updatePassword("newpassword123")
        vm.updatePasswordConfirmation("newpassword123")
        vm.resetPassword()

        assertTrue(vm.state.value is RecoveryState.Success)
    }
}
