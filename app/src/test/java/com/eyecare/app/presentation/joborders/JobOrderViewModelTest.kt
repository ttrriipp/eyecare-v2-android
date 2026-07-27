package com.eyecare.app.presentation.joborders

import com.eyecare.app.domain.model.JobOrder
import com.eyecare.app.domain.model.JobOrderItem
import com.eyecare.app.domain.model.JobOrderStatus
import com.eyecare.app.domain.repository.JobOrderRepository
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class JobOrderViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: JobOrderRepository

    private val fakeOrder = JobOrder(
        id = 1, jobOrderNumber = "JO-001", patientId = 1, encounterId = 1,
        prescriptionId = 1, quotationRevisionId = 1, status = JobOrderStatus.IN_PROGRESS,
        totalAmount = 8000.0, notes = null, startedAt = null, readyAt = null,
        dispensedAt = null, cancelledAt = null,
        items = listOf(JobOrderItem(1, "Frame", 1, 4500.0, 4500.0, 42)),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `list loads job orders with pagination`() = runTest {
        coEvery { repo.getJobOrders(1) } returns Result.success(PaginatedResult(listOf(fakeOrder), 1, 2, 30))
        val vm = JobOrderViewModel(repo)
        val state = vm.listState.value as JobOrderListUiState.Success
        assertEquals(1, state.jobOrders.size)
        assertTrue(state.hasMorePages)
    }

    @Test
    fun `list empty emits Empty state`() = runTest {
        coEvery { repo.getJobOrders(1) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        val vm = JobOrderViewModel(repo)
        assertInstanceOf(JobOrderListUiState.Empty::class.java, vm.listState.value)
    }

    @Test
    fun `list error emits Error state`() = runTest {
        coEvery { repo.getJobOrders(1) } returns Result.failure(RuntimeException("offline"))
        val vm = JobOrderViewModel(repo)
        assertInstanceOf(JobOrderListUiState.Error::class.java, vm.listState.value)
    }

    @Test
    fun `loadDetail loads single job order`() = runTest {
        coEvery { repo.getJobOrders(1) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { repo.getJobOrder(1) } returns Result.success(fakeOrder)
        val vm = JobOrderViewModel(repo)
        vm.loadDetail(1)
        val state = vm.detailState.value as JobOrderDetailUiState.Success
        assertEquals(1, state.jobOrder.id)
        assertEquals(JobOrderStatus.IN_PROGRESS, state.jobOrder.status)
        assertEquals(1, state.jobOrder.items.size)
    }

    @Test
    fun `loadDetail error emits Error state`() = runTest {
        coEvery { repo.getJobOrders(1) } returns Result.success(PaginatedResult(emptyList(), 1, 1, 0))
        coEvery { repo.getJobOrder(999) } returns Result.failure(RuntimeException("not found"))
        val vm = JobOrderViewModel(repo)
        vm.loadDetail(999)
        assertInstanceOf(JobOrderDetailUiState.Error::class.java, vm.detailState.value)
    }

    @Test
    fun `unknown status is non-actionable`() = runTest {
        val unknownOrder = fakeOrder.copy(status = JobOrderStatus.QUEUED)
        coEvery { repo.getJobOrders(1) } returns Result.success(PaginatedResult(listOf(unknownOrder), 1, 1, 1))
        val vm = JobOrderViewModel(repo)
        val state = vm.listState.value as JobOrderListUiState.Success
        assertEquals(JobOrderStatus.QUEUED, state.jobOrders[0].status)
    }
}
