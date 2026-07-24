package com.eyecare.app.presentation.orders

import app.cash.turbine.test
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ProductRepository
import io.mockk.coEvery
import io.mockk.coVerify
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
class OrderRequestViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var orderRepo: OrderRepository
    private lateinit var productRepo: ProductRepository

    private val fakeVariant = ProductVariant(1, "Black", "BK-001", "165.00", null, null, true, true, null, emptyList())
    private val fakeProduct = Product(1, "Cleaning Kit", "cleaning-kit", null, "accessory", "VisionCare", "Accessories",
        listOf(fakeVariant), emptyList())
    private val fakeOrder = Order(1, "ORD-001", null, null, true, OrderStatus.REQUESTED,
        "165.00", "165.00", emptyList(), "2026-10-24T10:00:00Z")

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        orderRepo = mockk()
        productRepo = mockk()
        coEvery { productRepo.getProduct(1) } returns Result.success(fakeProduct)
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun vm() = OrderRequestViewModel(orderRepo, productRepo, 1, 1)

    @Test
    fun `initial state loads product and variant`() = runTest {
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value as OrderRequestUiState.Ready
        assertEquals("Cleaning Kit", state.product.name)
        assertEquals(fakeVariant, state.selectedVariant)
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
        coEvery { orderRepo.createOrder(any()) } returns Result.success(fakeOrder)
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

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
    fun `non accessory product cannot enter order flow`() = runTest {
        coEvery { productRepo.getProduct(1) } returns
            Result.success(fakeProduct.copy(productType = "frame"))
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as OrderRequestUiState.Error
        assertEquals(false, state.canRetry)
        coVerify(exactly = 0) { orderRepo.createOrder(any()) }
    }

    @Test
    fun `accessory submission contains only variant and quantity`() = runTest {
        coEvery { orderRepo.createOrder(any()) } returns Result.success(fakeOrder)
        val vm = vm()
        dispatcher.scheduler.advanceUntilIdle()
        vm.setQuantity(3)
        vm.submit()
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            orderRepo.createOrder(
                items = match { items ->
                    items.single().productVariantId == fakeVariant.id &&
                        items.single().quantity == 3
                },
            )
        }
    }
}
