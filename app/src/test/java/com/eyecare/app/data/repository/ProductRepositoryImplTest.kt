package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.ProductDao
import com.eyecare.app.data.local.entity.ProductEntity
import com.eyecare.app.data.remote.api.ProductApiService
import com.eyecare.app.data.remote.dto.ProductDtos
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ProductRepositoryImplTest {

    private lateinit var api: ProductApiService
    private lateinit var dao: ProductDao
    private lateinit var repository: ProductRepositoryImpl

    private val fakeProductDto = ProductDtos.ProductDto(
        id = 1, name = "Clubmaster", slug = "clubmaster", description = "Classic",
        productType = "frame",
        brand = "Ray-Ban",
        category = "Frames",
        variants = listOf(
            ProductDtos.VariantDto(1, "Black", "RB-001", "165.00", null, null, true, true, "frames/rb001.png", emptyList())
        ),
        images = listOf("products/clubmaster.jpg"),
    )

    private val fakeMeta = ProductDtos.PaginationMeta(1, 1, 15, 1)

    private val fakeEntity = ProductEntity(
        id = 1, name = "Clubmaster", slug = "clubmaster", description = "Classic",
        productType = "frame", brandName = "Ray-Ban", categoryName = "Frames",
        variantsJson = """
            [{"id":1,"name":"Black","sku":"RB-001","price":"165.00",
            "in_stock":true,"ar_eligible":true,"ar_asset_reference":"frames/rb001.png"}]
        """.trimIndent(),
        imagesJson = "[]",
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        api = mockk()
        dao = mockk(relaxed = true)
        repository = ProductRepositoryImpl(api, dao, json)
    }

    @Test
    fun `getProducts fetches from network, caches, returns mapped domain models`() = runTest {
        coEvery { api.getProducts(any(), any()) } returns ProductDtos.PaginatedProductResponse(listOf(fakeProductDto), fakeMeta)
        coEvery { dao.getAll() } returns listOf(fakeEntity)

        val result = repository.getProducts()

        assertTrue(result.isSuccess)
        coVerify { dao.insertAll(any()) }
        val products = result.getOrThrow()
        assertEquals(1, products.size)
        assertEquals("Clubmaster", products[0].name)
        assertEquals("Ray-Ban", products[0].brand)
        assertEquals("frame", products[0].productType)
    }

    @Test
    fun `getProducts normalizes a null category at the repository boundary`() = runTest {
        val productWithoutCategory = fakeProductDto.copy(category = null)
        coEvery { api.getProducts(any(), any()) } returns
            ProductDtos.PaginatedProductResponse(listOf(productWithoutCategory), fakeMeta)

        val product = repository.getProducts().getOrThrow().single()

        assertEquals("", product.category)
        coVerify {
            dao.insertAll(match { entities -> entities.single().categoryName == "" })
        }
    }

    @Test
    fun `getProducts falls back to cache when network fails`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws RuntimeException("No network")
        coEvery { dao.getAll() } returns listOf(fakeEntity)

        val result = repository.getProducts()

        assertTrue(result.isSuccess)
        assertEquals("Clubmaster", result.getOrThrow()[0].name)
    }

    @Test
    fun `getProducts returns failure when network fails and cache is empty`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws RuntimeException("No network")
        coEvery { dao.getAll() } returns emptyList()

        val result = repository.getProducts()

        assertTrue(result.isFailure)
    }

    @Test
    fun `getProduct fetches single product from network`() = runTest {
        coEvery { api.getProduct(1) } returns ProductDtos.ProductResponse(fakeProductDto)

        val result = repository.getProduct(1)

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().id)
        assertTrue(result.getOrThrow().variants[0].arEligible)
    }

    @Test
    fun `getProducts hides disallowed types and removes non AR ready frame variants`() = runTest {
        val nonArVariant = fakeProductDto.variants.single().copy(
            id = 2,
            arAssetReference = null,
        )
        val accessory = fakeProductDto.copy(id = 2, productType = "accessory")
        val contactLens = fakeProductDto.copy(id = 3, productType = "contact_lens")
        coEvery { api.getProducts(any(), any()) } returns ProductDtos.PaginatedProductResponse(
            listOf(
                fakeProductDto.copy(variants = fakeProductDto.variants + nonArVariant),
                accessory,
                contactLens,
            ),
            fakeMeta,
        )

        val products = repository.getProducts().getOrThrow()

        assertEquals(listOf(1, 2), products.map { it.id })
        assertEquals(listOf(1), products.first().variants.map { it.id })
        coVerify {
            dao.insertAll(match { entities -> entities.map { it.id } == listOf(1, 2) })
        }
    }

    @Test
    fun `getProducts cache fallback hides legacy product types`() = runTest {
        coEvery { api.getProducts(any(), any()) } throws RuntimeException("No network")
        coEvery { dao.getAll() } returns listOf(
            fakeEntity,
            fakeEntity.copy(id = 2, productType = "general"),
            fakeEntity.copy(id = 3, productType = "lens"),
        )

        val products = repository.getProducts().getOrThrow()

        assertEquals(listOf(1), products.map { it.id })
    }

    @Test
    fun `getProduct does not return a hidden cached product after network failure`() = runTest {
        coEvery { api.getProduct(2) } throws RuntimeException("Not found")
        coEvery { dao.getById(2) } returns fakeEntity.copy(id = 2, productType = "contact_lens")

        val result = repository.getProduct(2)

        assertTrue(result.isFailure)
    }
}
