package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentApiService
import com.eyecare.app.domain.model.AppointmentStatus
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
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

class AppointmentRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AppointmentRepositoryImpl
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
        repository = AppointmentRepositoryImpl(retrofit.create(AppointmentApiService::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getAppointments maps list correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":[{"id":1,"visit_reason":"eye_exam","status":"pending",
            "scheduled_at":"2026-10-24T10:00:00Z","contact_notes":"test","staff_notes":null}]}
        """.trimIndent()))

        val result = repository.getAppointments()
        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(1, list.size)
        assertEquals("eye_exam", list[0].visitReason)
        assertEquals(AppointmentStatus.PENDING, list[0].status)
    }

    @Test
    fun `getAppointment maps single item correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":2,"visit_reason":"follow_up","status":"confirmed",
            "scheduled_at":"2026-10-25T14:00:00Z","contact_notes":null,"staff_notes":"All good"}}
        """.trimIndent()))

        val result = repository.getAppointment(2)
        assertTrue(result.isSuccess)
        val appt = result.getOrThrow()
        assertEquals(2, appt.id)
        assertEquals(AppointmentStatus.CONFIRMED, appt.status)
        assertEquals("All good", appt.staffNotes)
    }

    @Test
    fun `createAppointment returns created appointment`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""
            {"data":{"id":3,"visit_reason":"prescription_check","status":"pending",
            "scheduled_at":"2026-10-26T09:00:00Z","contact_notes":"please confirm","staff_notes":null}}
        """.trimIndent()))

        val result = repository.createAppointment("prescription_check", "2026-10-26T09:00:00Z", "please confirm")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().id)
    }

    @Test
    fun `createAppointment 422 maps to ValidationError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""
            {"message":"Invalid date","errors":{"scheduled_at":["The scheduled at field is required."]}}
        """.trimIndent()))

        val result = repository.createAppointment("eye_exam", "", null)
        assertTrue(result.isFailure)
        assertInstanceOf(
            com.eyecare.app.domain.model.AppointmentError.ValidationError::class.java,
            result.exceptionOrNull()
        )
    }
}
