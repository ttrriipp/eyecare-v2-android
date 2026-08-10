package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentRequestApiService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentRequestStatus
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
    fun `getAppointmentTypes maps to domain`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[{"id":1,"name":"First eye examination","description":"For your first examination.","duration_minutes":45,"requires_referral":false},{"id":4,"name":"Referral","description":null,"duration_minutes":45,"requires_referral":true}]}"""
        ))
        val result = repository.getAppointmentTypes()
        assertTrue(result.isSuccess)
        val types = result.getOrThrow()
        assertEquals(2, types.size)
        assertEquals(1, types[0].id)
        assertEquals("First eye examination", types[0].name)
        assertEquals(45, types[0].durationMinutes)
        assertEquals("For your first examination.", types[0].description)
        assertTrue(types[1].requiresReferral)
        assertNull(types[1].description)
    }

    @Test
    fun `getAvailability sends date and appointment_type_id`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[{"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true}]}}"""
        ))
        val result = repository.getAvailability("2026-08-10", 1)
        assertTrue(result.isSuccess)
        val avail = result.getOrThrow()
        assertEquals("2026-08-10", avail.date)
        assertEquals(15, avail.intervalMinutes)
        assertEquals(45, avail.visitDurationMinutes)
        assertEquals(1, avail.appointmentTypeId)
        assertEquals(1, avail.slots.size)
        assertTrue(avail.slots[0].available)

        val request = server.takeRequest()
        assertTrue(request.path!!.contains("date=2026-08-10"))
        assertTrue(request.path!!.contains("appointment_type_id=1"))
    }

    @Test
    fun `getRequests maps expanded fields`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":[{"id":1,"request_number":"APR-2026-000001","status":"pending","appointment_type":{"id":1,"name":"First eye examination","duration_minutes":45},"scheduled_at":"2026-08-10T10:00:00+08:00","alternative_scheduled_times":["2026-08-10T14:00:00+08:00"],"provisional_duration_minutes":45,"reason_for_visit":"Test","time_preferences_are_reserved":false,"created_at":"2026-08-09T10:00:00+08:00"}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}"""
        ))
        val result = repository.getRequests()
        assertTrue(result.isSuccess)
        val request = result.getOrThrow().data[0]
        assertEquals("APR-2026-000001", request.requestNumber)
        assertEquals(AppointmentRequestStatus.PENDING, request.status)
        assertEquals(1, request.appointmentType?.id)
        assertEquals("First eye examination", request.appointmentType?.name)
        assertEquals(45, request.appointmentType?.durationMinutes)
        assertEquals(listOf("2026-08-10T14:00:00+08:00"), request.alternativeScheduledTimes)
        assertEquals(45, request.provisionalDurationMinutes)
        assertTrue(request.timePreferencesAreReserved.not())
    }

    @Test
    fun `createRequest sends type ID, primary time, and alternatives`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"data":{"id":2,"request_number":"APR-2026-000002","status":"pending","scheduled_at":"2026-08-10T09:00:00+08:00","reason_for_visit":"Blurry","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))
        val result = repository.createRequest(
            appointmentTypeId = 1,
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurry",
            alternativeScheduledTimes = listOf("2026-08-10T10:30:00+08:00"),
        )
        assertTrue(result.isSuccess)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"appointment_type_id\":1"))
        assertTrue(body.contains("alternative_scheduled_times"))
        assertTrue(body.contains("2026-08-10T10:30:00+08:00"))
    }

    @Test
    fun `createRequest sends referral source when provided`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"data":{"id":3,"request_number":"APR-2026-000003","status":"pending","scheduled_at":"2026-08-10T09:00:00+08:00","reason_for_visit":"Blurry","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))
        val result = repository.createRequest(
            appointmentTypeId = 4,
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurry",
            referringSource = "Dr. Smith",
        )
        assertTrue(result.isSuccess)
        val body = server.takeRequest().body.readUtf8()
        assertTrue(body.contains("\"referring_source\":\"Dr. Smith\""))
    }

    @Test
    fun `createRequest sends optional identity for an unlinked account`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody(
            """{"data":{"id":3,"request_number":"APR-2026-000003","status":"pending","patient_id":null,"scheduled_at":"2026-08-10T09:00:00+08:00","reason_for_visit":"Blurry","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))

        val result = repository.createRequest(
            appointmentTypeId = 1,
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
    fun `getRequest maps expanded fields and appointment reference`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"request_number":"APR-2026-000001","status":"accepted","appointment_type":{"id":1,"name":"First eye examination","duration_minutes":45},"scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","time_preferences_are_reserved":false,"created_at":"2026-08-09T10:00:00+08:00","appointment":{"id":42}}}"""
        ))
        val result = repository.getRequest(1)
        assertTrue(result.isSuccess)
        val request = result.getOrThrow()
        assertEquals(42, request.appointmentId)
        assertEquals(1, request.appointmentType?.id)
    }

    @Test
    fun `cancelRequest returns cancelled status`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"data":{"id":1,"request_number":"APR-2026-000001","status":"cancelled","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","cancelled_at":"2026-08-09T11:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00"}}"""
        ))
        val result = repository.cancelRequest(1)
        assertTrue(result.isSuccess)
        assertEquals(AppointmentRequestStatus.CANCELLED, result.getOrThrow().status)
    }
}
