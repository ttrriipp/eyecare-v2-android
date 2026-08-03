package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestAppointmentViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository
    private lateinit var vm: RequestAppointmentViewModel

    private val fakeSlot = AvailabilitySlot(
        startsAt = "2026-08-10T09:00:00+08:00",
        endsAt = "2026-08-10T09:30:00+08:00",
        available = true,
        reason = null,
    )

    private val fakeAvailability = AppointmentRequestAvailability(
        date = "2026-08-10",
        timezone = "Asia/Manila",
        intervalMinutes = 30,
        slotDurationMinutes = 30,
        dayStatus = "open",
        generatedAt = "2026-08-09T10:00:00+08:00",
        slots = listOf(fakeSlot),
    )

    private val fakeRequest = AppointmentRequest(
        id = 1,
        requestNumber = "APR-2026-000001",
        status = AppointmentRequestStatus.PENDING,
        patientId = null,
        scheduledAt = "2026-08-10T09:00:00+08:00",
        reasonForVisit = "Test",
        expiresAt = "2026-08-11T09:00:00+08:00",
        cancelledAt = null,
        createdAt = "2026-08-09T10:00:00+08:00",
        appointmentId = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        vm = RequestAppointmentViewModel(repo)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial step is ChooseDate`() {
        assertTrue(vm.step.value is RequestStep.ChooseDate)
    }

    @Test
    fun `selectDate loads availability`() {
        coEvery { repo.getAvailability("2026-08-10") } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.ChooseSlot
        assertEquals("2026-08-10", step.date)
        assertEquals(1, step.availability?.slots?.size)
    }

    @Test
    fun `selectDate failure shows error`() {
        coEvery { repo.getAvailability(any()) } returns Result.failure(Exception("Network"))
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.ChooseSlot
        assertEquals("Network", step.error)
    }

    @Test
    fun `selectSlot selects available slot`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        val step = vm.step.value as RequestStep.ChooseSlot
        assertEquals(fakeSlot, step.selectedSlot)
    }

    @Test
    fun `confirmSlot moves to EnterReason`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        assertTrue(vm.step.value is RequestStep.EnterReason)
    }

    @Test
    fun `empty reason shows validation error`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("")
        vm.confirmReason()
        val step = vm.step.value as RequestStep.EnterReason
        assertEquals("Reason for visit is required", step.reasonError)
    }

    @Test
    fun `confirmReason moves to Review`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason()
        val step = vm.step.value as RequestStep.Review
        assertEquals("Blurred vision", step.reason)
        assertNull(step.identity)
    }

    @Test
    fun `confirmReason moves to identity step with account details when identity is required`() {
        val accountIdentity = AppointmentRequestIdentity(
            phone = "+639171234567",
            email = "alex@example.com",
            firstName = "Alex",
            middleName = "M",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = "Teacher",
            address = "123 Main St, Manila",
        )
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(
            identityDetailsRequired = true,
            initialIdentity = accountIdentity,
        )

        val step = vm.step.value as RequestStep.EnterIdentity
        assertEquals("Alex", step.firstName)
        assertEquals("M", step.middleName)
        assertEquals("Rivera", step.lastName)
        assertEquals("1990-05-15", step.dateOfBirth)
        assertEquals("+639171234567", step.phone)
        assertEquals("alex@example.com", step.email)
        assertEquals(AppointmentRequestGender.FEMALE, step.gender)
        assertEquals("Teacher", step.occupation)
        assertEquals("123 Main St, Manila", step.address)
    }

    @Test
    fun `confirmIdentity rejects missing required requester details`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(identityDetailsRequired = true)
        vm.confirmIdentity()

        val step = vm.step.value as RequestStep.EnterIdentity
        assertEquals("First name is required", step.errors["firstName"])
        assertEquals("Last name is required", step.errors["lastName"])
        assertEquals("Date of birth is required", step.errors["dateOfBirth"])
        assertEquals("A verified phone number is required", step.errors["phone"])
        assertEquals("Gender is required", step.errors["gender"])
        assertEquals("Occupation is required", step.errors["occupation"])
        assertEquals("Address is required", step.errors["address"])
    }

    @Test
    fun `confirmIdentity rejects an invalid optional email`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(identityDetailsRequired = true)
        vm.updateIdentity(
            phone = "+639171234567",
            email = "not-an-email",
            firstName = "Alex",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = "Teacher",
            address = "123 Main St, Manila",
        )

        vm.confirmIdentity()

        val step = vm.step.value as RequestStep.EnterIdentity
        assertEquals("Enter a valid email address", step.errors["email"])
    }

    @Test
    fun `confirmIdentity moves to Review with normalized requester identity`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(identityDetailsRequired = true)
        vm.updateIdentity(
            phone = "+639171234567",
            email = "alex@example.com",
            firstName = "  Alex ",
            middleName = " ",
            lastName = " Rivera ",
            dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = " Teacher ",
            address = " 123 Main St, Manila ",
        )
        vm.confirmIdentity()

        val step = vm.step.value as RequestStep.Review
        assertEquals(
            AppointmentRequestIdentity(
                phone = "+639171234567",
                email = "alex@example.com",
                firstName = "Alex",
                middleName = null,
                lastName = "Rivera",
                dateOfBirth = "1990-05-15",
                gender = AppointmentRequestGender.FEMALE,
                occupation = "Teacher",
                address = "123 Main St, Manila",
            ),
            step.identity,
        )
    }

    @Test
    fun `backFromReview returns to identity step when requester identity was collected`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(identityDetailsRequired = true)
        vm.updateIdentity(
            phone = "+639171234567",
            email = "alex@example.com",
            firstName = "Alex",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = "Teacher",
            address = "123 Main St, Manila",
        )
        vm.confirmIdentity()
        vm.backFromReview()

        val step = vm.step.value as RequestStep.EnterIdentity
        assertEquals("+639171234567", step.phone)
        assertEquals("alex@example.com", step.email)
        assertEquals("Alex", step.firstName)
        assertEquals("Rivera", step.lastName)
        assertEquals("1990-05-15", step.dateOfBirth)
        assertEquals(AppointmentRequestGender.FEMALE, step.gender)
        assertEquals("Teacher", step.occupation)
        assertEquals("123 Main St, Manila", step.address)
    }

    @Test
    fun `back from identity preserves the draft when returning from reason`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Blurred vision")
        vm.confirmReason(identityDetailsRequired = true)
        vm.updateIdentity(
            firstName = "Alex",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
        )
        vm.backToReason()
        vm.confirmReason(identityDetailsRequired = true)

        val step = vm.step.value as RequestStep.EnterIdentity
        assertEquals("Alex", step.firstName)
        assertEquals("Rivera", step.lastName)
        assertEquals("1990-05-15", step.dateOfBirth)
    }

    @Test
    fun `submit success returns request`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.success(fakeRequest)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Test")
        vm.confirmReason()
        vm.submit()
        val step = vm.step.value as RequestStep.Success
        assertEquals(1, step.request.id)
    }

    @Test
    fun `submit passes requester identity to repository`() {
        val identity = AppointmentRequestIdentity(
            phone = "+639171234567",
            email = "alex@example.com",
            firstName = "Alex",
            middleName = "M",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
            gender = AppointmentRequestGender.FEMALE,
            occupation = "Teacher",
            address = "123 Main St, Manila",
        )
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any(), identity) } returns Result.success(fakeRequest)

        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Test")
        vm.confirmReason(identityDetailsRequired = true)
        vm.updateIdentity(
            phone = "+639171234567",
            email = "alex@example.com",
            firstName = identity.firstName,
            middleName = identity.middleName,
            lastName = identity.lastName,
            dateOfBirth = identity.dateOfBirth,
            gender = identity.gender,
            occupation = identity.occupation,
            address = identity.address,
        )
        vm.confirmIdentity()
        vm.submit()

        coVerify {
            repo.createRequest(
                scheduledAt = fakeSlot.startsAt,
                reasonForVisit = "Test",
                identity = identity,
            )
        }
    }

    @Test
    fun `submit SLOT_UNAVAILABLE returns to slot selection`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "SLOT_UNAVAILABLE", "Slot taken.")
        )
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Test")
        vm.confirmReason()
        vm.submit()
        vm.handleSubmissionError()
        assertTrue(vm.step.value is RequestStep.ChooseSlot)
    }

    @Test
    fun `submit ACTIVE_REQUEST_LIMIT_REACHED preserves draft`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "ACTIVE_REQUEST_LIMIT_REACHED", "Limit reached.")
        )
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("Test")
        vm.confirmReason()
        vm.submit()
        vm.handleSubmissionError()
        val step = vm.step.value as RequestStep.SubmissionError
        assertEquals("ACTIVE_REQUEST_LIMIT_REACHED", step.errorCode)
        assertEquals("Test", step.reason)
    }

    @Test
    fun `backToReason preserves reason`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        vm.confirmSlot()
        vm.updateReason("My reason")
        vm.confirmReason()
        vm.backToReason()
        val step = vm.step.value as RequestStep.EnterReason
        assertEquals("My reason", step.reason)
    }
}
