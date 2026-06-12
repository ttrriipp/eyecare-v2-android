package com.eyecare.app.presentation.appointments

import app.cash.turbine.test
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.repository.AppointmentRepository
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentRepository

    private val fakeList = listOf(
        Appointment(1, "eye_exam", AppointmentStatus.PENDING, "2026-10-24T10:00:00Z", null, null),
        Appointment(2, "follow_up", AppointmentStatus.CONFIRMED, "2026-10-25T14:00:00Z", null, null),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading then Success`() = runTest {
        coEvery { repo.getAppointments() } returns Result.success(fakeList)
        val vm = AppointmentListViewModel(repo)

        vm.uiState.test {
            assertInstanceOf(AppointmentListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertInstanceOf(AppointmentListUiState.Success::class.java, state)
            assertEquals(2, (state as AppointmentListUiState.Success).appointments.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error from repo emits Error state`() = runTest {
        coEvery { repo.getAppointments() } returns Result.failure(RuntimeException("network error"))
        val vm = AppointmentListViewModel(repo)

        vm.uiState.test {
            assertInstanceOf(AppointmentListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AppointmentListUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads appointments`() = runTest {
        coEvery { repo.getAppointments() } returns Result.success(fakeList)
        val vm = AppointmentListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading (init)
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (init result)

            vm.refresh()
            assertInstanceOf(AppointmentListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AppointmentListUiState.Success::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty list emits Empty state`() = runTest {
        coEvery { repo.getAppointments() } returns Result.success(emptyList())
        val vm = AppointmentListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AppointmentListUiState.Empty::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `success list is sorted by scheduledAt descending`() = runTest {
        coEvery { repo.getAppointments() } returns Result.success(fakeList)
        val vm = AppointmentListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as AppointmentListUiState.Success
            assertEquals(2, state.appointments[0].id) // later date first
            cancelAndIgnoreRemainingEvents()
        }
    }
}
