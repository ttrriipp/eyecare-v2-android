package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameReservationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var appointments: AppointmentV1Repository
    private lateinit var reservations: FrameReservationRepository
    private lateinit var viewModel: AppointmentDetailViewModel

    private val appointment = AppointmentV1(
        id = 4,
        appointmentNumber = "APT-004",
        appointmentType = "Follow-up",
        durationMinutes = 15,
        referringSource = null,
        status = AppointmentStatus.SCHEDULED,
        scheduledAt = "2026-07-14T09:00:00+08:00",
        contactNotes = "Original note",
        reasonForVisit = null,
        lastRescheduleReason = "Doctor availability changed",
        source = "mobile",
        assignedOptometrist = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        appointments = mockk()
        reservations = mockk()
        coEvery { appointments.getAppointment(4) } returns Result.success(appointment)
        coEvery { reservations.getReservations() } returns Result.success(emptyList())
        viewModel = AppointmentDetailViewModel(
            repository = appointments,
            reservationRepository = reservations,
            savedStateHandle = SavedStateHandle(mapOf("appointmentId" to 4)),
        )
        dispatcher.scheduler.advanceUntilIdle()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `customer reschedule uses returned appointment and clears staff reason without refetch`() = runTest {
        val updated = appointment.copy(
            scheduledAt = "2026-07-15T10:00:00+08:00",
            status = AppointmentStatus.SCHEDULED,
            lastRescheduleReason = null,
        )
        coEvery {
            appointments.rescheduleAppointment(4, "2026-07-15T10:00:00+08:00")
        } returns Result.success(updated)

        viewModel.rescheduleAppointment("2026-07-15T10:00:00+08:00")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals(null, state.appointment.lastRescheduleReason)
        assertEquals("2026-07-15T10:00:00+08:00", state.appointment.scheduledAt)
        assertTrue(state.showRescheduleSuccessDialog)
        coVerify(exactly = 1) { appointments.getAppointment(4) }
    }

    @Test
    fun `cancel uses returned appointment without refetch`() = runTest {
        val cancelled = appointment.copy(status = AppointmentStatus.CANCELLED)
        coEvery { appointments.cancelAppointment(4) } returns Result.success(cancelled)

        viewModel.cancelAppointment()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals(AppointmentStatus.CANCELLED, state.appointment.status)
        assertFalse(state.isCancelling)
        coVerify(exactly = 1) { appointments.getAppointment(4) }
    }

    @Test
    fun `cancel error preserves current state with error message`() = runTest {
        coEvery { appointments.cancelAppointment(4) } returns
            Result.failure(RuntimeException("Cannot cancel"))

        viewModel.cancelAppointment()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals("Cannot cancel", state.cancelError)
        assertFalse(state.isCancelling)
        assertEquals(AppointmentStatus.SCHEDULED, state.appointment.status)
    }

    @Test
    fun `load failure shows error state`() = runTest {
        coEvery { appointments.getAppointment(4) } returns Result.failure(RuntimeException("Network error"))
        val vm = AppointmentDetailViewModel(
            repository = appointments,
            reservationRepository = reservations,
            savedStateHandle = SavedStateHandle(mapOf("appointmentId" to 4)),
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as AppointmentDetailUiState.Error
        assertEquals("Network error", state.message)
    }

    @Test
    fun `refresh reloads appointment`() = runTest {
        val updated = appointment.copy(status = AppointmentStatus.CHECKED_IN)
        coEvery { appointments.getAppointment(4) } returns Result.success(updated)

        viewModel.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals(AppointmentStatus.CHECKED_IN, state.appointment.status)
    }

    @Test
    fun `reschedule failure shows error message`() = runTest {
        coEvery {
            appointments.rescheduleAppointment(4, any())
        } returns Result.failure(RuntimeException("Slot taken"))

        viewModel.showRescheduleSheet()
        viewModel.rescheduleAppointment("2026-07-15T10:00:00+08:00")
        dispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertEquals("Slot taken", state.rescheduleError)
        assertFalse(state.isRescheduling)
        assertTrue(state.showRescheduleSheet)
    }

    @Test
    fun `missing appointmentId shows error state`() = runTest {
        val vm = AppointmentDetailViewModel(
            repository = appointments,
            reservationRepository = reservations,
            savedStateHandle = SavedStateHandle(emptyMap()),
        )
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as AppointmentDetailUiState.Error
        assertEquals("Missing appointment ID", state.message)
    }

    @Test
    fun `dismiss reschedule success dialog clears flag`() = runTest {
        val updated = appointment.copy(scheduledAt = "2026-07-15T10:00:00+08:00")
        coEvery { appointments.rescheduleAppointment(4, any()) } returns Result.success(updated)

        viewModel.rescheduleAppointment("2026-07-15T10:00:00+08:00")
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.dismissRescheduleSuccessDialog()

        val state = viewModel.uiState.value as AppointmentDetailUiState.Success
        assertFalse(state.showRescheduleSuccessDialog)
    }
}
