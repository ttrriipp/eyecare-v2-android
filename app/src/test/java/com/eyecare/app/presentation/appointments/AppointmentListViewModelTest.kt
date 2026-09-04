package com.eyecare.app.presentation.appointments

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppointmentListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: AppointmentV1Repository

    private val fakeList = listOf(
        AppointmentV1(1, "APT-001", "New Patient", 30, null, AppointmentStatus.SCHEDULED, "2026-10-24T10:00:00+08:00", null, null, null, "mobile", null),
        AppointmentV1(2, "APT-002", "Follow-up", 15, null, AppointmentStatus.SCHEDULED, "2026-10-25T14:00:00+08:00", null, null, null, "mobile", null),
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
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(AppointmentListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertInstanceOf(AppointmentListUiState.Success::class.java, state)
            assertEquals(2, (state as AppointmentListUiState.Success).appointments.size)
            assertFalse(state.hasMorePages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error from repo emits Error state`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.failure(RuntimeException("network error"))
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            assertInstanceOf(AppointmentListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AppointmentListUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh reloads appointments without resetting to Loading first`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            awaitItem() // Loading (init)
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (init result)

            vm.refresh()
            // Stays a Success instance with isRefreshing=true - items remain visible
            // underneath a pull-to-refresh indicator instead of a bare Loading spinner.
            val refreshing = awaitItem() as AppointmentListUiState.Success
            assertTrue(refreshing.isRefreshing)
            dispatcher.scheduler.advanceUntilIdle()
            val refreshed = awaitItem() as AppointmentListUiState.Success
            assertFalse(refreshed.isRefreshing)
            assertEquals(2, refreshed.appointments.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching accounts clears retained appointments before replacement loads`() = runTest {
        val replacementAppointment = fakeList.first().copy(
            id = 99,
            appointmentNumber = "APT-099",
        )
        coEvery { repo.getAppointments(1) } returnsMany listOf(
            Result.success(PaginatedResult(fakeList, 1, 1, 2)),
            Result.success(PaginatedResult(listOf(replacementAppointment), 1, 1, 1)),
        )
        val vm = AppointmentListViewModel(repo)

        vm.load(accountId = 101)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            fakeList.map { it.id }.sortedDescending(),
            (vm.uiState.value as AppointmentListUiState.Success).appointments.map { it.id },
        )

        vm.refresh(hasActivePatientLink = true, accountId = 202)

        assertInstanceOf(AppointmentListUiState.Loading::class.java, vm.uiState.value)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(
            listOf(99),
            (vm.uiState.value as AppointmentListUiState.Success).appointments.map { it.id },
        )
    }

    @Test
    fun `failed refresh keeps existing appointments visible instead of discarding them`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { repo.getAppointments(1) } returns Result.failure(RuntimeException("offline"))
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as AppointmentListUiState.Success
        assertEquals(2, state.appointments.size)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `empty list emits Empty state`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(AppointmentListUiState.Empty::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `success list is sorted by scheduledAt descending`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as AppointmentListUiState.Success
            assertEquals(2, state.appointments[0].id) // later date first
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `paginated result exposes hasMorePages`() = runTest {
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 2, 30),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as AppointmentListUiState.Success
            assertTrue(state.hasMorePages)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `loadMore appends next page`() = runTest {
        val page1 = listOf(fakeList[0])
        val page2 = listOf(fakeList[1])
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(page1, 1, 2, 2),
        )
        coEvery { repo.getAppointments(2) } returns Result.success(
            PaginatedResult(page2, 2, 2, 2),
        )
        val vm = AppointmentListViewModel(repo).also { it.load() }

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            val initial = awaitItem() as AppointmentListUiState.Success
            assertEquals(1, initial.appointments.size)
            assertTrue(initial.hasMorePages)

            vm.loadMore()
            val loadingMore = awaitItem() as AppointmentListUiState.Success
            assertTrue(loadingMore.isLoadingMore)
            dispatcher.scheduler.advanceUntilIdle()
            val final = awaitItem() as AppointmentListUiState.Success
            assertEquals(2, final.appointments.size)
            assertFalse(final.hasMorePages)
            assertFalse(final.isLoadingMore)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `limited load keeps appointment shell available without fetching confirmed appointments`() = runTest {
        val vm = AppointmentListViewModel(repo)

        vm.load(hasActivePatientLink = false)

        val state = vm.uiState.value as AppointmentListUiState.Success
        assertTrue(state.appointments.isEmpty())
        assertFalse(state.hasMorePages)
        coVerify(exactly = 0) { repo.getAppointments(any()) }
    }
}
