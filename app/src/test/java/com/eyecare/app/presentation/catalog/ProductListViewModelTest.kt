package com.eyecare.app.presentation.catalog

import app.cash.turbine.test
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProductRepository

    private fun makeProduct(id: Int, category: String, arEligible: Boolean = false) = Product(
        id = id, name = "Product $id", slug = "product-$id", description = null,
        price = "100.00", dimensions = null, brand = "Brand", category = category,
        variants = if (arEligible) listOf(
            ProductVariant(id, "v", "sku", "100.00", null, true, "img.png")
        ) else emptyList(),
        images = emptyList(),
    )

    private val products = listOf(
        makeProduct(1, "Frames", arEligible = true),
        makeProduct(2, "Sunglasses"),
        makeProduct(3, "Frames"),
        makeProduct(4, "Contacts"),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `initial state is Loading then Success with all products`() = runTest {
        coEvery { repo.getProducts() } returns Result.success(products)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            assertInstanceOf(ProductListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as ProductListUiState.Success
            assertEquals(4, state.products.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting Frames category filters to frames only`() = runTest {
        coEvery { repo.getProducts() } returns Result.success(products)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (all)

            vm.selectCategory("Frames")
            val filtered = awaitItem() as ProductListUiState.Success
            assertEquals(2, filtered.products.size)
            filtered.products.forEach { assertEquals("Frames", it.category) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting All category shows all products`() = runTest {
        coEvery { repo.getProducts() } returns Result.success(products)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (all)

            vm.selectCategory("Frames")
            awaitItem() // filtered
            vm.selectCategory("All")
            val all = awaitItem() as ProductListUiState.Success
            assertEquals(4, all.products.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search query filters products by name`() = runTest {
        coEvery { repo.getProducts() } returns Result.success(products)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success

            vm.search("Product 1")
            val filtered = awaitItem() as ProductListUiState.Success
            assertEquals(1, filtered.products.size)
            assertEquals(1, filtered.products[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error from repo emits Error state`() = runTest {
        coEvery { repo.getProducts() } returns Result.failure(RuntimeException("offline"))
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(ProductListUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
