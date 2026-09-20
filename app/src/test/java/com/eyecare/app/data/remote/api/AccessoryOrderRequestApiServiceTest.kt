package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.dto.AccessoryOrderRequestDtos
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

class AccessoryOrderRequestApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: AccessoryOrderRequestApiService
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
        api = retrofit.create(AccessoryOrderRequestApiService::class.java)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun requestJson(id: Int = 1, status: String = "pending") =
        """{"id":$id,"request_number":"ORQ-$id","status":"$status","subtotal_amount":"100.00","requested_discount_type":"none","items":[],"created_at":"2026-09-20T10:00:00+08:00"}"""

    @Test
    fun `getRequests sends correct path and filter`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":0}}""",
        ))
        api.getRequests(filter = "current", page = 1)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.contains("accessory-order-requests"))
        assertTrue(request.path!!.contains("filter=current"))
    }

    @Test
    fun `submitRequest sends POST with body`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""{"data":${requestJson()}}"""))
        val body = AccessoryOrderRequestDtos.SubmitOrderRequest(
            requestedDiscountType = "none",
            items = listOf(AccessoryOrderRequestDtos.SubmitOrderItem(productVariantId = 42, quantity = 2)),
        )
        api.submitRequest(body)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("accessory-order-requests"))
        val bodyStr = request.body.readUtf8()
        assertTrue(bodyStr.contains("42"))
        assertTrue(bodyStr.contains("2"))
    }

    @Test
    fun `getRequest sends correct path with ID`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":${requestJson(42)}}"""))
        api.getRequest(42)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.contains("accessory-order-requests/42"))
    }

    @Test
    fun `cancelRequest sends POST to cancel endpoint`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":${requestJson(status = "cancelled")}}"""))
        api.cancelRequest(10)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.contains("accessory-order-requests/10/cancel"))
    }
}