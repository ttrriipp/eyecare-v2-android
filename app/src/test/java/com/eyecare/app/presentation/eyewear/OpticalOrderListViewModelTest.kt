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
    fun `initial load fetches all orders`() = runTest {
        coEvery { repository.getOpticalOrders(null, 1) } returns Result.success(
            PaginatedResult(listOf(createOrder()), 1, 1, 1)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderListUiState.Success
        assertEquals(1, state.items.size)
    }

    @Test
    fun `empty list shows empty state`() = runTest {
        coEvery { repository.getOpticalOrders(null, 1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        val vm = OpticalOrderListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is OrderListUiState.Empty)
    }

    @Test
    fun `loadMore appends`() = runTest {
        coEvery { repository.getOpticalOrders(null, 1) } returns Result.success(
            PaginatedResult(listOf(createOrder(1)), 1, 2, 2)
        )
        coEvery { repository.getOpticalOrders(null, 2) } returns Result.success(
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
}
