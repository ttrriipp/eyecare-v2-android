package com.eyecare.app.presentation.catalog

import app.cash.turbine.test
import com.eyecare.app.domain.model.Product
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

/**
 * `ProductListViewModel` delegates all filtering (search, brand, category, sort, in-stock) to
 * the repository via `getProducts(...)` query params — it does not filter in-memory. So these
 * tests mock the repository to return different result sets per filter combination, rather than
 * asserting on client-side filtering of a single fixed product list.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProductRepository

    private fun makeProduct(id: Int, category: String, arEligible: Boolean = false) = Product(
        id = id, name = "Product $id", slug = "product-$id", description = null,
        productType = "frame", brand = "Brand", category = category,
        variants = if (arEligible) listOf(
            ProductVariant(id, "v", "sku", "100.00", null, null, true, true, "img.png", emptyList())
        ) else emptyList(),
        images = emptyList(),
    )

    private val allProducts = listOf(
        makeProduct(1, "Frames", arEligible = true),
        makeProduct(2, "Sunglasses"),
        makeProduct(3, "Frames"),
        makeProduct(4, "Contacts"),
    )
    private val framesProducts = allProducts.filter { it.category == "Frames" }
    private val product1Only = allProducts.filter { it.id == 1 }

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = mockk()
        coEvery { repo.getBrands() } returns Result.success(emptyList())
        coEvery { repo.getCategories() } returns Result.success(emptyList())
    }

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    /** Stubs `getProducts` for the default (no search/brand/category/in-stock) filter state. */
    private fun stubDefaultProducts(result: Result<List<Product>> = Result.success(allProducts)) {
        coEvery {
            repo.getProducts(
                page = 1, search = null, brandId = null, categoryId = null,
                sort = "name", inStock = null, minPrice = null, maxPrice = null,
            )
        } returns result
        coEvery { repo.hasMorePages(any()) } returns false
    }

    @Test
    fun `initial state is Loading then Success with all products`() = runTest {
        stubDefaultProducts()
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
    fun `selecting a category id filters via the repository`() = runTest {
        stubDefaultProducts()
        coEvery {
            repo.getProducts(
                page = 1, search = null, brandId = null, categoryId = 1,
                sort = "name", inStock = null, minPrice = null, maxPrice = null,
            )
        } returns Result.success(framesProducts)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (all)

            vm.selectCategory(1)
            dispatcher.scheduler.advanceUntilIdle()
            val filtered = awaitItem() as ProductListUiState.Success
            assertEquals(2, filtered.products.size)
            filtered.products.forEach { assertEquals("Frames", it.category) }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clearing category selection shows all products again`() = runTest {
        stubDefaultProducts()
        coEvery {
            repo.getProducts(
                page = 1, search = null, brandId = null, categoryId = 1,
                sort = "name", inStock = null, minPrice = null, maxPrice = null,
            )
        } returns Result.success(framesProducts)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success (all)

            vm.selectCategory(1)
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // filtered

            vm.selectCategory(null)
            dispatcher.scheduler.advanceUntilIdle()
            val all = awaitItem() as ProductListUiState.Success
            assertEquals(4, all.products.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search query is forwarded to the repository`() = runTest {
        stubDefaultProducts()
        coEvery {
            repo.getProducts(
                page = 1, search = "Product 1", brandId = null, categoryId = null,
                sort = "name", inStock = null, minPrice = null, maxPrice = null,
            )
        } returns Result.success(product1Only)
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Success

            vm.search("Product 1")
            dispatcher.scheduler.advanceUntilIdle()
            val filtered = awaitItem() as ProductListUiState.Success
            assertEquals(1, filtered.products.size)
            assertEquals(1, filtered.products[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `error from repo emits Error state`() = runTest {
        stubDefaultProducts(Result.failure(RuntimeException("offline")))
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem()
            dispatcher.scheduler.advanceUntilIdle()
            assertInstanceOf(ProductListUiState.Error::class.java, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
