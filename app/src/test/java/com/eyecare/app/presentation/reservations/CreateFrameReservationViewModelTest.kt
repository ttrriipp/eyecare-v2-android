package com.eyecare.app.presentation.reservations

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.MAX_RESERVATION_ITEMS
import com.eyecare.app.domain.model.ReservationAppointment
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.repository.AppointmentRequestRepository
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
    private lateinit var appointmentRequestRepo: AppointmentRequestRepository

    private fun fakeRequest(
        id: Int,
        status: AppointmentRequestStatus,
        scheduledAt: String = "2030-08-01T10:00:00+08:00",
        createdAt: String = "2026-07-01T10:00:00+08:00",
    ) = AppointmentRequest(
        id = id,
        requestNumber = "APR-${id.toString().padStart(6, '0')}",
        status = status,
        patientId = null,
        appointmentType = null,
        scheduledAt = scheduledAt,
        alternativeScheduledTimes = emptyList(),
        provisionalDurationMinutes = null,
        reasonForVisit = "Checkup",
        referringSource = null,
        timePreferencesAreReserved = false,
        expiresAt = null,
        cancelledAt = null,
        rejectionReason = null,
        createdAt = createdAt,
        appointmentId = null,
    )

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
        appointmentRequestRepo = mockk()
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm(existingReservations: List<FrameReservation> = emptyList()): CreateFrameReservationViewModel {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(scheduledFuture, checkedIn, fulfilled), 1, 1, 3)
        )
        coEvery { reservationRepo.getReservations() } returns Result.success(existingReservations)
        return CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
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
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
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
    fun `no eligible appointments and no requests shows generic empty state`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(checkedIn, fulfilled), 1, 1, 2)
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.NoEligibleAppointments)
    }

    @Test
    fun `no eligible appointments with a pending request shows RequestPending`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(listOf(checkedIn, fulfilled), 1, 1, 2)
        )
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(7, AppointmentRequestStatus.PENDING)), 1, 1, 1)
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.RequestPending
        assertEquals(7, state.primaryRequestId)
        assertEquals(1, state.pendingCount)
    }

    @Test
    fun `two pending requests report pendingCount 2 so booking another is hidden`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(
                listOf(
                    fakeRequest(1, AppointmentRequestStatus.PENDING, scheduledAt = "2030-08-05T10:00:00+08:00"),
                    fakeRequest(2, AppointmentRequestStatus.PENDING, scheduledAt = "2030-08-01T10:00:00+08:00"),
                ),
                1, 1, 2,
            )
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.RequestPending
        assertEquals(2, state.pendingCount)
        // The soonest-scheduled pending request is the one linked, not just the first in list order.
        assertEquals(2, state.primaryRequestId)
    }

    @Test
    fun `no eligible appointments with only a rejected request shows PriorRequestStatus`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(listOf(fakeRequest(3, AppointmentRequestStatus.REJECTED)), 1, 1, 1)
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.PriorRequestStatus
        assertEquals(AppointmentRequestStatus.REJECTED, state.status)
    }

    @Test
    fun `a pending request takes priority over an older rejected request`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.success(
            PaginatedResult(
                listOf(
                    fakeRequest(4, AppointmentRequestStatus.REJECTED),
                    fakeRequest(5, AppointmentRequestStatus.PENDING),
                ),
                1, 1, 2,
            )
        )
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.RequestPending)
    }

    @Test
    fun `request lookup failure falls back to the generic empty state`() = runTest {
        coEvery { appointmentRepo.getAppointments(any()) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        coEvery { appointmentRequestRepo.getRequests(1, 15) } returns Result.failure(RuntimeException("offline"))
        val vm = CreateFrameReservationViewModel(reservationRepo, appointmentRepo, appointmentRequestRepo, 1, 42)
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
    fun `mergeOutcome allows room only below the coordinated maximum for counts zero through five`() {
        for (count in 0..5) {
            val reservation = createReservation(
                items = (1..count).map { reservationItem(it) },
            )

            val outcome = mergeOutcome(reservation, 999)
            if (count < 3) {
                assertTrue(outcome is MergeOutcome.Mergeable, "count=$count should still accept an item")
            } else {
                assertTrue(outcome is MergeOutcome.Full, "count=$count should reject another item")
            }
        }
    }

    @Test
    fun `legacy four and five item reservations remain intact and cannot accept another item`() {
        for (count in 4..5) {
            val items = (1..count).map { reservationItem(it) }
            val reservation = createReservation(items = items)

            assertEquals(count, reservation.items.size)
            assertEquals(items, reservation.items)
            assertTrue(mergeOutcome(reservation, 999) is MergeOutcome.Full)
        }
    }

    @Test
    fun `mergeOutcome is Mergeable for an unheld reservation with room`() {
        val unheld = createReservation(isHeld = false, items = listOf(reservationItem(99)))
        assertTrue(mergeOutcome(unheld, 42) is MergeOutcome.Mergeable)
    }

    // ── submit(): merging into an existing reservation ────────────────────

    @Test
    fun `submit on a mergeable appointment issues one addItem call and zero deletes`() = runTest {
        val existing = createReservation(isHeld = false, items = listOf(reservationItem(99)))
        coEvery { reservationRepo.addItem(1, 42) } returns Result.success(
            createReservation(items = listOf(reservationItem(99), reservationItem(42))),
        )
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is CreateReservationUiState.Success)
        // Permanent guard: merge must never delete-then-recreate. One add-item call is the
        // correct behavior — the old cancel-then-recreate could destroy a patient's hold
        // if the recreate failed.
        coVerify(exactly = 0) { reservationRepo.deleteReservation(any()) }
        coVerify(exactly = 1) { reservationRepo.addItem(1, 42) }
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
        coVerify(exactly = 0) { reservationRepo.deleteReservation(any()) }
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
        coVerify(exactly = 0) { reservationRepo.deleteReservation(any()) }
        coVerify(exactly = 0) { reservationRepo.createReservation(any(), any()) }
    }

    @Test
    fun `submit at the new three item cap shows a derived error without calling the repository`() = runTest {
        val existing = createReservation(
            isHeld = false,
            items = (1..3).map { reservationItem(it) },
        )
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals("This reservation already has the maximum of $MAX_RESERVATION_ITEMS frames.", state.itemFieldError)
        coVerify(exactly = 0) { reservationRepo.addItem(any(), any()) }
        coVerify(exactly = 0) { reservationRepo.createReservation(any(), any()) }
    }

    @Test
    fun `server capacity validation is surfaced while retaining the selected appointment`() = runTest {
        val existing = createReservation(
            isHeld = false,
            items = listOf(reservationItem(99), reservationItem(100)),
        )
        coEvery { reservationRepo.addItem(1, 42) } returns Result.failure(
            FrameReservationError.ValidationError(
                mapOf("items" to listOf("A reservation may contain no more than 3 frames.")),
            ),
        )
        val vm = vm(existingReservations = listOf(existing))
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectAppointment(1)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as CreateReservationUiState.Ready
        assertEquals(1, state.selectedAppointmentId)
        assertEquals("A reservation may contain no more than 3 frames.", state.itemFieldError)
        coVerify(exactly = 1) { reservationRepo.addItem(1, 42) }
    }
}
