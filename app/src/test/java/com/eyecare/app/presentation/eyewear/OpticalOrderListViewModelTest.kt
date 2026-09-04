package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.repository.OpticalOrderRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.coVerify
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
        items = listOf(OpticalOrderItem(10, "Lens", 1, BigDecimal("5000.00"), BigDecimal("5000.00"), null, false, null)),
        paymentSummary = null,
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial load fetches current orders by default`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder()), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(1, state.items.size)
        assertEquals(OrderListFilter.CURRENT, state.selectedFilter)
    }

    @Test
    fun `empty list shows empty state`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Empty
        assertEquals(OrderListFilter.CURRENT, state.selectedFilter)
    }

    @Test
    fun `selecting history reloads only history orders`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 1, 1)
        )
        coEvery { repository.getOpticalOrders("history", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(2, OpticalOrderStatus.DISPENSED)), 1, 1, 1)
        )

        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectFilter(OrderListFilter.HISTORY)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(OrderListFilter.HISTORY, state.selectedFilter)
        assertEquals(listOf(2), state.items.map { it.id })
    }

    @Test
    fun `refresh replaces items without resetting to Loading first`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(2)), 1, 1, 1)
        )
        vm.refresh()
        // With an unconfined dispatcher the coroutine body runs immediately, but the assignment
        // to isRefreshing = true still happens synchronously before the network call resolves.
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(2, state.items.first().id)
        assertFalse(state.isRefreshing)
    }

    @Test
    fun `failed refresh keeps existing items visible instead of discarding them`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getOpticalOrders("current", 1) } returns Result.failure(RuntimeException("offline"))
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(1, state.items.size)
        assertEquals(1, state.items.first().id)
        assertFalse(state.isRefreshing)
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
    fun `failed loadMore retries the same page without skipping orders`() = runTest {
        coEvery { repository.getOpticalOrders("current", 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 2, 2)
        )
        coEvery { repository.getOpticalOrders("current", 2) } returnsMany listOf(
            Result.failure(RuntimeException("offline")),
            Result.success(PaginatedResult(listOf(createOrder(2)), 2, 2, 2)),
        )

        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue((vm.uiState.value as OrderListUiState.Success).loadMoreError != null)

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(listOf(1, 2), state.items.map { it.id })
        coVerify(exactly = 2) { repository.getOpticalOrders("current", 2) }
    }
}
