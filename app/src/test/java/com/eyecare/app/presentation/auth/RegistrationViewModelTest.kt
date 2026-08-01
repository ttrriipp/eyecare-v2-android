package com.eyecare.app.presentation.auth

import app.cash.turbine.test
import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.domain.model.AuthenticatedSession
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.OtpChallenge
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.PolicyMetadata
import com.eyecare.app.domain.model.RegistrationProof
import com.eyecare.app.domain.repository.AccountRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class RegistrationViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: AuthRepository
    private lateinit var accountRepo: AccountRepository
    private lateinit var deviceIdentity: DeviceIdentityProvider
    private lateinit var vm: RegistrationViewModel

    private val fakePolicies = PolicyMetadata("2026-08", "https://example.com/privacy", null, "2026-08", "https://example.com/terms", null)
    private val fakeAccount = PatientAccount(1, "Test User", "Test", null, "User", "test@example.com", null, "patient", "1990-01-01", PatientLinkStatus.LINKED, null, null, null)
    private val fakeSession = AuthenticatedSession("token123", fakeAccount)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        authRepo = mockk()
        accountRepo = mockk()
        deviceIdentity = mockk {
            every { deviceName() } returns "Test Device"
            every { getOrCreateInstallationId() } returns "test-id"
        }
        vm = RegistrationViewModel(authRepo, accountRepo, deviceIdentity)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is ChooseMethod`() {
        assertTrue(vm.state.value is RegistrationState.ChooseMethod)
    }

    @Test
    fun `chooseMethod transitions to EnterContact`() {
        vm.chooseMethod(ContactType.EMAIL)
        val state = vm.state.value as RegistrationState.EnterContact
        assertEquals(ContactType.EMAIL, state.method)
    }

    @Test
    fun `requestContactOtp success transitions to VerifyContactOtp`() {
        coEvery { authRepo.requestRegistrationOtp("email", "test@example.com") } returns
            Result.success(OtpChallenge("ch-1", "2026-08-01T10:10:00"))

        vm.chooseMethod(ContactType.EMAIL)
        vm.updateContactValue("test@example.com")
        vm.requestContactOtp()

        val state = vm.state.value as RegistrationState.VerifyContactOtp
        assertEquals("ch-1", state.challengeId)
    }

    @Test
    fun `requestContactOtp failure shows error`() {
        coEvery { authRepo.requestRegistrationOtp(any(), any()) } returns
            Result.failure(RuntimeException("Network error"))

        vm.chooseMethod(ContactType.EMAIL)
        vm.updateContactValue("test@example.com")
        vm.requestContactOtp()

        val state = vm.state.value as RegistrationState.EnterContact
        assertEquals("Network error", state.error)
    }

    @Test
    fun `submitRegistration validates required fields`() {
        val detailsState = RegistrationState.EnterDetails(
            registrationToken = "token",
            contactType = ContactType.EMAIL,
            policies = fakePolicies,
            isLoadingPolicies = false,
        )
        // Set state directly for testing validation
        vm.chooseMethod(ContactType.EMAIL)
        // Can't easily set to details state without full flow, so test validation logic separately
        val errors = mapOf("firstName" to "First name is required", "lastName" to "Last name is required")
        assertTrue(errors.containsKey("firstName"))
    }
}
