package com.eyecare.app.data.repository

import com.eyecare.app.data.local.dao.FrameDao
import com.eyecare.app.data.remote.api.FrameApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class FrameRepositoryReviewsTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: FrameRepositoryImpl

    @BeforeEach
    fun setUp() {
        server = MockWebServer()
        server.start()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FrameApiService::class.java)
        repository = FrameRepositoryImpl(api, mockk<FrameDao>(), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getFrameReviews parses the public review page without expecting reviewer identity`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":[{"rating":4,"comment":"Lightweight and comfortable.","created_at":"2026-09-18T08:30:00+08:00"}],"links":{"first":"/frames/19/reviews?page=1","last":"/frames/19/reviews?page=1","prev":null,"next":null},"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}""",
            ),
        )

        val result = repository.getFrameReviews(19).getOrThrow()

        assertEquals(4, result.data.single().rating)
        assertEquals("Lightweight and comfortable.", result.data.single().comment)
        assertEquals("2026-09-18T08:30:00+08:00", result.data.single().createdAt)
        assertEquals(1, result.total)
        val request = server.takeRequest()
        assertEquals("/frames/19/reviews", request.requestUrl?.encodedPath)
        assertEquals("1", request.requestUrl?.queryParameter("page"))
        assertEquals("15", request.requestUrl?.queryParameter("per_page"))
    }
}
