package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.ApiContractFixtures
import com.eyecare.app.data.remote.api.SavedFrameApiService
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class SavedFrameRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: SavedFrameRepositoryImpl
    private val json: Json = ApiContractFixtures.json

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = SavedFrameRepositoryImpl(retrofit.create(SavedFrameApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getSavedFrames maps DTOs to domain models`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageAvailable))

        val result = repository.getSavedFrames()

        assertTrue(result.isSuccess)
        val page = result.getOrNull()!!
        assertEquals(1, page.items.size)
        assertEquals(1, page.currentPage)
        assertEquals(1, page.lastPage)

        val item = page.items[0]
        assertEquals(42, item.productVariantId)
        assertEquals(SavedFrameAvailability.AVAILABLE, item.availability)
        assertEquals("Black / 52mm", item.variant.name)
        assertEquals("Classic Rectangle", item.variant.product.name)
    }

    @Test
    fun `getSavedFrames maps unavailable availability`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageUnavailable))

        val result = repository.getSavedFrames()

        assertTrue(result.isSuccess)
        val item = result.getOrNull()!!.items[0]
        assertEquals(SavedFrameAvailability.UNAVAILABLE, item.availability)
    }

    @Test
    fun `save returns server resource`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFrameSaveResponse))

        val result = repository.save(42)

        assertTrue(result.isSuccess)
        val saved = result.getOrNull()!!
        assertEquals(42, saved.productVariantId)
        assertEquals(SavedFrameAvailability.AVAILABLE, saved.availability)
    }

    @Test
    fun `remove treats 204 as success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val result = repository.remove(42)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `remove treats 200 as idempotent success`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val result = repository.remove(42)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `getSavedFrames maps AR data when present`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageAvailable))

        val result = repository.getSavedFrames()
        val ar = result.getOrNull()!!.items[0].variant.ar

        assertTrue(ar != null)
        assertEquals("https://cdn.example.com/ar/variants/42/v1/model.glb", ar!!.asset.url)
    }

    @Test
    fun `getSavedFrames handles null AR`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageUnavailable))

        val result = repository.getSavedFrames()
        val ar = result.getOrNull()!!.items[0].variant.ar

        assertTrue(ar == null)
    }
}
