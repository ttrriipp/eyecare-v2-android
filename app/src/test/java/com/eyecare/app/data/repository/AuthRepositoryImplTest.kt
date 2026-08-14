package com.eyecare.app.data.repository

import com.eyecare.app.data.local.DeviceIdentityProvider
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.data.remote.api.AuthApiService
import com.eyecare.app.domain.model.LoginOutcome
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
    fun `beginLogin trusted device returns Authenticated`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"step_up_required":false,"token":"tok123","user":{"id":1,"name":"Jane","email":"jane@example.com","role":"patient","link_status":"linked"}}}"""
            )
        )
        val result = repository.beginLogin("+639171234567", "password123", null, null)
        assertTrue(result.isSuccess)
        val outcome = result.getOrThrow()
        assertTrue(outcome is LoginOutcome.Authenticated)
        assertEquals("tok123", (outcome as LoginOutcome.Authenticated).token)
        verify(exactly = 1) { tokenManager.saveToken("tok123") }
    }

    @Test
    fun `beginLogin untrusted device returns OtpRequired`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"step_up_required":true,"challenge_id":"abc","expires_at":"2026-08-01T10:00:00+08:00"}}"""
            )
        )
        val result = repository.beginLogin("+639171234567", "password123", null, null)
        assertTrue(result.isSuccess)
        val outcome = result.getOrThrow()
        assertTrue(outcome is LoginOutcome.OtpRequired)
    }

    @Test
    fun `requestRegistrationOtp sends phone contact type`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"challenge_id":"reg-1","expires_at":"2026-08-01T10:10:00+08:00"}}""",
            ),
        )

        val result = repository.requestRegistrationOtp("9171234567")

        assertTrue(result.isSuccess)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"contact_type\":\"phone\""))
        assertTrue(requestBody.contains("\"contact_value\":\"+639171234567\""))
    }

    @Test
    fun `requestPasswordRecoveryOtp normalizes local phone`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"challenge_id":"recovery-1","expires_at":"2026-08-01T10:10:00+08:00"}}""",
            ),
        )

        val result = repository.requestPasswordRecoveryOtp("9171234567")

        assertTrue(result.isSuccess)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"contact_value\":\"+639171234567\""))
    }

    @Test
    fun `verifyLogin sends device metadata`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"data":{"token":"tok123","user":{"id":1,"name":"Jane","phone":"+639171234567","role":"patient","link_status":"linked"}}}""",
            ),
        )

        val result = repository.verifyLogin("challenge-1", "123456", null, null)

        assertTrue(result.isSuccess)
        val requestBody = server.takeRequest().body.readUtf8()
        assertTrue(requestBody.contains("\"device_name\":\"Test Device\""))
        assertTrue(requestBody.contains("\"installation_id\":\"test-install-id\""))
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
        val account = result.getOrThrow()
        assertEquals("PAT-2026-000001", account.linkedPatient?.patientNumber)
        assertEquals("Jane Doe", account.linkedPatient?.fullName)
    }

    @Test
    fun `logoutCurrent clears token`() = runTest {
        server.enqueue(MockResponse().setResponseCode(204))
        val result = repository.logoutCurrent()
        assertTrue(result.isSuccess)
    }
}
