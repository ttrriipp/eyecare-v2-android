package com.eyecare.app.presentation.reservations

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameReservationRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFrameReservationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var reservationRepo: FrameReservationRepository
    private lateinit var appointmentRepo: AppointmentV1Repository

    private val scheduledFuture = AppointmentV1(
        id = 1, appointmentNumber = "APT-001", appointmentType = "New Patient",
        durationMinutes = 30, referringSource = null, status = AppointmentStatus.SCHEDULED,
        scheduledAt = "2030-08-01T10:00:00+08:00", contactNotes = null,
        lastRescheduleReason = null, source = "mobile", assignedOptometrist = null,
    )

    private val checkedIn = scheduledFuture.copy(id = 2, status = AppointmentStatus.CHECKED_IN)
    private val fulfilled = scheduledFuture.copy(id = 3, status = AppointmentStatus.FULFILLED)

    private fun createReservation() = FrameReservation(
        id = 1,
        appointment = ReservationAppointment(1, "APT-001", AppointmentStatus.SCHEDULED, "2030-08-01T10:00:00+08:00", 30),
        status = ReservationStatus.REQUESTED,
        expiresAt = null,
        createdAt = "2026-07-28T10:00:00+08:00",
        items = emptyList(),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        reservationRepo = mockk()
        appointmentRepo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(): CreateFrameReservationViewModel {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(scheduledFuture, checkedIn, fulfilled), 1, 1, 3)
        )
        return CreateFrameReservationViewModel(reservationRepo, appointmentRepo, 1, 42)
    }

    @Test
    fun `init loads and filters eligible appointments`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals(1, state.eligibleAppointments.size)
        assertEquals(1, state.eligibleAppointments[0].id)
        assertNull(state.selectedAppointmentId)
    }

    @Test
    fun `selectAppointment updates selection`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals(1, state.selectedAppointmentId)
    }

    @Test
    fun `submit without selection does nothing`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.uiState.test {
            awaitItem() // initial Ready
            vm.submit()
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit success emits Success`() = runTest {
        coEvery { reservationRepo.createReservation(any(), any()) } returns Result.success(createReservation())
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is CreateReservationUiState.Success)
    }

    @Test
    fun `appointment field error clears selection and reloads`() = runTest {
        coEvery { reservationRepo.createReservation(any(), any()) } returns Result.failure(
            FrameReservationError.ValidationError(mapOf("appointment_id" to listOf("Invalid appointment")))
        )
        var callCount = 0
        coEvery { appointmentRepo.getAppointments(any()) } answers {
            callCount++
            if (callCount <= 1) {
                Result.success(PaginatedResult(listOf(scheduledFuture), 1, 1, 1))
            } else {
                Result.success(PaginatedResult(listOf(scheduledFuture), 1, 1, 1))
            }
        }
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        // After appointment error, state reloads appointments
        val state = vm.uiState.value
        assertTrue(state is CreateReservationUiState.Ready)
        assertNull((state as CreateReservationUiState.Ready).selectedAppointmentId)
    }

    @Test
    fun `item field error retains selection`() = runTest {
        coEvery { reservationRepo.createReservation(any(), any()) } returns Result.failure(
            FrameReservationError.ValidationError(mapOf("items" to listOf("Invalid variant")))
        )
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals(1, state.selectedAppointmentId)
        assertEquals("Invalid variant", state.itemFieldError)
    }

    @Test
    fun `no eligible appointments shows empty state`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(checkedIn, fulfilled), 1, 1, 2)
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.NoEligibleAppointments)
    }
}
