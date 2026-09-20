package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AccessoryDtos
import com.eyecare.app.domain.model.AccessoryQuery
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AccessoryApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AccessoryApiService
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(AccessoryApiService::class.java)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun enqueueAccessoryList(vararg ids: Int) {
        val data = ids.joinToString(",") { id ->
            """{"id":$id,"name":"Product $id","slug":"product-$id","images":[],"rating_count":0,"variants":[]}"""
        }
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[$data],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":${ids.size}}}""",
        ))
    }

    @Test
    fun `getAccessories sends correct path and method`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories()
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.contains("accessories"))
    }

    @Test
    fun `getAccessories sends search parameter`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery(search = "lens"))
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("search=lens"))
    }

    @Test
    fun `getAccessories sends sort parameter`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery(sort = "rating"))
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("sort=rating"))
    }

    @Test
    fun `getAccessories sends minimum_rating parameter`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery(minimumRating = 4))
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("minimum_rating=4"))
    }

    @Test
    fun `getAccessories sends rated parameter`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery(rated = "rated"))
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("rated=rated"))
    }

    @Test
    fun `getAccessories sends page and per_page`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery(page = 2, perPage = 25))
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("page=2"))
        assertTrue(request.path!!.contains("per_page=25"))
    }

    @Test
    fun `getAccessories does not send brand category or placement`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories(query = AccessoryQuery())
        val request = server.takeRequest()
        val path = request.path!!
        assertFalse(path.contains("brand"))
        assertFalse(path.contains("category"))
        assertFalse(path.contains("placement"))
    }

    @Test
    fun `getAccessoryDetail sends correct path with ID`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":42,"name":"X","slug":"x","images":[],"rating_count":0,"variants":[]}}""",
        ))
        api.getAccessory(42)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.contains("accessories/42"))
    }

    @Test
    fun `getAccessories with default query sends no optional parameters`() = runTest {
        enqueueAccessoryList(1)
        api.getAccessories()
        val request = server.takeRequest()
        val path = request.path!!
        // Should only have base path, no query params for defaults
        assertNull(request.requestUrl?.queryParameter("search"))
        assertNull(request.requestUrl?.queryParameter("minimum_rating"))
        assertNull(request.requestUrl?.queryParameter("rated"))
    }

    private fun assertFalse(condition: Boolean) {
        org.junit.jupiter.api.Assertions.assertFalse(condition)
    }
}