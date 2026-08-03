package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.QuotationApiService
import com.eyecare.app.domain.model.QuotationItemType
import com.eyecare.app.domain.model.QuotationStatus
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

class QuotationRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: QuotationRepositoryImpl
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
        repository = QuotationRepositoryImpl(retrofit.create(QuotationApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun enqueueQuotation(
        id: Int = 1,
        number: String = "QUO-001",
        status: String = "presented",
        total: String = "8000.00",
        subtotal: String = "8500.00",
        discount: String = "500.00",
        validUntil: String? = "2026-08-03",
        notes: String? = null,
        presentedAt: String? = "2026-07-27T10:05:00+08:00",
        confirmedAt: String? = null,
        opticalOrder: String? = null,
        items: String = """[{"id":10,"item_type":"product","description":"Frame","quantity":1,"unit_price":"4500.00","amount":"4500.00"}]""",
    ) {
        val oo = opticalOrder ?: "null"
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[{"id":$id,"quotation_number":"$number","status":"$status","valid_until":${validUntil?.let { "\"$it\"" } ?: "null"},"subtotal":"$subtotal","discount_amount":"$discount","total":"$total","notes":${notes?.let { "\"$it\"" } ?: "null"},"created_at":"2026-07-27T10:00:00+08:00","presented_at":${presentedAt?.let { "\"$it\"" } ?: "null"},"confirmed_at":${confirmedAt?.let { "\"$it\"" } ?: "null"},"optical_order":$oo,"items":$items}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}""".trimIndent(),
        ))
    }

    @Test
    fun `getQuotations returns paginated list`() = runTest {
        enqueueQuotation()
        val result = repository.getQuotations().getOrThrow()
        assertEquals(1, result.data.size)
        val q = result.data[0]
        assertEquals(QuotationStatus.PRESENTED, q.status)
        assertEquals(BigDecimal("8000.00"), q.total)
        assertEquals(1, q.items.size)
        assertEquals(QuotationItemType.PRODUCT, q.items[0].itemType)
    }

    @Test
    fun `getQuotations sends current filter`() = runTest {
        enqueueQuotation()
        repository.getQuotations(filter = "current").getOrThrow()
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("filter=current"))
    }

    @Test
    fun `getQuotations sends history filter`() = runTest {
        enqueueQuotation(status = "accepted")
        repository.getQuotations(filter = "history").getOrThrow()
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("filter=history"))
    }

    @Test
    fun `getQuotations sends page parameter`() = runTest {
        enqueueQuotation()
        repository.getQuotations(page = 2).getOrThrow()
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("page=2"))
    }

    @Test
    fun `getQuotation decodes with optical_order cross-link`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":2,"quotation_number":"QUO-002","status":"accepted","valid_until":null,"subtotal":"500.00","discount_amount":"0.00","total":"500.00","notes":"Confirmed","created_at":"2026-07-27T10:00:00+08:00","presented_at":"2026-07-27T10:00:00+08:00","confirmed_at":"2026-07-28T14:00:00+08:00","optical_order":{"id":5,"order_number":"OO-005"},"items":[{"id":20,"item_type":"service","description":"Fitting","quantity":1,"unit_price":"500.00","amount":"500.00"}]}}""".trimIndent(),
        ))

        val q = repository.getQuotation(2).getOrThrow()
        assertEquals(QuotationStatus.ACCEPTED, q.status)
        assertEquals("2026-07-28T14:00:00+08:00", q.confirmedAt)
        val orderRef = q.opticalOrder!!
        assertEquals(5, orderRef.id)
        assertEquals("OO-005", orderRef.orderNumber)
    }

    @Test
    fun `getQuotation passes ID unchanged`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":42,"quotation_number":"Q-42","status":"presented","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]}}""".trimIndent(),
        ))
        val q = repository.getQuotation(42).getOrThrow()
        assertEquals(42, q.id)
        val request = server.takeRequest()
        assertTrue(request.path!!.contains("quotations/42"))
    }

    @Test
    fun `getQuotation maps all status values`() = runTest {
        val statuses = listOf("presented", "accepted", "declined", "expired")
        statuses.forEach { status ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"id":1,"quotation_number":"Q-001","status":"$status","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]}}""",
            ))
            val q = repository.getQuotation(1).getOrThrow()
            assertEquals(QuotationStatus.from(status), q.status)
        }
    }

    @Test
    fun `unknown status maps to UNKNOWN`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"quotation_number":"Q-001","status":"future_status","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]}}""",
        ))
        val q = repository.getQuotation(1).getOrThrow()
        assertEquals(QuotationStatus.UNKNOWN, q.status)
    }

    @Test
    fun `unknown item type maps to UNKNOWN`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"quotation_number":"Q-001","status":"presented","subtotal":"100.00","discount_amount":"0.00","total":"100.00","created_at":"2026-08-01T00:00:00Z","items":[{"id":1,"item_type":"future_type","description":"X","quantity":1,"unit_price":"100.00","amount":"100.00"}]}}""",
        ))
        val q = repository.getQuotation(1).getOrThrow()
        assertEquals(QuotationItemType.UNKNOWN, q.items[0].itemType)
    }

    @Test
    fun `preserves server ordering`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[{"id":3,"quotation_number":"Q-3","status":"presented","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]},{"id":1,"quotation_number":"Q-1","status":"presented","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-07-01T00:00:00Z","items":[]}],"meta":{"current_page":1,"last_page":2,"per_page":15,"total":2}}""".trimIndent(),
        ))
        val result = repository.getQuotations().getOrThrow()
        assertEquals(3, result.data[0].id)
        assertEquals(1, result.data[1].id)
        assertEquals(2, result.lastPage)
    }

    @Test
    fun `null optical_order maps to null domain reference`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"quotation_number":"Q-001","status":"presented","subtotal":"0.00","discount_amount":"0.00","total":"0.00","created_at":"2026-08-01T00:00:00Z","items":[]}}""",
        ))
        val q = repository.getQuotation(1).getOrThrow()
        assertNull(q.opticalOrder)
    }
}
