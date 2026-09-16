package com.eyecare.app.presentation.appointments

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.repository.AppointmentRequestRepository
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyAppointmentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentRequestRepository

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
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load emits Loading then Content for none`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.success(CurrentAppointmentJourney.None)
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(MyAppointmentUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val error = awaitItem() as MyAppointmentUiState.Error
            assertTrue(error.message.contains("network error"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry after error reloads successfully`() = runTest {
        coEvery { repo.getCurrentAppointmentJourney() } returns Result.failure(RuntimeException("fail"))
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
        val vm = MyAppointmentViewModel(repo).also { it.load() }

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
}
