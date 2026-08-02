package com.eyecare.app.presentation.appointments

import app.cash.turbine.test
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
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
        val vm = AppointmentListViewModel(repo)

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
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
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
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0),
        )
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
        coEvery { repo.getAppointments(1) } returns Result.success(
            PaginatedResult(fakeList, 1, 1, 2),
        )
        val vm = AppointmentListViewModel(repo)

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
        val vm = AppointmentListViewModel(repo)

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
        val vm = AppointmentListViewModel(repo)

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
}
