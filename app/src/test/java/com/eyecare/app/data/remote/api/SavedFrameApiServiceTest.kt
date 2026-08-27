package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.ApiContractFixtures
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

class SavedFrameApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SavedFrameApiService
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
        api = retrofit.create(SavedFrameApiService::class.java)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getSavedFrames sends page and per_page query parameters`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageAvailable))

        api.getSavedFrames(page = 2, perPage = 25)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("page=2"))
        assertTrue(request.path!!.contains("per_page=25"))
        assertEquals("GET", request.method)
    }

    @Test
    fun `getSavedFrames decodes page envelope`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFramesPageAvailable))

        val response = api.getSavedFrames()

        assertEquals(1, response.data.size)
        assertEquals(42, response.data[0].productVariantId)
        assertEquals(1, response.meta.currentPage)
    }

    @Test
    fun `saveFrame sends PUT with no body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFrameSaveResponse))

        api.saveFrame(productVariantId = 42)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertTrue(request.path!!.contains("saved-frames/42"))
        // PUT has no request body
        assertEquals("", request.body.readUtf8())
    }

    @Test
    fun `saveFrame decodes single resource response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.savedFrameSaveResponse))

        val response = api.saveFrame(productVariantId = 42)

        assertEquals(42, response.data.productVariantId)
        assertEquals("available", response.data.availability)
    }

    @Test
    fun `removeFrame sends DELETE and accepts 204`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))

        val response = api.removeFrame(productVariantId = 42)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertTrue(request.path!!.contains("saved-frames/42"))
        assertEquals(204, response.code())
        assertTrue(response.isSuccessful)
    }

    @Test
    fun `removeFrame accepts 200 for idempotent delete`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200))

        val response = api.removeFrame(productVariantId = 42)

        assertTrue(response.isSuccessful)
    }
}
