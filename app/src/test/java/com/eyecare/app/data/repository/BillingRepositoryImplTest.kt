package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.BillingApiService
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class BillingRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: BillingRepositoryImpl

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        repository = BillingRepositoryImpl(retrofit.create(BillingApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getBilling maps optional notes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(billingJson("Insurance claim pending")))

        val billing = repository.getBilling(1).getOrThrow()

        assertEquals("Insurance claim pending", billing.notes)
    }

    @Test
    fun `getBilling accepts response without notes`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(billingJson()))

        val billing = repository.getBilling(1).getOrThrow()

        assertNull(billing.notes)
    }

    private fun billingJson(notes: String? = null): String {
        val notesField = notes?.let { ",\"notes\":\"$it\"" }.orEmpty()
        return """
            {"data":{"id":1,"billing_number":"BIL-2026-000001","status":"issued",
            "subtotal":"165.00","discount_amount":"0.00","total_amount":"165.00",
            "amount_paid":"0.00","balance_due":"165.00","created_at":"2026-10-24T10:00:00Z",
            "items":[],"payments":[]$notesField}}
        """.trimIndent()
    }
}
