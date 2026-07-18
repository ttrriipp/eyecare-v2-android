package com.eyecare.app.presentation.catalog

import app.cash.turbine.test
import com.eyecare.app.domain.model.Category
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
 * `ProductListViewModel` delegates search, brand, category, sort, and stock filtering to the
 * repository. Catalog tabs are filtered in-memory because the products endpoint does not expose
 * a product-type query parameter.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repo: ProductRepository

    private fun makeProduct(
        id: Int,
        category: String,
        productType: String = "frame",
        arEligible: Boolean = false,
    ) = Product(
        id = id, name = "Product $id", slug = "product-$id", description = null,
        productType = productType, brand = "Brand", category = category,
        variants = if (arEligible) listOf(
            ProductVariant(id, "v", "sku", "100.00", null, null, true, true, "img.png", emptyList())
        ) else emptyList(),
        images = emptyList(),
    )

    private val allProducts = listOf(
        makeProduct(1, "Frames", arEligible = true),
        makeProduct(2, "Sunglasses"),
        makeProduct(3, "Cleaning Kits", productType = "general"),
        makeProduct(4, "Contact Accessories", productType = "GENERAL"),
        makeProduct(5, "Eye Drops", productType = "accessory"),
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
    fun `initial state shows frames by default`() = runTest {
        stubDefaultProducts()
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            assertInstanceOf(ProductListUiState.Loading::class.java, awaitItem())
            dispatcher.scheduler.advanceUntilIdle()
            val state = awaitItem() as ProductListUiState.Success
            assertEquals(CatalogTab.FRAMES, state.selectedTab)
            assertEquals(listOf(1, 2), state.products.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting eye products shows every non-frame product`() = runTest {
        stubDefaultProducts()
        val vm = ProductListViewModel(repo)

        vm.uiState.test {
            awaitItem() // Loading
            dispatcher.scheduler.advanceUntilIdle()
            awaitItem() // Frames

            vm.selectCatalogTab(CatalogTab.EYE_PRODUCTS)

            val state = awaitItem() as ProductListUiState.Success
            assertEquals(CatalogTab.EYE_PRODUCTS, state.selectedTab)
            assertEquals(listOf(3, 4, 5), state.products.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `catalog tabs expose only their relevant category filters`() {
        val categories = listOf(
            Category(1, "Frames"),
            Category(2, "Sunglasses"),
            Category(3, "Contact Lenses"),
            Category(4, "Accessories"),
            Category(5, "Cleaning Kits"),
        )

        assertEquals(
            listOf("Frames", "Sunglasses"),
            categoriesForCatalogTab(categories, CatalogTab.FRAMES).map { it.name },
        )
        assertEquals(
            listOf("Contact Lenses", "Accessories"),
            categoriesForCatalogTab(categories, CatalogTab.EYE_PRODUCTS).map { it.name },
        )
    }

    @Test
    fun `filter options are retained when they load before products`() = runTest {
        val brands = listOf(com.eyecare.app.domain.model.Brand(1, "VisionCraft"))
        val categories = listOf(
            Category(3, "Contact Lenses"),
            Category(4, "Accessories"),
        )
        coEvery { repo.getBrands() } returns Result.success(brands)
        coEvery { repo.getCategories() } returns Result.success(categories)
        stubDefaultProducts()

        val vm = ProductListViewModel(repo)
        dispatcher.scheduler.advanceUntilIdle()

        val state = vm.uiState.value as ProductListUiState.Success
        assertEquals(brands, state.brands)
        assertEquals(categories, state.categories)
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
            assertEquals(1, filtered.products.size)
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
            assertEquals(listOf(1, 2), all.products.map { it.id })
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
