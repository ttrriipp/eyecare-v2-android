package com.eyecare.app.presentation.accessories

import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import com.eyecare.app.domain.repository.PaginatedResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class AccessoryOrderRequestListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: AccessoryOrderRequestRepository

    private fun request(id: Int, status: OrderRequestStatus = OrderRequestStatus.PENDING) = AccessoryOrderRequest(
        id = id,
        requestNumber = "ORQ-$id",
        status = status,
        subtotalAmount = BigDecimal("100.00"),
        requestedDiscountType = DiscountType.NONE,
        resolvedBy = null,
        resolvedAt = null,
        items = emptyList(),
        rejectionReason = null,
        cancelledAt = null,
        createdAt = "2026-09-20T10:00:00+08:00",
        order = null,
    )

    private fun paginatedResult(items: List<AccessoryOrderRequest>, currentPage: Int = 1, lastPage: Int = 1) =
        PaginatedResult(data = items, currentPage = currentPage, lastPage = lastPage, total = items.size)

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial load shows loading then success`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(listOf(request(1)))
        )
        val viewModel = AccessoryOrderRequestListViewModel(repository)

        assertTrue(viewModel.uiState.value is RequestListUiState.Loading)
        advanceUntilIdle()
        val state = viewModel.uiState.value as RequestListUiState.Success
        assertEquals(1, state.items.size)
    }

    @Test
    fun `initial load shows empty when no results`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(emptyList())
        )
        val viewModel = AccessoryOrderRequestListViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RequestListUiState.Empty)
    }

    @Test
    fun `selecting history sends history filter`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(listOf(request(1)))
        )
        coEvery { repository.getRequests(OrderRequestFilter.HISTORY, any()) } returns Result.success(
            paginatedResult(listOf(request(2, OrderRequestStatus.ACCEPTED)))
        )
        val viewModel = AccessoryOrderRequestListViewModel(repository)
        advanceUntilIdle()

        viewModel.selectFilter(OrderRequestFilter.HISTORY)
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestListUiState.Success
        assertEquals(2, state.items[0].id)
        assertEquals(OrderRequestFilter.HISTORY, state.selectedFilter)
    }

    @Test
    fun `refresh replaces items`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(listOf(request(1)))
        )
        val viewModel = AccessoryOrderRequestListViewModel(repository)
        advanceUntilIdle()

        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(listOf(request(2), request(3)))
        )
        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestListUiState.Success
        assertEquals(2, state.items.size)
    }

    @Test
    fun `loadMore appends items`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, 1) } returns Result.success(
            paginatedResult(listOf(request(1)), lastPage = 2)
        )
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, 2) } returns Result.success(
            paginatedResult(listOf(request(2)), currentPage = 2, lastPage = 2)
        )
        val viewModel = AccessoryOrderRequestListViewModel(repository)
        advanceUntilIdle()

        viewModel.loadMore()
        advanceUntilIdle()

        val state = viewModel.uiState.value as RequestListUiState.Success
        assertEquals(2, state.items.size)
        assertFalse(state.hasMorePages)
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.failure(RuntimeException("fail"))
        val viewModel = AccessoryOrderRequestListViewModel(repository)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RequestListUiState.Error)

        coEvery { repository.getRequests(OrderRequestFilter.CURRENT, any()) } returns Result.success(
            paginatedResult(listOf(request(1)))
        )
        viewModel.retry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is RequestListUiState.Success)
    }
}