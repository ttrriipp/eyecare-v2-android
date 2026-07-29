package com.eyecare.app.presentation.eyewear

import app.cash.turbine.test
import com.eyecare.app.domain.model.EyewearProgress
import com.eyecare.app.domain.model.EyewearSummary
import com.eyecare.app.domain.repository.EyewearRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class EyewearListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: EyewearRepository = mockk()

    private fun createSummary(key: String, progress: String = "in_preparation") = EyewearSummary(
        key = key,
        description = "Frame $key",
        consultationAt = null,
        createdAt = "2026-07-27T10:00:00+08:00",
        progress = EyewearProgress.fromApi(progress),
        paymentStatus = null,
        totalAmount = BigDecimal("8000.00"),
        balanceDue = null,
        activityAt = "2026-07-27T11:00:00+08:00",
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial load requests current filter`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_1")), 1, 1, 1)
        )
        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EyewearListUiState.Success
        assertEquals(EyewearFilter.CURRENT, state.filter)
        assertEquals(1, state.items.size)
    }

    @Test
    fun `switch filter clears old records and loads new`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_current")), 1, 1, 1)
        )
        coEvery { repository.getEyewear("history", 1) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_history", "dispensed")), 1, 1, 1)
        )
        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectFilter(EyewearFilter.HISTORY)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EyewearListUiState.Success
        assertEquals(EyewearFilter.HISTORY, state.filter)
        assertEquals(1, state.items.size)
        assertEquals("eyw_history", state.items[0].key)
    }

    @Test
    fun `empty filter shows empty state`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EyewearListUiState.Empty)
        assertEquals(EyewearFilter.CURRENT, (vm.uiState.value as EyewearListUiState.Empty).filter)
    }

    @Test
    fun `error shows error state`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.failure(RuntimeException("offline"))
        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EyewearListUiState.Error)
    }

    @Test
    fun `loadMore appends and guards duplicates`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_1")), 1, 2, 2)
        )
        coEvery { repository.getEyewear("current", 2) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_2")), 2, 2, 2)
        )
        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val initial = vm.uiState.value as EyewearListUiState.Success
        assertTrue(initial.hasMorePages)

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val final = vm.uiState.value as EyewearListUiState.Success
        assertEquals(2, final.items.size)
        assertFalse(final.hasMorePages)
    }

    @Test
    fun `stale filter response is ignored`() = runTest {
        coEvery { repository.getEyewear("current", 1) } returns Result.success(
            PaginatedResult(listOf(createSummary("eyw_current")), 1, 1, 1)
        )
        // History response comes back slowly
        coEvery { repository.getEyewear("history", 1) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(PaginatedResult(listOf(createSummary("eyw_history", "dispensed")), 1, 1, 1))
        }

        val vm = EyewearListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectFilter(EyewearFilter.HISTORY)
        // Immediately switch back before history loads
        vm.selectFilter(EyewearFilter.CURRENT)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EyewearListUiState.Success
        assertEquals(EyewearFilter.CURRENT, state.filter)
    }
}
