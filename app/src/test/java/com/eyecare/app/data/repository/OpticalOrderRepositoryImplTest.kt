package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.OpticalOrderApiService
import com.eyecare.app.data.remote.dto.OpticalOrderDtos
import com.eyecare.app.domain.model.FulfillmentMode
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit
import java.math.BigDecimal

class OpticalOrderRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: OpticalOrderRepositoryImpl
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
        repository = OpticalOrderRepositoryImpl(retrofit.create(OpticalOrderApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun orderJson(
        id: Int = 1,
        status: String = "in_progress",
        fulfillmentMode: String = "prepared",
        total: String = "5000.00",
        sourceQuotation: String? = """{"id":1,"quotation_number":"Q-001"}""",
        paymentSummary: String? = null,
        items: String = """[{"id":10,"description":"Lens","quantity":1,"unit_price":"4500.00","amount":"4500.00","is_rateable":false}]""",
    ): String {
        val sq = sourceQuotation ?: "null"
        val ps = paymentSummary ?: "null"
        return """{"id":$id,"order_number":"OO-$id","status":"$status","fulfillment_mode":"$fulfillmentMode","total_amount":"$total","created_at":"2026-08-01T10:00:00Z","source_quotation":$sq,"items":$items,"payment_summary":$ps}"""
    }

    private fun enqueueList(vararg orders: String, lastPage: Int = 1, total: Int = orders.size) {
        val data = orders.joinToString(",")
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[$data],"meta":{"current_page":1,"last_page":$lastPage,"per_page":15,"total":$total}}""",
        ))
    }

    private fun enqueueSingle(json: String) {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":$json}"""))
    }

    @Test
    fun `getOpticalOrders returns paginated list`() = runTest {
        enqueueList(orderJson())
        val result = repository.getOpticalOrders().getOrThrow()
        assertEquals(1, result.data.size)
        val o = result.data[0]
        assertEquals(OpticalOrderStatus.IN_PROGRESS, o.status)
        assertEquals(FulfillmentMode.PREPARED, o.fulfillmentMode)
        assertEquals(BigDecimal("5000.00"), o.totalAmount)
    }

    @Test
    fun `getOpticalOrders sends current filter`() = runTest {
        enqueueList(orderJson())
        repository.getOpticalOrders(filter = "current").getOrThrow()
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("filter=current"))
    }

    @Test
    fun `getOpticalOrders sends history filter`() = runTest {
        enqueueList(orderJson(status = "dispensed"))
        repository.getOpticalOrders(filter = "history").getOrThrow()
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("filter=history"))
    }

    @Test
    fun `getOpticalOrder passes ID unchanged`() = runTest {
        enqueueSingle(orderJson(id = 42))
        val o = repository.getOpticalOrder(42).getOrThrow()
        assertEquals(42, o.id)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("optical-orders/42"))
    }

    @Test
    fun `maps all order statuses`() = runTest {
        listOf("queued", "in_progress", "ready_for_dispensing", "dispensed", "cancelled").forEach { status ->
            enqueueSingle(orderJson(status = status))
            val o = repository.getOpticalOrder(1).getOrThrow()
            assertEquals(OpticalOrderStatus.from(status), o.status)
        }
    }

    @Test
    fun `unknown status maps to UNKNOWN`() = runTest {
        enqueueSingle(orderJson(status = "future_status"))
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertEquals(OpticalOrderStatus.UNKNOWN, o.status)
    }

    @Test
    fun `source quotation maps to domain reference`() = runTest {
        enqueueSingle(orderJson())
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertEquals(1, o.sourceQuotation?.id)
        assertEquals("Q-001", o.sourceQuotation?.quotationNumber)
    }

    @Test
    fun `null source quotation maps to null`() = runTest {
        enqueueSingle(orderJson(sourceQuotation = "null"))
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertNull(o.sourceQuotation)
    }

    @Test
    fun `payment summary maps correctly`() = runTest {
        enqueueSingle(orderJson(paymentSummary = """{"status":"partially_paid","total_amount":"5000.00","amount_paid":"2000.00","balance_due":"3000.00","payment_due_date":"2026-09-01","is_overdue":false}"""))
        val o = repository.getOpticalOrder(1).getOrThrow()
        val ps = o.paymentSummary!!
        assertEquals(PaymentStatus.PARTIALLY_PAID, ps.status)
        assertEquals(BigDecimal("3000.00"), ps.balanceDue)
        assertEquals("2026-09-01", ps.paymentDueDate)
        assertTrue(!ps.isOverdue)
    }

    @Test
    fun `null payment summary maps to null`() = runTest {
        enqueueSingle(orderJson(paymentSummary = "null"))
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertNull(o.paymentSummary)
    }

    @Test
    fun `is_rateable passes through unchanged`() = runTest {
        enqueueSingle(orderJson(items = """[{"id":20,"description":"Frame","quantity":1,"unit_price":"3000.00","amount":"3000.00","product_variant_id":5,"is_rateable":true,"rating":{"rating":4,"comment":"Good","created_at":"2026-08-05T00:00:00Z"}}]"""))
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertTrue(o.items[0].isRateable)
        assertEquals(4, o.items[0].rating?.rating)
        assertEquals(5, o.items[0].productVariantId)
    }

    @Test
    fun `item with absent rateable fields defaults safely`() = runTest {
        enqueueSingle(orderJson(items = """[{"id":21,"description":"Lens","quantity":1,"unit_price":"1000.00","amount":"1000.00"}]"""))
        val o = repository.getOpticalOrder(1).getOrThrow()
        assertFalse(o.items[0].isRateable)
        assertNull(o.items[0].rating)
        assertNull(o.items[0].productVariantId)
    }

    @Test
    fun `preserves server ordering`() = runTest {
        enqueueList(
            orderJson(id = 3, status = "queued"),
            orderJson(id = 1, status = "queued"),
            lastPage = 2, total = 2,
        )
        val result = repository.getOpticalOrders().getOrThrow()
        assertEquals(3, result.data[0].id)
        assertEquals(1, result.data[1].id)
        assertEquals(2, result.lastPage)
    }
}
