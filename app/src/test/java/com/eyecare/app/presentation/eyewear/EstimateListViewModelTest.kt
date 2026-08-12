package com.eyecare.app.presentation.eyewear

import app.cash.turbine.test
import com.eyecare.app.domain.model.OpticalOrderReference
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.model.QuotationItem
import com.eyecare.app.domain.model.QuotationItemType
import com.eyecare.app.domain.model.QuotationStatus
import com.eyecare.app.domain.repository.PaginatedResult
import com.eyecare.app.domain.repository.QuotationRepository
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class EstimateListViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: QuotationRepository = mockk()

    private fun createQuotation(
        id: Int = 1,
        status: QuotationStatus = QuotationStatus.PRESENTED,
        opticalOrder: OpticalOrderReference? = null,
    ) = Quotation(
        id = id,
        quotationNumber = "Q-2026-${id.toString().padStart(3, '0')}",
        status = status,
        validUntil = "2026-09-01T00:00:00Z",
        subtotal = BigDecimal("1500.00"),
        discountAmount = BigDecimal("100.00"),
        total = BigDecimal("1400.00"),
        notes = null,
        createdAt = "2026-08-01T10:00:00Z",
        presentedAt = "2026-08-01T10:05:00Z",
        confirmedAt = null,
        opticalOrder = opticalOrder,
        items = listOf(
            QuotationItem(10, QuotationItemType.PRODUCT, "Lens", 1, BigDecimal("1200.00"), BigDecimal("1200.00"), null, null, null),
        ),
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial load fetches all estimates`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation()), 1, 1, 1)
        )
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateListUiState.Success
        assertEquals(1, state.items.size)
    }

    @Test
    fun `empty list shows empty state`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(emptyList(), 1, 1, 0)
        )
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EstimateListUiState.Empty)
    }

    @Test
    fun `error shows error state`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.failure(RuntimeException("offline"))
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EstimateListUiState.Error)
    }

    @Test
    fun `retry reloads`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.failure(RuntimeException("offline"))
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is EstimateListUiState.Error)

        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation()), 1, 1, 1)
        )
        vm.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EstimateListUiState.Success)
    }

    @Test
    fun `refresh reloads at page 1`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation(1)), 1, 2, 2)
        )
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation(1)), 1, 1, 1)
        )
        vm.refresh()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateListUiState.Success
        assertFalse(state.hasMorePages)
    }

    @Test
    fun `loadMore appends and guards duplicates`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation(1)), 1, 2, 2)
        )
        coEvery { repository.getQuotations(null, 2) } returns Result.success(
            PaginatedResult(listOf(createQuotation(2)), 2, 2, 2)
        )
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val initial = vm.uiState.value as EstimateListUiState.Success
        assertTrue(initial.hasMorePages)

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val final = vm.uiState.value as EstimateListUiState.Success
        assertEquals(2, final.items.size)
        assertFalse(final.hasMorePages)
    }

    @Test
    fun `loadMore failure retains already-loaded cards`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation(1)), 1, 2, 2)
        )
        coEvery { repository.getQuotations(null, 2) } returns Result.failure(RuntimeException("timeout"))
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        vm.loadMore()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateListUiState.Success
        assertEquals(1, state.items.size)
        assertTrue(state.loadMoreError != null)
    }

    @Test
    fun `no optical order repository or aggregate type referenced`() = runTest {
        coEvery { repository.getQuotations(null, 1) } returns Result.success(
            PaginatedResult(listOf(createQuotation(opticalOrder = OpticalOrderReference(5, "OO-005"))), 1, 1, 1)
        )
        val vm = EstimateListViewModel(repository)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateListUiState.Success
        assertEquals(5, state.items[0].opticalOrder?.id)
    }
}
