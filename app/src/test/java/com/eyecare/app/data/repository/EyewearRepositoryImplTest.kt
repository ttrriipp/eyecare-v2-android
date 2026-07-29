package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.EyewearApiService
import com.eyecare.app.data.remote.dto.EyewearDtos
import com.eyecare.app.data.remote.dto.PaginationMeta
import com.eyecare.app.domain.model.EyewearProgress
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.coEvery
import io.mockk.mockk
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
import java.math.BigDecimal

class EyewearRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: EyewearRepositoryImpl
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
        repository = EyewearRepositoryImpl(retrofit.create(EyewearApiService::class.java))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getEyewear with current filter returns paginated summaries`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":[{"key":"eyw_test","description":"Frame","created_at":"2026-07-27T10:00:00+08:00",
            "progress":"in_preparation","total_amount":"8000.00","activity_at":"2026-07-27T11:00:00+08:00"}],
            "meta":{"current_page":1,"last_page":2,"per_page":15,"total":20}}
            """.trimIndent(),
        ))

        val result = repository.getEyewear(filter = "current", page = 1).getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(2, result.lastPage)
        assertEquals(20, result.total)
        assertTrue(result.hasMorePages)
        assertEquals(EyewearProgress.IN_PREPARATION, result.data[0].progress)
    }

    @Test
    fun `getEyewearDetail with canonical key returns complete detail`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """
            {"data":{"key":"eyw_test","description":"Frame","created_at":"2026-07-27T10:00:00+08:00",
            "progress":"dispensed","payment_status":"balance_due","total_amount":"8000.00",
            "balance_due":"3000.00","activity_at":"2026-07-29T10:05:00+08:00",
            "estimate":{"subtotal":"8500.00","discount_amount":"500.00","total":"8000.00","items":[]},
            "preparation":{"status":"dispensed","total_amount":"8000.00","items":[]},
            "dispensing":{"status":"dispensed","dispensed_at":"2026-07-29T10:00:00+08:00"},
            "payment_summary":{"status":"partially_paid","total_amount":"8000.00","amount_paid":"5000.00","balance_due":"3000.00","payments":[]}}}
            """.trimIndent(),
        ))

        val detail = repository.getEyewearDetail("eyw_test").getOrThrow()
        assertEquals("eyw_test", detail.key)
        assertEquals(EyewearProgress.DISPENSED, detail.progress)
        assertNotNull(detail.estimate)
        assertNotNull(detail.preparation)
        assertNotNull(detail.dispensing)
        assertNotNull(detail.paymentSummary)
        assertEquals(BigDecimal("3000.00"), detail.balanceDue)
    }

    @Test
    fun `getEyewearDetail with jo_ alias passes key unchanged`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"key":"jo_42","description":"Job Order","created_at":"2026-07-27T10:00:00+08:00",
            "progress":"in_preparation","total_amount":"5000.00","activity_at":"2026-07-27T11:00:00+08:00"}}""".trimIndent(),
        ))

        val detail = repository.getEyewearDetail("jo_42").getOrThrow()
        assertEquals("jo_42", detail.key)

        val request = server.takeRequest()
        assertEquals("/eyewear/jo_42", request.path)
    }

    @Test
    fun `estimate-only detail decodes correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"key":"eyw_est","description":"Estimate","created_at":"2026-07-27T10:00:00+08:00",
            "progress":"estimate_available","total_amount":"5000.00","activity_at":"2026-07-27T10:00:00+08:00",
            "estimate":{"total":"5000.00","items":[]}}}""".trimIndent(),
        ))

        val detail = repository.getEyewearDetail("eyw_est").getOrThrow()
        assertNotNull(detail.estimate)
        assertNull(detail.preparation)
        assertNull(detail.dispensing)
        assertNull(detail.paymentSummary)
    }
}
