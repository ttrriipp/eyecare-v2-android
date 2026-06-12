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
        price = "165.00", dimensions = null,
        brand = "Ray-Ban",
        category = "Frames",
        variants = listOf(
            ProductDtos.VariantDto(1, "Black", "RB-001", "165.00", null, true, "frames/rb001.png")
        ),
        images = listOf(ProductDtos.ImageDto(1, "products/clubmaster.jpg", true, 0)),
    )

    private val fakeEntity = ProductEntity(
        id = 1, name = "Clubmaster", slug = "clubmaster", description = "Classic",
        price = "165.00", dimensions = null, brandName = "Ray-Ban", categoryName = "Frames",
        variantsJson = "[]", imagesJson = "[]",
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
        coEvery { api.getProducts() } returns ProductDtos.ProductListResponse(listOf(fakeProductDto))
        coEvery { dao.getAll() } returns listOf(fakeEntity)

        val result = repository.getProducts()

        assertTrue(result.isSuccess)
        coVerify { dao.insertAll(any()) }
        val products = result.getOrThrow()
        assertEquals(1, products.size)
        assertEquals("Clubmaster", products[0].name)
        assertEquals("Ray-Ban", products[0].brand)
    }

    @Test
    fun `getProducts falls back to cache when network fails`() = runTest {
        coEvery { api.getProducts() } throws RuntimeException("No network")
        coEvery { dao.getAll() } returns listOf(fakeEntity)

        val result = repository.getProducts()

        assertTrue(result.isSuccess)
        assertEquals("Clubmaster", result.getOrThrow()[0].name)
    }

    @Test
    fun `getProducts returns failure when network fails and cache is empty`() = runTest {
        coEvery { api.getProducts() } throws RuntimeException("No network")
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
}
