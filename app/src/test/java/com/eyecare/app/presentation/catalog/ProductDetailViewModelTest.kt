package com.eyecare.app.presentation.catalog

import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductImage
import com.eyecare.app.domain.model.ProductVariant
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProductRepository

    private val arVariant = ProductVariant(1, "Black", "BK-001", "165.00", null, arEligible = true, "frames/bk.png")
    private val nonArVariant = ProductVariant(2, "Gold", "GD-001", "185.00", null, arEligible = false, null)

    private val fakeProduct = Product(
        id = 1, name = "Clubmaster", slug = "clubmaster", description = "Classic style",
        price = "165.00", dimensions = "Bridge: 18 · Temple: 140", brand = "Ray-Ban",
        category = "Frames",
        variants = listOf(arVariant, nonArVariant),
        images = listOf(ProductImage(1, "products/img.jpg", true, 0)),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading then Success with first variant selected`() = runTest {
        coEvery { repo.getProduct(1) } returns Result.success(fakeProduct)
        val vm = ProductDetailViewModel(repo, 1)

        dispatcher.scheduler.advanceUntilIdle()
        val state = vm.uiState.value
        assertInstanceOf(ProductDetailUiState.Success::class.java, state)
        assertEquals(arVariant, (state as ProductDetailUiState.Success).selectedVariant)
    }

    @Test
    fun `selecting variant updates selectedVariant and price`() = runTest {
        coEvery { repo.getProduct(1) } returns Result.success(fakeProduct)
        val vm = ProductDetailViewModel(repo, 1)
        dispatcher.scheduler.advanceUntilIdle()

        vm.selectVariant(nonArVariant)

        val state = vm.uiState.value as ProductDetailUiState.Success
        assertEquals(nonArVariant, state.selectedVariant)
        assertEquals("185.00", state.selectedVariant.price)
    }

    @Test
    fun `showArButton is true only when selected variant has arEligible = true`() = runTest {
        coEvery { repo.getProduct(1) } returns Result.success(fakeProduct)
        val vm = ProductDetailViewModel(repo, 1)
        dispatcher.scheduler.advanceUntilIdle()

        val initial = vm.uiState.value as ProductDetailUiState.Success
        assertTrue(initial.selectedVariant.arEligible)

        vm.selectVariant(nonArVariant)
        val updated = vm.uiState.value as ProductDetailUiState.Success
        assertFalse(updated.selectedVariant.arEligible)
    }

    @Test
    fun `error state on repo failure`() = runTest {
        coEvery { repo.getProduct(99) } returns Result.failure(RuntimeException("not found"))
        val vm = ProductDetailViewModel(repo, 99)
        dispatcher.scheduler.advanceUntilIdle()

        assertInstanceOf(ProductDetailUiState.Error::class.java, vm.uiState.value)
    }
}
