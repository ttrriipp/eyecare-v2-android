package com.eyecare.app.data.remote.interceptor

import app.cash.turbine.test
import com.eyecare.app.data.local.TokenManager
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthInterceptorTest {

    private lateinit var server: MockWebServer
    private lateinit var tokenManager: TokenManager
    private lateinit var authEventBus: AuthEventBus
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenManager = mockk()
        authEventBus = AuthEventBus()
        client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager, authEventBus))
            .build()
    }

    @AfterEach
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `adds Authorization header when token exists`() {
        every { tokenManager.getToken() } returns "test-token"
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        val recorded = server.takeRequest()
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"))
    }

    @Test
    fun `skips Authorization header when token is absent`() {
        every { tokenManager.getToken() } returns null
        server.enqueue(MockResponse().setResponseCode(200))

        client.newCall(Request.Builder().url(server.url("/")).build()).execute()

        val recorded = server.takeRequest()
        assertNull(recorded.getHeader("Authorization"))
    }

    @Test
    fun `emits logout event on 401 response`() = runTest {
        every { tokenManager.getToken() } returns "expired-token"
        server.enqueue(MockResponse().setResponseCode(401))

        authEventBus.events.test {
            client.newCall(Request.Builder().url(server.url("/")).build()).execute()
            assertEquals(AuthEvent.Logout, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
