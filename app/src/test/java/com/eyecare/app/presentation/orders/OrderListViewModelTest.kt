package com.eyecare.app.presentation.orders

import app.cash.turbine.test
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.repository.OrderRepository
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
class OrderListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: OrderRepository

    private fun makeOrder(id: Int, status: OrderStatus) = Order(
        id, "ORD-00$id", null, null, false, status, "165.00", "165.00", emptyList(), "2026-10-24T10:00:00Z"
    )

    private val orders = listOf(
        makeOrder(1, OrderStatus.REQUESTED),
        makeOrder(2, OrderStatus.CONFIRMED),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial load emits Loading then Success`() = runTest {
        coEvery { repo.getOrders() } returns Result.success(orders)
        val vm = OrderListViewModel(repo)

        vm.uiState.test {
            assertInstanceOf(OrderListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as OrderListUiState.Success
            assertEquals(2, state.orders.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty list emits Empty`() = runTest {
        coEvery { repo.getOrders() } returns Result.success(emptyList())
        val vm = OrderListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(OrderListUiState.Empty::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error emits Error`() = runTest {
        coEvery { repo.getOrders() } returns Result.failure(RuntimeException("error"))
        val vm = OrderListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(OrderListUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `detail loads order by id`() = runTest {
        coEvery { repo.getOrder(1) } returns Result.success(orders[0])
        val vm = OrderDetailViewModel(repo, 1)

        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value as OrderDetailUiState.Success
        assertEquals("ORD-001", state.order.orderNumber)
    }
}
