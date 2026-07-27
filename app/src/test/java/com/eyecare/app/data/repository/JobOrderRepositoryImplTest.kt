package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.JobOrderApiService
import com.eyecare.app.domain.model.JobOrderStatus
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

class JobOrderRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: JobOrderRepositoryImpl
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
        repository = JobOrderRepositoryImpl(retrofit.create(JobOrderApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getJobOrders returns paginated list with items`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":[{"id":1,"job_order_number":"JO-001","status":"in_progress",
            "total_amount":8000.0,"items":[{"id":1,"description":"Frame","quantity":1,"unit_price":4500.0,"amount":4500.0}]}],
            "links":{"first":"?page=1","last":"?page=1","prev":null,"next":null},
            "meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}
            """.trimIndent(),
        ))

        val result = repository.getJobOrders().getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(JobOrderStatus.IN_PROGRESS, result.data[0].status)
        assertEquals(1, result.data[0].items.size)
        assertEquals("Frame", result.data[0].items[0].description)
    }

    @Test
    fun `getJobOrder decodes detail with nullable fields`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"id":1,"job_order_number":"JO-001","patient_id":1,"encounter_id":1,
            "prescription_id":1,"quotation_revision_id":1,"status":"dispensed","total_amount":8000.0,
            "notes":null,"started_at":"2026-07-27T11:00:00+08:00","ready_at":"2026-07-27T14:00:00+08:00",
            "dispensed_at":"2026-07-27T15:00:00+08:00","cancelled_at":null,
            "items":[{"id":1,"job_order_id":1,"description":"Frame","quantity":1,"unit_price":4500.0,
            "amount":4500.0,"product_variant_id":42,"lens_category_id":null}]}}
            """.trimIndent(),
        ))

        val order = repository.getJobOrder(1).getOrThrow()
        assertEquals(JobOrderStatus.DISPENSED, order.status)
        assertEquals(1, order.encounterId)
        assertNull(order.cancelledAt)
        assertEquals(42, order.items[0].productVariantId)
    }

    @Test
    fun `getJobOrders decodes all five statuses`() = runTest {
        val statuses = listOf("queued", "in_progress", "ready_for_dispensing", "dispensed", "cancelled")
        statuses.forEach { status ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":[{"id":1,"status":"$status","items":[]}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}""",
            ))
            val result = repository.getJobOrders().getOrThrow()
            assertEquals(JobOrderStatus.from(status), result.data[0].status)
        }
    }
}
