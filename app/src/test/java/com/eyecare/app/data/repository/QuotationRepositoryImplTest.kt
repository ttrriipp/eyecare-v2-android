package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.QuotationApiService
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

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

    @Test
    fun `getQuotations returns paginated list`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":[{"id":1,"quotation_number":"QUO-001","status":"presented",
            "valid_until":"2026-08-03","notes":null,
            "revision":{"revision_number":1,"subtotal":8500.0,"discount_amount":500.0,"total":8000.0,
            "items":[{"description":"Frame","quantity":1,"unit_price":4500.0,"amount":4500.0}]},"created_at":"2026-07-27T10:00:00+08:00"}],
            "links":{"first":"?page=1","last":"?page=1","prev":null,"next":null},
            "meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}
            """.trimIndent(),
        ))

        val result = repository.getQuotations().getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(QuotationStatus.PRESENTED, result.data[0].status)
        assertNotNull(result.data[0].revision)
        assertEquals(8000.0, result.data[0].revision!!.total)
        assertEquals(1, result.data[0].revision!!.items.size)
    }

    @Test
    fun `getQuotation decodes null revision`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":2,"quotation_number":"QUO-002","status":"draft",
            "valid_until":null,"notes":null,"revision":null,"created_at":"2026-07-27T10:00:00+08:00"}}
            """.trimIndent(),
        ))

        val quotation = repository.getQuotation(2).getOrThrow()
        assertEquals(QuotationStatus.DRAFT, quotation.status)
        assertNull(quotation.revision)
    }

    @Test
    fun `getQuotation decodes all five statuses`() = runTest {
        val statuses = listOf("draft", "presented", "accepted", "declined", "expired")
        statuses.forEach { status ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"id":1,"status":"$status","revision":null}}""",
            ))
            val q = repository.getQuotation(1).getOrThrow()
            assertEquals(QuotationStatus.from(status), q.status)
        }
    }
}
