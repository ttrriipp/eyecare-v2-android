package com.eyecare.app.presentation.auth

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.model.RegistrationProof
import com.eyecare.app.domain.repository.AuthRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var deviceIdentity: DeviceIdentityProvider
    private lateinit var vm: RegistrationViewModel

    private val fakePolicies = PolicyMetadata(
        "2026-08",
        "https://example.com/privacy",
        null,
        "2026-08",
        "https://example.com/terms",
        null,
    )
    private val fakeAccount = PatientAccount(
        1,
        "Test User",
        "Test",
        null,
        "User",
        "test@example.com",
        "+639171234567",
        "patient",
        "1990-01-01",
        PatientLinkStatus.LINKED,
        null,
        null,
        null,
    )
    private val fakeSession = AuthenticatedSession("token123", fakeAccount)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        deviceIdentity = mockk {
            every { deviceName() } returns "Test Device"
            every { getOrCreateInstallationId() } returns "test-id"
        }
        vm = RegistrationViewModel(authRepo, deviceIdentity)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is EnterPhone`() {
        assertTrue(vm.state.value is RegistrationState.EnterPhone)
    }

    @Test
    fun `requestPhoneOtp transitions to phone OTP`() {
        coEvery { authRepo.requestRegistrationOtp("+639171234567") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.updatePhone("+639171234567")
        vm.requestPhoneOtp()

        val state = vm.state.value as RegistrationState.VerifyPhoneOtp
        assertEquals("+639171234567", state.phoneNumber)
        assertEquals("ch-1", state.challengeId)
    }

    @Test
    fun `requestPhoneOtp failure shows backend error`() {
        coEvery { authRepo.requestRegistrationOtp(any()) } returns
            Result.failure(RuntimeException("Phone number is already registered."))

        vm.updatePhone("+639171234567")
        vm.requestPhoneOtp()

        val state = vm.state.value as RegistrationState.EnterPhone
        assertEquals("Phone number is already registered.", state.error)
    }

    @Test
    fun `verifyPhoneOtp transitions to account details`() {
        enterDetails()

        val state = vm.state.value as RegistrationState.EnterDetails
        assertEquals("registration-token", state.registrationToken)
        assertEquals("", state.email)
    }

    @Test
    fun `submitRegistration sends optional email and completes without secondary flow`() = runTest {
        enterDetails()
        coEvery {
            authRepo.register(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.success(fakeSession)

        vm.updateDetails(
            firstName = "Test",
            lastName = "User",
            dateOfBirth = "1990-01-01",
            email = "test@example.com",
            password = "123456789012",
            passwordConfirmation = "123456789012",
            privacyAccepted = true,
            termsAccepted = true,
        )
        vm.submitRegistration()

        assertTrue(vm.state.value is RegistrationState.Success)
    }

    @Test
    fun `submitRegistration shows backend field errors on matching fields`() = runTest {
        val backendError = com.eyecare.app.domain.model.ApiDomainError(
            httpStatus = 422,
            code = "VALIDATION",
            message = "The given data was invalid.",
            fieldErrors = mapOf(
                "email" to listOf("The email has already been taken."),
                "password_confirmation" to listOf("The passwords do not match."),
            ),
        )
        enterDetails()
        coEvery {
            authRepo.register(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.failure(backendError)

        vm.updateDetails(
            firstName = "Test",
            lastName = "User",
            dateOfBirth = "1990-01-01",
            email = "test@example.com",
            password = "123456789012",
            passwordConfirmation = "123456789012",
            privacyAccepted = true,
            termsAccepted = true,
        )
        vm.submitRegistration()

        val state = vm.state.value as RegistrationState.EnterDetails
        assertEquals("The email has already been taken.", state.errors["email"])
        assertEquals("The passwords do not match.", state.errors["passwordConfirmation"])
    }

    @Test
    fun `submitRegistration rejects a future date of birth before calling repository`() = runTest {
        enterDetails()
        coEvery {
            authRepo.register(
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            )
        } returns Result.success(fakeSession)

        val futureDate = LocalDate.now(ZoneId.of("Asia/Manila")).plusDays(1).toString()
        vm.updateDetails(
            firstName = "Test",
            lastName = "User",
            dateOfBirth = futureDate,
            password = "123456789012",
            passwordConfirmation = "123456789012",
            privacyAccepted = true,
            termsAccepted = true,
        )
        vm.submitRegistration()

        val state = vm.state.value as RegistrationState.EnterDetails
        assertEquals("Date of birth must be before today", state.errors["dateOfBirth"])
    }

    private fun enterDetails() {
        coEvery { authRepo.requestRegistrationOtp("+639171234567") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))
        coEvery { authRepo.verifyRegistrationOtp("ch-1", "123456") } returns
            Result.success(RegistrationProof("registration-token", "2026-08-01T10:40:00", ContactType.PHONE))
        coEvery { authRepo.getPolicies() } returns Result.success(fakePolicies)

        vm.updatePhone("+639171234567")
        vm.requestPhoneOtp()
        vm.updateOtpCode("123456")
        vm.verifyPhoneOtp()
    }
}
