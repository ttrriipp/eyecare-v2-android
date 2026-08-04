package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
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

    private fun scheduleWithSlot(): RequestStep.Schedule {
        coEvery { repo.getAvailability("2026-08-10") } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        vm.selectSlot(fakeSlot)
        return vm.step.value as RequestStep.Schedule
    }

    @Test
    fun `initial step is Schedule`() {
        assertTrue(vm.step.value is RequestStep.Schedule)
    }

    @Test
    fun `selectDate loads availability`() {
        coEvery { repo.getAvailability("2026-08-10") } returns Result.success(fakeAvailability)
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.Schedule
        assertEquals("2026-08-10", step.date)
        assertEquals(1, step.availability?.slots?.size)
    }

    @Test
    fun `selectDate failure shows error`() {
        coEvery { repo.getAvailability(any()) } returns Result.failure(Exception("Network"))
        vm.selectDate("2026-08-10")
        val step = vm.step.value as RequestStep.Schedule
        assertEquals("Network", step.availabilityError)
    }

    @Test
    fun `selectSlot selects available slot`() {
        val step = scheduleWithSlot()
        assertEquals(fakeSlot, step.selectedSlot)
    }

    @Test
    fun `confirmSchedule moves to ProfileAndReason`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        assertTrue(vm.step.value is RequestStep.ProfileAndReason)
    }

    @Test
    fun `confirmSchedule without identity required skips identity fields`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        val step = vm.step.value as RequestStep.ProfileAndReason
        assertTrue(!step.identityRequired)
    }

    @Test
    fun `confirmSchedule with identity required seeds account details`() {
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
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true, initialIdentity = accountIdentity)

        val step = vm.step.value as RequestStep.ProfileAndReason
        assertTrue(step.identityRequired)
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
    fun `empty reason shows validation error`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("")
        vm.confirmProfileAndReason()
        val step = vm.step.value as RequestStep.ProfileAndReason
        assertEquals("Reason for visit is required", step.reasonError)
    }

    @Test
    fun `confirmProfileAndReason moves to Review without identity`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("Blurred vision")
        vm.confirmProfileAndReason()
        val step = vm.step.value as RequestStep.Review
        assertEquals("Blurred vision", step.reason)
        assertNull(step.identity)
    }

    @Test
    fun `confirmProfileAndReason rejects missing required requester details`() {
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Blurred vision")
        vm.confirmProfileAndReason()

        val step = vm.step.value as RequestStep.ProfileAndReason
        assertEquals("First name is required", step.errors["firstName"])
        assertEquals("Last name is required", step.errors["lastName"])
        assertEquals("Date of birth is required", step.errors["dateOfBirth"])
        assertEquals("A verified phone number is required", step.errors["phone"])
        assertEquals("Gender is required", step.errors["gender"])
        assertEquals("Occupation is required", step.errors["occupation"])
        assertEquals("Address is required", step.errors["address"])
    }

    @Test
    fun `confirmProfileAndReason rejects an invalid optional email`() {
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Blurred vision")
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

        vm.confirmProfileAndReason()

        val step = vm.step.value as RequestStep.ProfileAndReason
        assertEquals("Enter a valid email address", step.errors["email"])
    }

    @Test
    fun `confirmProfileAndReason moves to Review with normalized requester identity`() {
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Blurred vision")
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
        vm.confirmProfileAndReason()

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
    fun `backFromReview returns to ProfileAndReason with identity repopulated`() {
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Blurred vision")
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
        vm.confirmProfileAndReason()
        vm.backFromReview()

        val step = vm.step.value as RequestStep.ProfileAndReason
        assertTrue(step.identityRequired)
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
    fun `backToSchedule preserves the draft when returning to ProfileAndReason`() {
        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Blurred vision")
        vm.updateIdentity(
            firstName = "Alex",
            lastName = "Rivera",
            dateOfBirth = "1990-05-15",
        )
        vm.backToSchedule()
        vm.confirmSchedule(identityDetailsRequired = true)

        val step = vm.step.value as RequestStep.ProfileAndReason
        assertEquals("Blurred vision", step.reason)
        assertEquals("Alex", step.firstName)
        assertEquals("Rivera", step.lastName)
        assertEquals("1990-05-15", step.dateOfBirth)
    }

    @Test
    fun `backToSchedule reloads availability for the current date`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.backToSchedule()

        val step = vm.step.value as RequestStep.Schedule
        assertEquals("2026-08-10", step.date)
        assertEquals(fakeSlot, step.selectedSlot)
        assertEquals(1, step.availability?.slots?.size)
    }

    @Test
    fun `submit success returns request`() {
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.success(fakeRequest)
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("Test")
        vm.confirmProfileAndReason()
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
        coEvery { repo.createRequest(any(), any(), identity) } returns Result.success(fakeRequest)

        scheduleWithSlot()
        vm.confirmSchedule(identityDetailsRequired = true)
        vm.updateReason("Test")
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
        vm.confirmProfileAndReason()
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
    fun `submit SLOT_UNAVAILABLE returns to Schedule`() {
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "SLOT_UNAVAILABLE", "Slot taken.")
        )
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("Test")
        vm.confirmProfileAndReason()
        vm.submit()
        vm.handleSubmissionError()
        assertTrue(vm.step.value is RequestStep.Schedule)
    }

    @Test
    fun `submit ACTIVE_REQUEST_LIMIT_REACHED preserves draft`() {
        coEvery { repo.createRequest(any(), any(), any()) } returns Result.failure(
            ApiDomainError(422, "ACTIVE_REQUEST_LIMIT_REACHED", "Limit reached.")
        )
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("Test")
        vm.confirmProfileAndReason()
        vm.submit()
        vm.handleSubmissionError()
        val step = vm.step.value as RequestStep.SubmissionError
        assertEquals("ACTIVE_REQUEST_LIMIT_REACHED", step.errorCode)
        assertEquals("Test", step.reason)
    }

    @Test
    fun `backToSchedule preserves reason`() {
        scheduleWithSlot()
        vm.confirmSchedule()
        vm.updateReason("My reason")
        vm.backToSchedule()
        vm.confirmSchedule()
        val step = vm.step.value as RequestStep.ProfileAndReason
        assertEquals("My reason", step.reason)
    }
}
