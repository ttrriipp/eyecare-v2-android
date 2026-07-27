package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.JobOrderApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class FrameRatingRepositoryTest {

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
        repository = JobOrderRepositoryImpl(retrofit.create(JobOrderApiService::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `submitRating maps complete revision history`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """
            {"data":{"id":1,"patient_id":1,"product_variant_id":42,"dispensing_event_id":1,
            "rating":5,"comment":"Perfect fit!","current_revision_id":1,"is_hidden":false,
            "moderation_reason":null,"revisions":[
              {"id":1,"frame_rating_id":1,"revision_number":1,"rating":5,"comment":"Perfect fit!",
               "revised_by":5,"revised_at":"2026-07-27T10:00:00+08:00"}
            ]}}
            """.trimIndent(),
        ))

        val result = repository.submitRating(1, 42, 5, "Perfect fit!", 1).getOrThrow()
        assertEquals(1, result.id)
        assertEquals(42, result.productVariantId)
        assertEquals(5, result.rating)
        assertEquals("Perfect fit!", result.comment)
        assertEquals(1, result.revisions.size)
        assertEquals(1, result.revisions[0].revisionNumber)
        assertEquals(5, result.revisions[0].rating)
    }

    @Test
    fun `submitRating maps revision with incremented revision number`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """
            {"data":{"id":1,"patient_id":1,"product_variant_id":42,
            "rating":4,"comment":"Updated comment","current_revision_id":2,"is_hidden":false,
            "moderation_reason":null,"revisions":[
              {"id":1,"frame_rating_id":1,"revision_number":1,"rating":5,"comment":"Initial",
               "revised_by":5,"revised_at":"2026-07-27T10:00:00+08:00"},
              {"id":2,"frame_rating_id":1,"revision_number":2,"rating":4,"comment":"Updated comment",
               "revised_by":5,"revised_at":"2026-07-27T11:00:00+08:00"}
            ]}}
            """.trimIndent(),
        ))

        val result = repository.submitRating(1, 42, 4, "Updated comment").getOrThrow()
        assertEquals(2, result.currentRevisionId)
        assertEquals(2, result.revisions.size)
        assertEquals(2, result.revisions[1].revisionNumber)
    }

    @Test
    fun `submitRating 403 preserves error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(403).setBody(
            """{"message":"This action is unauthorized."}""",
        ))

        val result = repository.submitRating(1, 42, 5)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as FrameRatingError
        assertEquals(403, error.httpCode)
    }

    @Test
    fun `submitRating 404 preserves error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404).setBody(
            """{"message":"No query results for model [App\\Models\\JobOrderItem] 999"}""",
        ))

        val result = repository.submitRating(999, 42, 5)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as FrameRatingError
        assertEquals(404, error.httpCode)
    }

    @Test
    fun `submitRating 422 preserves validation errors`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody(
            """{"message":"The given data was invalid.","errors":{"rating":["The rating must be between 1 and 5."]}}""",
        ))

        val result = repository.submitRating(1, 42, 0)
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull() as FrameRatingError
        assertEquals(422, error.httpCode)
        assertEquals("The rating must be between 1 and 5.", error.fieldErrors?.get("rating")?.first())
    }
}
