package com.eyecare.app.data.repository

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
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

class AuthRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AuthRepositoryImpl
    private lateinit var tokenManager: TokenManager
    private lateinit var deviceIdentityProvider: DeviceIdentityProvider

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        tokenManager = mockk(relaxed = true)
        deviceIdentityProvider = mockk {
            every { getOrCreateInstallationId() } returns "test-install-id"
            every { deviceName() } returns "Test Device"
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AuthApiService::class.java)
        repository = AuthRepositoryImpl(api, tokenManager, deviceIdentityProvider)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `login trusted device returns user and saves token`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"step_up_required":false,"token":"tok123","user":{"id":1,"name":"Jane","email":"jane@example.com","role":"patient","link_status":"linked","linked_patient":{"patient_number":"PAT-2026-000001","full_name":"Jane Doe"}}}}"""
            )
        )
        val result = repository.login("jane@example.com", "password123")
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("Jane", user.name)
        assertEquals("jane@example.com", user.email)
        assertEquals("PAT-2026-000001", user.patientNumber)
    }

    @Test
    fun `login untrusted device throws OTP required`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"step_up_required":true,"challenge_id":"abc","expires_at":"2026-08-01T10:00:00+08:00"}}"""
            )
        )
        val result = repository.login("jane@example.com", "password123")
        assertTrue(result.isFailure)
    }

    @Test
    fun `getMe returns mapped account`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"id":1,"name":"Jane","email":"jane@example.com","role":"patient","link_status":"linked","linked_patient":{"patient_number":"PAT-2026-000001","full_name":"Jane Doe"}}}"""
            )
        )
        val result = repository.getMe()
        assertTrue(result.isSuccess)
        val user = result.getOrThrow()
        assertEquals("PAT-2026-000001", user.linkedPatient?.patientNumber)
        assertEquals("Jane Doe", user.linkedPatient?.fullName)
    }

    @Test
    fun `logout clears token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val result = repository.logout()
        assertTrue(result.isSuccess)
    }
}
