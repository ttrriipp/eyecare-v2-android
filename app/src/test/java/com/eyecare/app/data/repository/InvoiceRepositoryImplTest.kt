package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.InvoiceApiService
import com.eyecare.app.domain.model.InvoiceStatus
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

class InvoiceRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: InvoiceRepositoryImpl
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
        repository = InvoiceRepositoryImpl(retrofit.create(InvoiceApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getInvoices returns paginated list with items and payments`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":[{"id":1,"invoice_number":"INV-001","status":"partially_paid",
            "total":8000.0,"amount_paid":5000.0,"balance_due":3000.0,
            "items":[{"id":1,"description":"Frame","quantity":1,"unit_price":4500.0,"amount":4500.0}],
            "payments":[{"id":1,"amount":5000.0,"payment_method":"gcash","reference_number":"GC-12345","status":"posted"}]}],
            "links":{"first":"?page=1","last":"?page=1","prev":null,"next":null},
            "meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}
            """.trimIndent(),
        ))

        val result = repository.getInvoices().getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(InvoiceStatus.PARTIALLY_PAID, result.data[0].status)
        assertEquals(5000.0, result.data[0].amountPaid)
        assertEquals(3000.0, result.data[0].balanceDue)
        assertEquals(1, result.data[0].items.size)
        assertEquals(1, result.data[0].payments.size)
        assertEquals("gcash", result.data[0].payments[0].paymentMethod)
    }

    @Test
    fun `getInvoice decodes detail with all nullable fields`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":1,"invoice_number":"INV-001","official_number":null,"patient_id":1,
            "job_order_id":1,"encounter_id":1,"status":"paid","sale_type":"retail",
            "sold_to_name":"Ana Reyes","subtotal":8500.0,"discount_amount":500.0,"tax_amount":0.0,
            "total":8000.0,"amount_paid":8000.0,"balance_due":0.0,"notes":null,"recorded_by":1,
            "issued_at":"2026-07-27T15:00:00+08:00","items":[],"payments":[]}}
            """.trimIndent(),
        ))

        val invoice = repository.getInvoice(1).getOrThrow()
        assertEquals(InvoiceStatus.PAID, invoice.status)
        assertNull(invoice.officialNumber)
        assertEquals("Ana Reyes", invoice.soldToName)
        assertEquals(0.0, invoice.balanceDue)
    }

    @Test
    fun `getInvoices decodes all five statuses`() = runTest {
        val statuses = listOf("draft", "issued", "partially_paid", "paid", "voided")
        statuses.forEach { status ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":[{"id":1,"status":"$status","items":[],"payments":[]}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}""",
            ))
            val result = repository.getInvoices().getOrThrow()
            assertEquals(InvoiceStatus.from(status), result.data[0].status)
        }
    }
}
