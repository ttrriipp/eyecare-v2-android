package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AppointmentRequestRepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AppointmentRequestRepositoryImpl
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AppointmentRequestApiService::class.java)
        repository = AppointmentRequestRepositoryImpl(api)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getAvailability maps slots correctly`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":30,"slot_duration_minutes":30,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[{"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:30:00+08:00","available":true}]}}"""
        ))
        val result = repository.getAvailability("2026-08-10")
        assertTrue(result.isSuccess)
        val avail = result.getOrThrow()
        assertEquals("2026-08-10", avail.date)
        assertEquals(1, avail.slots.size)
        assertTrue(avail.slots[0].available)
    }

    @Test
    fun `getRequests maps pagination and status`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[{"id":1,"request_number":"APR-2026-000001","status":"pending","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","created_at":"2026-08-09T10:00:00+08:00"}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}"""
        ))
        val result = repository.getRequests()
        assertTrue(result.isSuccess)
        val paginated = result.getOrThrow()
        assertEquals(1, paginated.data.size)
        assertEquals("APR-2026-000001", paginated.data[0].requestNumber)
        assertEquals(com.eyecare.app.domain.model.AppointmentRequestStatus.PENDING, paginated.data[0].status)
    }

    @Test
    fun `createRequest sends only scheduled_at and reason_for_visit`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"data":{"id":2,"request_number":"APR-2026-000002","status":"pending","scheduled_at":"2026-08-10T09:00:00+08:00","reason_for_visit":"Blurry","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))
        val result = repository.createRequest("2026-08-10T09:00:00+08:00", "Blurry")
        assertTrue(result.isSuccess)
        val request = result.getOrThrow()
        assertEquals(2, request.id)
        assertNull(request.appointmentId)
    }

    @Test
    fun `createRequest sends optional identity for an unlinked account`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"data":{"id":3,"request_number":"APR-2026-000003","status":"pending","patient_id":null,"scheduled_at":"2026-08-10T09:00:00+08:00","reason_for_visit":"Blurry","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))

        val result = repository.createRequest(
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurry",
            identity = AppointmentRequestIdentity(
                phone = "+639171234567",
                email = "alex@example.com",
                firstName = "Alex",
                middleName = "M",
                lastName = "Rivera",
                dateOfBirth = "1990-05-15",
                gender = AppointmentRequestGender.FEMALE,
                occupation = "Teacher",
                address = "123 Main St, Manila",
            ),
        )

        assertTrue(result.isSuccess)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"identity\""))
        assertTrue(body.contains("\"first_name\":\"Alex\""))
        assertTrue(body.contains("\"date_of_birth\":\"1990-05-15\""))
        assertTrue(body.contains("\"phone\":\"+639171234567\""))
        assertTrue(body.contains("\"gender\":\"female\""))
        assertTrue(body.contains("\"occupation\":\"Teacher\""))
        assertTrue(body.contains("\"address\":\"123 Main St, Manila\""))
    }

    @Test
    fun `getRequest maps accepted appointment reference`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"request_number":"APR-2026-000001","status":"accepted","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","created_at":"2026-08-09T10:00:00+08:00","appointment":{"id":42}}}"""
        ))
        val result = repository.getRequest(1)
        assertTrue(result.isSuccess)
        assertEquals(42, result.getOrThrow().appointmentId)
    }

    @Test
    fun `cancelRequest returns cancelled status`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"request_number":"APR-2026-000001","status":"cancelled","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","cancelled_at":"2026-08-09T11:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))
        val result = repository.cancelRequest(1)
        assertTrue(result.isSuccess)
        assertEquals(com.eyecare.app.domain.model.AppointmentRequestStatus.CANCELLED, result.getOrThrow().status)
    }
}
