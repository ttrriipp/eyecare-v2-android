package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.eyecare.app.domain.model.OpticalOrderReference
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.model.QuotationItem
import com.eyecare.app.domain.model.QuotationItemType
import com.eyecare.app.domain.model.QuotationStatus
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
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class EstimateDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: QuotationRepository = mockk()

    private fun createQuotation(
        id: Int = 1,
        opticalOrder: OpticalOrderReference? = null,
        notes: String? = null,
    ) = Quotation(
        id = id,
        quotationNumber = "Q-2026-001",
        status = QuotationStatus.PRESENTED,
        validUntil = "2026-09-01T00:00:00Z",
        subtotal = BigDecimal("1500.00"),
        discountAmount = BigDecimal("100.00"),
        total = BigDecimal("1400.00"),
        notes = notes,
        createdAt = "2026-08-01T10:00:00Z",
        presentedAt = "2026-08-01T10:05:00Z",
        confirmedAt = null,
        opticalOrder = opticalOrder,
        items = listOf(
            QuotationItem(10, QuotationItemType.PRODUCT, "Lens", 1, BigDecimal("1200.00"), BigDecimal("1200.00")),
            QuotationItem(11, QuotationItemType.SERVICE, "Fitting", 1, BigDecimal("300.00"), BigDecimal("300.00")),
        ),
    )

    private fun createVm(id: Int = 1) = EstimateDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("quotationId" to id)),
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads quotation detail by ID`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.success(createQuotation())
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateDetailUiState.Success
        assertEquals(1, state.quotation.id)
        assertEquals(2, state.quotation.items.size)
    }

    @Test
    fun `shows error on failure`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.failure(RuntimeException("not found"))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EstimateDetailUiState.Error)
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.failure(RuntimeException("offline"))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is EstimateDetailUiState.Error)

        coEvery { repository.getQuotation(1) } returns Result.success(createQuotation())
        vm.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is EstimateDetailUiState.Success)
    }

    @Test
    fun `passes ID unchanged to repository`() = runTest {
        coEvery { repository.getQuotation(42) } returns Result.success(createQuotation(42))
        val vm = createVm(42)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateDetailUiState.Success
        assertEquals(42, state.quotation.id)
    }

    @Test
    fun `optical_order reference is present when linked`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.success(
            createQuotation(opticalOrder = OpticalOrderReference(5, "OO-005"))
        )
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateDetailUiState.Success
        val ref = state.quotation.opticalOrder!!
        assertEquals(5, ref.id)
        assertEquals("OO-005", ref.orderNumber)
    }

    @Test
    fun `optical_order is null when not linked`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.success(createQuotation(opticalOrder = null))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateDetailUiState.Success
        assertNull(state.quotation.opticalOrder)
    }

    @Test
    fun `notes are preserved`() = runTest {
        coEvery { repository.getQuotation(1) } returns Result.success(createQuotation(notes = "Please confirm"))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as EstimateDetailUiState.Success
        assertEquals("Please confirm", state.quotation.notes)
    }
}
