package com.eyecare.app.presentation.appointments

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.AppointmentV1Repository
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MyAppointmentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository
    private lateinit var appointmentRepo: AppointmentV1Repository

    private val fakeRequest = AppointmentRequest(
        id = 5,
        requestNumber = "APR-2026-000005",
        status = AppointmentRequestStatus.PENDING,
        requestType = AppointmentRequestType.NEW,
        patientId = null,
        appointmentType = null,
        scheduledAt = "2026-09-20T10:00:00+08:00",
        alternativeScheduledTimes = listOf("2026-09-20T14:00:00+08:00"),
        provisionalDurationMinutes = null,
        reasonForVisit = "Checkup",
        referringSource = null,
        timePreferencesAreReserved = false,
        expiresAt = null,
        cancelledAt = null,
        rejectionReason = null,
        createdAt = "2026-09-16T08:00:00+08:00",
        appointmentId = null,
    )

    private val fakeAppointment = AppointmentV1(
        id = 42,
        appointmentNumber = "APT-2026-000042",
        appointmentType = "First eye examination",
        durationMinutes = 45,
        referringSource = null,
        status = AppointmentStatus.SCHEDULED,
        scheduledAt = "2026-09-25T09:00:00+08:00",
        contactNotes = null,
        reasonForVisit = "Annual checkup",
        lastRescheduleReason = null,
        source = "mobile",
        assignedOptometrist = null,
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        appointmentRepo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load emits Loading then Content for none`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val content = awaitItem() as MyAppointmentUiState.Content
            assertInstanceOf(CurrentAppointmentJourney.None::class.java, content.journey)
            assertFalse(content.isRefreshing)
            assertNull(content.refreshError)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial load emits Content for pending request`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val content = awaitItem() as MyAppointmentUiState.Content
            val pending = content.journey as CurrentAppointmentJourney.PendingRequest
            assertEquals(5, pending.request.id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial load emits Content for confirmed appointment`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.Appointment(
                appointment = fakeAppointment,
                originalRequest = null,
                pendingReschedule = null,
            ),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val content = awaitItem() as MyAppointmentUiState.Content
            val appt = content.journey as CurrentAppointmentJourney.Appointment
            assertEquals(42, appt.appointment.id)
            assertNull(appt.originalRequest)
            assertNull(appt.pendingReschedule)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial load error emits Error state`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.failure(RuntimeException("network error"))
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as MyAppointmentUiState.Error
            assertEquals("Something went wrong. Please try again.", error.message)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after error reloads successfully`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.failure(RuntimeException("fail"))
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(MyAppointmentUiState.Error::class.java, awaitItem())

            coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
            vm.retry()
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(MyAppointmentUiState.Content::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh preserves content while loading`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // initial content

            coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
                CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
            )
            vm.refresh()
            dispatcher.scheduler.advanceUntilIdle()
            val refreshing = awaitItem() as MyAppointmentUiState.Content
            assertTrue(refreshing.isRefreshing)
            assertInstanceOf(CurrentAppointmentJourney.None::class.java, refreshing.journey)

            dispatcher.scheduler.advanceUntilIdle()
            val updated = awaitItem() as MyAppointmentUiState.Content
            assertFalse(updated.isRefreshing)
            assertInstanceOf(CurrentAppointmentJourney.PendingRequest::class.java, updated.journey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh failure retains content and sets refresh error`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // initial content

            coEvery { repo.getCurrentAppointmentJourney() } returns Result.failure(RuntimeException("timeout"))
            vm.refresh()
            dispatcher.scheduler.advanceUntilIdle()
            val refreshing = awaitItem() as MyAppointmentUiState.Content
            assertTrue(refreshing.isRefreshing)

            dispatcher.scheduler.advanceUntilIdle()
            val failed = awaitItem() as MyAppointmentUiState.Content
            assertFalse(failed.isRefreshing)
            assertNotNull(failed.refreshError)
            assertInstanceOf(CurrentAppointmentJourney.None::class.java, failed.journey)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `rapid refresh calls do not produce stale overwrite`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        dispatcher.scheduler.advanceUntilIdle()

        // Bump generation twice before any response arrives
        vm.refresh()
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val final = vm.uiState.value as MyAppointmentUiState.Content
        assertFalse(final.isRefreshing)
        assertNull(final.refreshError)
    }

    @Test
    fun `appointment with pending reschedule loads correctly`() = runTest {
        val reschedule = fakeRequest.copy(id = 7, requestType = AppointmentRequestType.RESCHEDULE)
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.Appointment(
                appointment = fakeAppointment,
                originalRequest = fakeRequest.copy(status = AppointmentRequestStatus.ACCEPTED),
                pendingReschedule = reschedule,
            ),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val content = awaitItem() as MyAppointmentUiState.Content
            val appt = content.journey as CurrentAppointmentJourney.Appointment
            assertEquals(42, appt.appointment.id)
            assertEquals(5, appt.originalRequest?.id)
            assertEquals(7, appt.pendingReschedule?.id)
            assertEquals(AppointmentRequestType.RESCHEDULE, appt.pendingReschedule?.requestType)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `cancel request refetches current journey on success`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
        )
        coEvery { repo.cancelRequest(5, "Changed my mind") } returns Result.success(fakeRequest)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelRequest(5, "Changed my mind")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertEquals("Request cancelled.", content.mutationSuccess)
        coVerify(exactly = 2) { repo.getCurrentAppointmentJourney() }
    }

    @Test
    fun `cancel request shows error on failure`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
        )
        coEvery { repo.cancelRequest(5, any()) } returns Result.failure(RuntimeException("server error"))
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelRequest(5, "Changed my mind")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertNotNull(content.mutationError)
    }

    @Test
    fun `cancel appointment refetches current journey on success`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.Appointment(appointment = fakeAppointment, originalRequest = null, pendingReschedule = null),
        )
        coEvery { appointmentRepo.cancelAppointment(42, "No longer needed") } returns Result.success(fakeAppointment)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelAppointment("No longer needed")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertEquals("Appointment cancelled.", content.mutationSuccess)
        coVerify(exactly = 2) { repo.getCurrentAppointmentJourney() }
    }

    @Test
    fun `mutation refresh failure retains the confirmed appointment`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returnsMany listOf(
            Result.success(
                CurrentAppointmentJourney.Appointment(
                    appointment = fakeAppointment,
                    originalRequest = null,
                    pendingReschedule = null,
                ),
            ),
            Result.failure(RuntimeException("refresh failed")),
        )
        coEvery { appointmentRepo.cancelAppointment(42, "No longer needed") } returns Result.success(fakeAppointment)
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelAppointment("No longer needed")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertFalse(content.isMutating)
        assertNotNull(content.mutationError)
        assertInstanceOf(CurrentAppointmentJourney.Appointment::class.java, content.journey)
        assertEquals(
            42,
            (content.journey as CurrentAppointmentJourney.Appointment).appointment.id,
        )
    }

    @Test
    fun `blank cancel reason shows validation error`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelRequest(5, "   ")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertNotNull(content.mutationError)
    }

    @Test
    fun `same day pending request shows cancellation guidance without calling API`() = runTest {
        val sameDayRequest = fakeRequest.copy(
            scheduledAt = "${LocalDate.now(CLINIC_TIME_ZONE)}T10:00:00+08:00",
        )
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = sameDayRequest),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelRequest(sameDayRequest.id, "Changed my mind")
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertEquals(SAME_DAY_CANCELLATION_MESSAGE, content.mutationError)
        coVerify(exactly = 0) { repo.cancelRequest(any(), any()) }
    }

    @Test
    fun `overlong cancellation reason is rejected before calling API`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(
            CurrentAppointmentJourney.PendingRequest(request = fakeRequest),
        )
        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.cancelRequest(fakeRequest.id, "x".repeat(PATIENT_CANCELLATION_REASON_MAX_LENGTH + 1))
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        assertEquals(CANCELLATION_REASON_REQUIRED_MESSAGE, content.mutationError)
        coVerify(exactly = 0) { repo.cancelRequest(any(), any()) }
    }

    @Test
    fun `requesting a different time refetches current journey`() = runTest {
        val requestedAt = "${LocalDate.now(CLINIC_TIME_ZONE).plusDays(2)}T10:30:00+08:00"
        val pendingReschedule = fakeRequest.copy(
            id = 7,
            requestType = AppointmentRequestType.RESCHEDULE,
            appointmentId = fakeAppointment.id,
            scheduledAt = requestedAt,
        )
        coEvery { repo.getCurrentAppointmentJourney() } returnsMany listOf(
            Result.success(
                CurrentAppointmentJourney.Appointment(
                    appointment = fakeAppointment,
                    originalRequest = null,
                    pendingReschedule = null,
                ),
            ),
            Result.success(
                CurrentAppointmentJourney.Appointment(
                    appointment = fakeAppointment,
                    originalRequest = null,
                    pendingReschedule = pendingReschedule,
                ),
            ),
        )
        coEvery {
            appointmentRepo.getAppointmentAvailability(any(), fakeAppointment.id)
        } returns Result.success(
            AppointmentAvailability(
                date = requestedAt.take(10),
                timezone = "Asia/Manila",
                intervalMinutes = 15,
                visitReasonId = 1,
                visitDurationMinutes = fakeAppointment.durationMinutes,
                optometristId = null,
                appointmentId = fakeAppointment.id,
                dayStatus = "open",
                generatedAt = requestedAt,
                slots = listOf(
                    AppointmentSlot(
                        startsAt = requestedAt,
                        endsAt = "${requestedAt.take(10)}T11:15:00+08:00",
                        available = true,
                        reason = null,
                    ),
                ),
            ),
        )
        coEvery {
            repo.createRebookingRequest(fakeAppointment.id, requestedAt, null, null)
        } returns Result.success(pendingReschedule)

        val vm = MyAppointmentViewModel(repo, appointmentRepo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        vm.showRescheduleSheet()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue((vm.uiState.value as MyAppointmentUiState.Content).showRescheduleSheet)

        vm.rescheduleAppointment(requestedAt)
        dispatcher.scheduler.advanceUntilIdle()

        val content = vm.uiState.value as MyAppointmentUiState.Content
        val journey = content.journey as CurrentAppointmentJourney.Appointment
        assertFalse(content.isMutating)
        assertEquals("Time-change request sent.", content.mutationSuccess)
        assertEquals(fakeAppointment.scheduledAt, journey.appointment.scheduledAt)
        assertEquals(pendingReschedule.id, journey.pendingReschedule?.id)
        coVerify(exactly = 2) { repo.getCurrentAppointmentJourney() }
    }
}
