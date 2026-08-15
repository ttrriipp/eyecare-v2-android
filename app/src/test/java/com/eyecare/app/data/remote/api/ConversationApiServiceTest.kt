package com.eyecare.app.data.remote.api

import com.eyecare.app.data.remote.ApiContractFixtures
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class ConversationApiServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ConversationApiService
    private val json: Json = ApiContractFixtures.json

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        api = retrofit.create(ConversationApiService::class.java)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getMessages omits cursor query parameter when null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPage1))

        api.getMessages(cursor = null)

        val request = server.takeRequest()
        assertNotNull(request.path)
        assertFalse(request.path!!.contains("cursor"))
    }

    @Test
    fun `getMessages sends cursor query parameter when non-null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPage1))

        api.getMessages(cursor = "abc123")

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("cursor=abc123"))
    }

    @Test
    fun `searchMessages sends both q and cursor parameters`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageSearchResults))

        api.searchMessages(query = "prescription", cursor = "abc123")

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("q=prescription"))
        assertTrue(request.path!!.contains("cursor=abc123"))
    }

    @Test
    fun `searchMessages sends q without cursor on first page`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageSearchResults))

        api.searchMessages(query = "prescription", cursor = null)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("q=prescription"))
        assertFalse(request.path!!.contains("cursor"))
    }

    @Test
    fun `markMessagesRead uses POST method and correct path`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.markReadResponse))

        api.markMessagesRead()

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/conversation/messages/read"))
    }

    @Test
    fun `getMessages decodes cursor metadata from response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPageWithCursor))

        val response = api.getMessages(cursor = null)

        assertEquals(1, response.data.size)
        assertEquals(50, response.data[0].id)
        assertNotNull(response.meta.nextCursor)
        assertTrue(response.meta.hasMore)
        assertEquals(
            "eyJpZCI6NTAsImNyZWF0ZWRfYXQiOiIyMDI2LTA3LTE1VDA4OjAwOjAwKzA4OjAwIn0=",
            response.meta.nextCursor,
        )
    }
}
