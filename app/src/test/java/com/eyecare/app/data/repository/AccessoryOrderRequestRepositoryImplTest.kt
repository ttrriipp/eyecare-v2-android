package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AccessoryOrderRequestApiService
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
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
import retrofit2.Retrofit
import java.math.BigDecimal

class AccessoryOrderRequestRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AccessoryOrderRequestRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = AccessoryOrderRequestRepositoryImpl(retrofit.create(AccessoryOrderRequestApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun requestJson(id: Int = 1, status: String = "pending") =
        """{"id":$id,"request_number":"ORQ-$id","status":"$status","subtotal_amount":"100.00","requested_discount_type":"none","items":[],"created_at":"2026-09-20T10:00:00+08:00"}"""

    private fun enqueueSingle(json: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":$json}"""))
    }

    private fun enqueueList(vararg requests: String, lastPage: Int = 1) {
        val data = requests.joinToString(",")
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[$data],"meta":{"current_page":1,"last_page":$lastPage,"per_page":15,"total":${requests.size}}}""",
        ))
    }

    @Test
    fun `getRequests returns paginated list`() = runTest {
        enqueueList(requestJson())
        val result = repository.getRequests(OrderRequestFilter.CURRENT).getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(1, result.data[0].id)
        assertEquals(OrderRequestStatus.PENDING, result.data[0].status)
    }

    @Test
    fun `getRequest returns single request`() = runTest {
        enqueueSingle(requestJson(id = 42))
        val request = repository.getRequest(42).getOrThrow()
        assertEquals(42, request.id)
        val req = server.takeRequest()
        assertTrue(req.path!!.contains("accessory-order-requests/42"))
    }

    @Test
    fun `submitRequest sends correct body and maps response`() = runTest {
        enqueueSingle(requestJson(status = "pending"))
        val result = repository.submitRequest("none", listOf(42 to 2)).getOrThrow()
        assertEquals(OrderRequestStatus.PENDING, result.status)
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        val body = req.body.readUtf8()
        assertTrue(body.contains("42"))
        assertTrue(body.contains("2"))
    }

    @Test
    fun `cancelRequest sends POST and maps response`() = runTest {
        enqueueSingle(requestJson(status = "cancelled"))
        val result = repository.cancelRequest(10).getOrThrow()
        assertEquals(OrderRequestStatus.CANCELLED, result.status)
        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertTrue(req.path!!.contains("cancel"))
    }

    @Test
    fun `unknown status maps to UNKNOWN`() = runTest {
        enqueueSingle(requestJson(status = "future_status"))
        val result = repository.getRequest(1).getOrThrow()
        assertEquals(OrderRequestStatus.UNKNOWN, result.status)
    }

    @Test
    fun `preserves server ordering`() = runTest {
        enqueueList(requestJson(id = 3), requestJson(id = 1), lastPage = 2)
        val result = repository.getRequests(OrderRequestFilter.CURRENT).getOrThrow()
        assertEquals(3, result.data[0].id)
        assertEquals(1, result.data[1].id)
        assertEquals(2, result.lastPage)
    }

    @Test
    fun `404 maps to ApiDomainError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not found"}"""))
        val result = repository.getRequest(999)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ApiDomainError)
    }
}