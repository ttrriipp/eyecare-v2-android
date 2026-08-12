package com.eyecare.app.presentation.reservations

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.MAX_RESERVATION_ITEMS
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameReservationRepository
import com.eyecare.app.domain.repository.PaginatedResult
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CreateFrameReservationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var reservationRepo: FrameReservationRepository
    private lateinit var appointmentRepo: AppointmentV1Repository

    private val scheduledFuture = AppointmentV1(
        id = 1, appointmentNumber = "APT-001", appointmentType = "New Patient",
        durationMinutes = 30, referringSource = null, status = AppointmentStatus.SCHEDULED,
        scheduledAt = "2030-08-01T10:00:00+08:00", contactNotes = null,
        reasonForVisit = null,
        lastRescheduleReason = null, source = "mobile", assignedOptometrist = null,
    )

    private val checkedIn = scheduledFuture.copy(id = 2, status = AppointmentStatus.CHECKED_IN)
    private val fulfilled = scheduledFuture.copy(id = 3, status = AppointmentStatus.FULFILLED)

    private fun createReservation(
        isHeld: Boolean = false,
        items: List<FrameReservationItem> = emptyList(),
    ) = FrameReservation(
        id = 1,
        appointment = ReservationAppointment(1, "APT-001", AppointmentStatus.SCHEDULED, "2030-08-01T10:00:00+08:00", 30),
        isHeld = isHeld,
        status = if (isHeld) ReservationStatus.PREPARED else ReservationStatus.REQUESTED,
        expiresAt = null,
        createdAt = "2026-07-28T10:00:00+08:00",
        items = items,
    )

    private fun reservationItem(variantId: Int) = FrameReservationItem(
        id = variantId, productVariantId = variantId, variantName = "Variant $variantId", variantSku = "SKU-$variantId",
        price = BigDecimal.TEN, compareAtPrice = null, frameId = variantId, frameName = "Frame $variantId",
        frameBrand = "Brand", frameCategory = "Category", frameDescription = null, attributes = null, images = emptyList(),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        reservationRepo = mockk()
        appointmentRepo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(existingReservations: List<FrameReservation> = emptyList()): CreateFrameReservationViewModel {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(scheduledFuture, checkedIn, fulfilled), 1, 1, 3)
        )
        coEvery { reservationRepo.getReservations() } returns Result.success(existingReservations)
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
        coEvery { reservationRepo.getReservations() } returns Result.success(emptyList())
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

    // ── mergeOutcome (pure function) ──────────────────────────────────────

    @Test
    fun `mergeOutcome is None when no existing reservation`() {
        assertEquals(MergeOutcome.None, mergeOutcome(null, 42))
    }

    @Test
    fun `mergeOutcome is AlreadyReserved when the variant is already in the reservation`() {
        val existing = createReservation(items = listOf(reservationItem(42)))
        assertTrue(mergeOutcome(existing, 42) is MergeOutcome.AlreadyReserved)
    }

    @Test
    fun `mergeOutcome is Blocked when the reservation is held`() {
        val held = createReservation(isHeld = true)
        assertTrue(mergeOutcome(held, 42) is MergeOutcome.Blocked)
    }

    @Test
    fun `mergeOutcome is Full at the item cap`() {
        val full = createReservation(items = (1..MAX_RESERVATION_ITEMS).map { reservationItem(it) })
        assertTrue(mergeOutcome(full, 999) is MergeOutcome.Full)
    }

    @Test
    fun `mergeOutcome is Mergeable for an unheld reservation with room`() {
        val unheld = createReservation(isHeld = false, items = listOf(reservationItem(99)))
        assertTrue(mergeOutcome(unheld, 42) is MergeOutcome.Mergeable)
    }

    // ── submit(): merging into an existing reservation ────────────────────

    @Test
    fun `submit on a mergeable appointment cancels then recreates with combined items`() = runTest {
        val existing = createReservation(isHeld = false, items = listOf(reservationItem(99)))
        coEvery { reservationRepo.cancelReservation(1) } returns Result.success(
            createReservation(items = listOf(reservationItem(99))),
        )
        coEvery { reservationRepo.createReservation(listOf(99, 42), 1) } returns Result.success(
            createReservation(items = listOf(reservationItem(99), reservationItem(42))),
        )
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.Success)
        coVerify(exactly = 1) { reservationRepo.cancelReservation(1) }
        coVerify(exactly = 1) { reservationRepo.createReservation(listOf(99, 42), 1) }
    }

    @Test
    fun `submit skips held reservations and creates a new one`() = runTest {
        val existing = createReservation(isHeld = true, items = listOf(reservationItem(99)))
        coEvery { reservationRepo.createReservation(listOf(42), 1) } returns Result.success(
            createReservation(items = listOf(reservationItem(42))),
        )
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.Success)
        coVerify(exactly = 0) { reservationRepo.cancelReservation(any()) }
        coVerify(exactly = 1) { reservationRepo.createReservation(listOf(42), 1) }
    }

    @Test
    fun `submit for an already-reserved frame shows an item error without calling the repository`() = runTest {
        val existing = createReservation(isHeld = false, items = listOf(reservationItem(42)))
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals("This frame is already part of your reservation for this appointment.", state.itemFieldError)
        coVerify(exactly = 0) { reservationRepo.cancelReservation(any()) }
        coVerify(exactly = 0) { reservationRepo.createReservation(any(), any()) }
    }
}
