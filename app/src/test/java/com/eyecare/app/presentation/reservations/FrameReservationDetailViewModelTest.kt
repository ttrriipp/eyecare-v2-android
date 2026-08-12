package com.eyecare.app.presentation.reservations

import androidx.lifecycle.SavedStateHandle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.ReservationAppointment
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
    fun `delete success transitions to Deleted terminal state`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.deleteReservation(1) } returns Result.success(Unit)

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.deleteReservation()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is ReservationDetailUiState.Deleted)
    }

    @Test
    fun `delete failure keeps the reservation and surfaces an error`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.deleteReservation(1) } returns Result.failure(RuntimeException("Cannot delete"))

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.deleteReservation()
        advanceUntilIdle()

        val state = viewModel.uiState.value as ReservationDetailUiState.Success
        assertFalse(state.isCancelling)
        assertNotNull(state.cancelError)

        viewModel.dismissCancelError()
        assertEquals(null, (viewModel.uiState.value as ReservationDetailUiState.Success).cancelError)
    }

    @Test
    fun `delete succeeds for a held reservation`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(
            listOf(reservation(isHeld = true)),
        )
        coEvery { repository.deleteReservation(1) } returns Result.success(Unit)

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.deleteReservation()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteReservation(1) }
    }

    @Test
    fun `repeat delete taps issue a single request`() = runTest(dispatcher) {
        coEvery { repository.getReservations() } returns Result.success(listOf(reservation()))
        coEvery { repository.deleteReservation(1) } returns Result.success(Unit)

        val viewModel = vm()
        advanceUntilIdle()
        viewModel.deleteReservation()
        viewModel.deleteReservation()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.deleteReservation(1) }
    }
}
