package com.eyecare.app.presentation.reservations

import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.repository.FrameReservationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class FrameReservationDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FrameReservationRepository

    private fun item(id: Int = 1, price: String = "4500.00") = FrameReservationItem(
        id = id,
        productVariantId = 42,
        variantName = "Black / 52mm",
        variantSku = "RB-CR-BLK-52",
        price = BigDecimal(price),
        compareAtPrice = null,
        frameId = 7,
        frameName = "Classic Rectangle",
        frameBrand = "Ray-Ban",
        frameCategory = "Full Rim",
        frameDescription = "Timeless frame design",
        attributes = mapOf("color" to "black"),
        images = listOf("frames/classic.jpg"),
    )

    private fun reservation(
        id: Int = 1,
        isHeld: Boolean = false,
        items: List<FrameReservationItem> = listOf(item()),
    ) = FrameReservation(
        id = id,
        appointment = ReservationAppointment(
            id = 42,
            appointmentNumber = "APT-2026-000042",
            status = AppointmentStatus.SCHEDULED,
            scheduledAt = "2026-08-30T09:00:00+08:00",
            durationMinutes = 30,
        ),
        isHeld = isHeld,
        status = if (isHeld) ReservationStatus.PREPARED else ReservationStatus.REQUESTED,
        expiresAt = null,
        createdAt = "2026-07-27T10:00:00+08:00",
        items = items,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(reservationId: Int = 1) = FrameReservationDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("reservationId" to reservationId)),
    )

    @Test
    fun `resolves the requested reservation from the patient list`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(
            listOf(reservation(id = 9), reservation(id = 1)),
        )

        val viewModel = vm(reservationId = 1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is ReservationDetailUiState.Success)
        assertEquals(1, (state as ReservationDetailUiState.Success).reservation.id)
    }

    @Test
    fun `unknown reservation id renders not found instead of another record`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation(id = 9)))

        val viewModel = vm(reservationId = 1)
        advanceUntilIdle()

        assertEquals(ReservationDetailUiState.NotFound, viewModel.uiState.value)
    }

    @Test
    fun `load failure is retryable`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.failure(RuntimeException("network down"))

        val viewModel = vm()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is ReservationDetailUiState.Error)

        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        viewModel.retry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ReservationDetailUiState.Success)
    }

    @Test
    fun `cancel replaces the reservation with the returned record`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.cancelReservation(1) } returns Result.success(
            reservation(isHeld = false),
        )

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.cancelReservation()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReservationDetailUiState.Success
        assertFalse(state.isCancelling)
    }

    @Test
    fun `cancel failure keeps the reservation and surfaces an error`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.cancelReservation(1) } returns Result.failure(RuntimeException("Cannot cancel"))

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.cancelReservation()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReservationDetailUiState.Success
        assertFalse(state.reservation.isHeld)
        assertFalse(state.isCancelling)
        assertNotNull(state.cancelError)

        viewModel.dismissCancelError()
        assertEquals(null, (viewModel.uiState.value as ReservationDetailUiState.Success).cancelError)
    }

    @Test
    fun `cancel succeeds for a held reservation`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(
            listOf(reservation(isHeld = true)),
        )
        coEvery { repository.cancelReservation(1) } returns Result.success(
            reservation(isHeld = false),
        )

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.cancelReservation()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelReservation(1) }
    }

    @Test
    fun `repeat cancel taps issue a single request`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.cancelReservation(1) } returns Result.success(
            reservation(isHeld = false),
        )

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.cancelReservation()
        viewModel.cancelReservation()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.cancelReservation(1) }
    }
}
