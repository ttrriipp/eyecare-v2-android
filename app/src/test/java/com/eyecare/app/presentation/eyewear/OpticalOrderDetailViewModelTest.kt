package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.domain.model.PaymentSummary
import com.eyecare.app.domain.model.QuotationReference
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.model.RatingSummary
import com.eyecare.app.domain.repository.OpticalOrderRepository
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
class OpticalOrderDetailViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository: OpticalOrderRepository = mockk()

    private fun createOrder(
        id: Int = 1,
        status: OpticalOrderStatus = OpticalOrderStatus.IN_PROGRESS,
        sourceQuotation: QuotationReference? = null,
        items: List<OpticalOrderItem> = listOf(
            OpticalOrderItem(10, "Lens", 1, BigDecimal("4500.00"), BigDecimal("4500.00"), null, false, null),
        ),
        paymentSummary: PaymentSummary? = null,
    ) = OpticalOrder(
        id = id,
        orderNumber = "OO-2026-${id.toString().padStart(3, '0')}",
        status = status,
        fulfillmentMode = FulfillmentMode.PREPARED,
        totalAmount = BigDecimal("5000.00"),
        startedAt = "2026-08-02T09:00:00Z",
        readyAt = null,
        dispensedAt = null,
        cancelledAt = null,
        createdAt = "2026-08-01T10:00:00Z",
        sourceQuotation = sourceQuotation,
        items = items,
        paymentSummary = paymentSummary,
    )

    private fun createVm(id: Int = 1) = OpticalOrderDetailViewModel(
        repository = repository,
        savedStateHandle = SavedStateHandle(mapOf("orderId" to id)),
    )

    @BeforeEach
    fun setup() { Dispatchers.setMain(dispatcher) }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `loads order detail by ID`() = runTest {
        coEvery { repository.getOpticalOrder(1) } returns Result.success(createOrder())
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OpticalOrderDetailUiState.Success
        assertEquals(1, state.order.id)
    }

    @Test
    fun `shows error on failure`() = runTest {
        coEvery { repository.getOpticalOrder(1) } returns Result.failure(RuntimeException("not found"))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is OpticalOrderDetailUiState.Error)
    }

    @Test
    fun `retry reloads after error`() = runTest {
        coEvery { repository.getOpticalOrder(1) } returns Result.failure(RuntimeException("offline"))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(vm.uiState.value is OpticalOrderDetailUiState.Error)

        coEvery { repository.getOpticalOrder(1) } returns Result.success(createOrder())
        vm.retry()
        dispatcher.scheduler.advanceUntilIdle()

        assertTrue(vm.uiState.value is OpticalOrderDetailUiState.Success)
    }

    @Test
    fun `source quotation is present when linked`() = runTest {
        coEvery { repository.getOpticalOrder(1) } returns Result.success(
            createOrder(sourceQuotation = QuotationReference(1, "Q-001"))
        )
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OpticalOrderDetailUiState.Success
        assertEquals(1, state.order.sourceQuotation?.id)
    }

    @Test
    fun `source quotation is null when direct order`() = runTest {
        coEvery { repository.getOpticalOrder(1) } returns Result.success(createOrder(sourceQuotation = null))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OpticalOrderDetailUiState.Success
        assertNull(state.order.sourceQuotation)
    }

    @Test
    fun `updateItemRating replaces rating on matching item`() = runTest {
        val items = listOf(
            OpticalOrderItem(10, "Lens", 1, BigDecimal("4500.00"), BigDecimal("4500.00"), null, true, null),
        )
        coEvery { repository.getOpticalOrder(1) } returns Result.success(createOrder(items = items))
        val vm = createVm()
        dispatcher.scheduler.advanceUntilIdle()

        vm.updateItemRating(10, RatingResult(5, "Great!", 1))
        val state = vm.uiState.value as OpticalOrderDetailUiState.Success
        assertEquals(5, state.order.items[0].rating?.rating)
    }
}
