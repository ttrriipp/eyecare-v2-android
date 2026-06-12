package com.eyecare.app.data.repository

import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.data.remote.dto.AuthDtos
import com.eyecare.app.domain.model.AuthError
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
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var tokenManager: TokenManager

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenManager = mockk(relaxed = true)
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AuthApiService::class.java)
        repository = AuthRepositoryImpl(api, tokenManager, json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `login success maps token and user correctly`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"token":"tok123","user":{"id":1,"name":"Jane","email":"jane@example.com","role":"customer"}}}"""
            )
        )
        val result = repository.login("jane@example.com", "password123")
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Jane", user.name)
        assertEquals("jane@example.com", user.email)
        assertEquals("customer", user.role)
    }

    @Test
    fun `login 422 maps to ValidationError with field messages`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(422).setBody(
                """{"message":"The email field is required.","errors":{"email":["The email field is required."]}}"""
            )
        )
        val result = repository.login("", "")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertInstanceOf(AuthError.ValidationError::class.java, error)
        val ve = error as AuthError.ValidationError
        assertEquals(listOf("The email field is required."), ve.fieldErrors["email"])
    }

    @Test
    fun `login 429 maps to RateLimitError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(429))
        val result = repository.login("jane@example.com", "pass")
        assertTrue(result.isFailure)
        assertInstanceOf(AuthError.RateLimitError::class.java, result.exceptionOrNull())
    }

    @Test
    fun `register success maps user correctly`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"data":{"token":"regTok","user":{"id":2,"name":"Bob","email":"bob@example.com","role":"customer"}}}"""
            )
        )
        val result = repository.register(
            AuthDtos.RegisterRequest("Bob", "bob@example.com", null, "password1", "password1")
        )
        assertTrue(result.isSuccess)
        assertEquals("Bob", result.getOrThrow().name)
    }
}
