package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
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
    }

    @Test
    fun `submit success returns request`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any()) } returns Result.success(fakeRequest)
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
    fun `submit SLOT_UNAVAILABLE returns to slot selection`() {
        coEvery { repo.getAvailability(any()) } returns Result.success(fakeAvailability)
        coEvery { repo.createRequest(any(), any()) } returns Result.failure(
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
        coEvery { repo.createRequest(any(), any()) } returns Result.failure(
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
