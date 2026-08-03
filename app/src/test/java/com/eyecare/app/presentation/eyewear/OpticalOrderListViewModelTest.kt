package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.repository.OpticalOrderRepository
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
class OpticalOrderListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: OpticalOrderRepository = mockk()

    private fun createOrder(id: Int = 1, status: OpticalOrderStatus = OpticalOrderStatus.IN_PROGRESS) = OpticalOrder(
        id = id,
        orderNumber = "OO-$id",
        status = status,
        fulfillmentMode = FulfillmentMode.PREPARED,
        totalAmount = BigDecimal("5000.00"),
        startedAt = null,
        readyAt = null,
        dispensedAt = null,
        cancelledAt = null,
        createdAt = "2026-08-01T10:00:00Z",
        sourceQuotation = null,
        items = listOf(OpticalOrderItem(10, "Lens", 1, BigDecimal("5000.00"), BigDecimal("5000.00"), null, false, null)),
        paymentSummary = null,
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial load requests current filter`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder()), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(OrderFilter.CURRENT, state.filter)
    }

    @Test
    fun `switch filter clears and loads new`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 1, 1)
        )
        coEvery { repository.getOpticalOrders("history", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(2, OpticalOrderStatus.DISPENSED)), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectFilter(OrderFilter.HISTORY)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(OrderFilter.HISTORY, state.filter)
    }

    @Test
    fun `empty filter shows empty state`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is OrderListUiState.Empty)
    }

    @Test
    fun `loadMore appends`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 2, 2)
        )
        coEvery { repository.getOpticalOrders("current", 2) } returns Result.success(
            PaginatedResult(listOf(createOrder(2)), 2, 2, 2)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(2, state.items.size)
        assertFalse(state.hasMorePages)
    }

    @Test
    fun `stale response is ignored`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 1, 1)
        )
        coEvery { repository.getOpticalOrders("history", 1) } coAnswers {
            kotlinx.coroutines.delay(1000)
            Result.success(PaginatedResult(listOf(createOrder(2)), 1, 1, 1))
        }

        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectFilter(OrderFilter.HISTORY)
        vm.selectFilter(OrderFilter.CURRENT)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(OrderFilter.CURRENT, state.filter)
    }
}
