package com.eyecare.app.presentation.orders

import app.cash.turbine.test
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ProductRepository
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderRequestViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var orderRepo: OrderRepository
    private lateinit var productRepo: ProductRepository
    private lateinit var appointmentRepo: AppointmentRepository

    private val fakeVariant = ProductVariant(1, "Black", "BK-001", "165.00", null, true, null)
    private val fakeProduct = Product(1, "Clubmaster", "clubmaster", null, "165.00", null, "Ray-Ban", "Frames",
        listOf(fakeVariant), emptyList())
    private val fakeOrder = Order(1, "ORD-001", null, false, OrderStatus.REQUESTED,
        "165.00", "165.00", emptyList(), "2026-10-24T10:00:00Z")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        orderRepo = mockk()
        productRepo = mockk()
        appointmentRepo = mockk()
        coEvery { productRepo.getProduct(1) } returns Result.success(fakeProduct)
        coEvery { appointmentRepo.getAppointments() } returns Result.success(emptyList())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = OrderRequestViewModel(orderRepo, productRepo, appointmentRepo, 1, 1)

    @Test
    fun `initial state loads product and variant`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value as OrderRequestUiState.Ready
        assertEquals("Clubmaster", state.product.name)
        assertEquals(fakeVariant, state.selectedVariant)
    }

    @Test
    fun `selectLensType updates selected lens type`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectLensType(LensType.BIFOCAL)
        val state = vm.uiState.value as OrderRequestUiState.Ready
        assertEquals(LensType.BIFOCAL, state.selectedLensType)
    }

    @Test
    fun `setQuantity updates quantity within bounds`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.setQuantity(3)
        assertEquals(3, (vm.uiState.value as OrderRequestUiState.Ready).quantity)
    }

    @Test
    fun `setQuantity clamps to 1-4 range`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.setQuantity(0)
        assertEquals(1, (vm.uiState.value as OrderRequestUiState.Ready).quantity)
        vm.setQuantity(5)
        assertEquals(4, (vm.uiState.value as OrderRequestUiState.Ready).quantity)
    }

    @Test
    fun `submit success emits Submitted`() = runTest {
        coEvery { orderRepo.createOrder(any(), any(), any()) } returns Result.success(fakeOrder)
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.selectLensType(LensType.SINGLE_VISION)

        vm.uiState.test {
            awaitItem() // Ready
            vm.submit()
            val loading = awaitItem() as OrderRequestUiState.Ready
            assertEquals(true, loading.isSubmitting)
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(OrderRequestUiState.Submitted::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `submit without lens type emits validation error`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        // No lens type selected (default null)
        vm.submit()
        val state = vm.uiState.value as OrderRequestUiState.Ready
        assertNotNull(state.error)
    }
}
