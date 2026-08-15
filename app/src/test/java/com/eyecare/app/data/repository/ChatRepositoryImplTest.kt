package com.eyecare.app.data.repository

import android.content.Context
import com.eyecare.app.data.remote.ApiContractFixtures
import com.eyecare.app.data.remote.api.ConversationApiService
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class ChatRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: ChatRepositoryImpl
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
        val api = retrofit.create(ConversationApiService::class.java)
        repository = ChatRepositoryImpl(api, context = mockk<Context>(relaxed = true))
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getMessages maps DTOs to domain MessagePage with cursor metadata`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPage1))

        val result = repository.getMessages()

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(2, page.messages.size)
        assertEquals(2, page.messages[0].id)
        assertEquals("Your frame is ready for pickup.", page.messages[0].body)
        assertNull(page.nextCursor)
        assertFalse(page.hasMore)
    }

    @Test
    fun `searchMessages forwards query and cursor to API`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageSearchResults))

        val result = repository.searchMessages("prescription", cursor = null)

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertEquals(1, page.messages.size)
        assertEquals("When will my prescription be ready?", page.messages[0].body)
        assertFalse(page.hasMore)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("q=prescription"))
    }

    @Test
    fun `markMessagesRead returns marked count`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.markReadResponse))

        val result = repository.markMessagesRead()

        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow())
    }

    @Test
    fun `terminal page has hasMore false and nextCursor null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPage1))

        val result = repository.getMessages()

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertFalse(page.hasMore)
        assertNull(page.nextCursor)
    }

    @Test
    fun `non-terminal page has hasMore true and nextCursor non-null`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ApiContractFixtures.messageListPageWithCursor))

        val result = repository.getMessages()

        assertTrue(result.isSuccess)
        val page = result.getOrThrow()
        assertTrue(page.hasMore)
        assertNotNull(page.nextCursor)
        assertEquals(
            "eyJpZCI6NTAsImNyZWF0ZWRfYXQiOiIyMDI2LTA3LTE1VDA4OjAwOjAwKzA4OjAwIn0=",
            page.nextCursor,
        )
    }
}
