package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.OrderApiService
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.OrderStatus
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class OrderRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OrderRepositoryImpl
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
        repository = OrderRepositoryImpl(retrofit.create(OrderApiService::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getOrders maps list correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":[{"id":1,"order_number":"ORD-001","appointment_id":null,
            "is_non_prescription":true,"status":"requested","subtotal":"165.00",
            "total_amount":"165.00","items":[],"created_at":"2026-10-24T10:00:00Z"}]}
        """.trimIndent()))

        val result = repository.getOrders()
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().size)
        assertEquals("ORD-001", result.getOrThrow()[0].orderNumber)
        assertEquals(OrderStatus.REQUESTED, result.getOrThrow()[0].status)
    }

    @Test
    fun `getOrder maps single order with items`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":1,"order_number":"ORD-001","appointment_id":null,
            "is_non_prescription":false,"status":"confirmed","subtotal":"165.00",
            "total_amount":"165.00","created_at":"2026-10-24T10:00:00Z",
            "items":[{"id":1,"product_variant_id":1,"lens_type_id":1,"product_id":1,
            "product_name":"Clubmaster","variant_name":"Black","variant_sku":"BK-001",
            "lens_type_name":"Single Vision","unit_price":"165.00","quantity":1,"subtotal":"165.00"}]}}
        """.trimIndent()))

        val result = repository.getOrder(1)
        assertTrue(result.isSuccess)
        val order = result.getOrThrow()
        assertEquals(OrderStatus.CONFIRMED, order.status)
        assertEquals(1, order.items.size)
        assertEquals("Clubmaster", order.items[0].productName)
    }

    @Test
    fun `createOrder returns created order`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""
            {"data":{"id":2,"order_number":"ORD-002","appointment_id":null,
            "is_non_prescription":true,"status":"requested","subtotal":"165.00",
            "total_amount":"165.00","items":[],"created_at":"2026-10-24T10:00:00Z"}}
        """.trimIndent()))

        val result = repository.createOrder(
            appointmentId = null,
            isNonPrescription = true,
            items = listOf(OrderDtos.OrderItemRequest(1, 1, 1)),
        )
        assertTrue(result.isSuccess)
        assertEquals("ORD-002", result.getOrThrow().orderNumber)
    }

    @Test
    fun `createOrder 422 maps to ValidationError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""
            {"message":"Invalid items","errors":{"items":["Items are required."]}}
        """.trimIndent()))

        val result = repository.createOrder(null, false, emptyList())
        assertTrue(result.isFailure)
        assertInstanceOf(
            com.eyecare.app.domain.model.OrderError.ValidationError::class.java,
            result.exceptionOrNull()
        )
    }
}
