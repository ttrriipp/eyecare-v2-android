package com.eyecare.app.data.repository

import com.eyecare.app.data.remote.api.AppointmentV1ApiService
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.Retrofit

class AppointmentV1RepositoryImplTest {

    private lateinit var server: MockWebServer
    private lateinit var repository: AppointmentV1RepositoryImpl
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
        repository = AppointmentV1RepositoryImpl(retrofit.create(AppointmentV1ApiService::class.java), json)
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    @Test
    fun `getAppointmentTypes maps requires_referral`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"data":[
                  {"id":1,"name":"New Patient","duration_minutes":30,"requires_referral":false},
                  {"id":4,"name":"Referral","duration_minutes":30,"requires_referral":true}
                ]}
                """.trimIndent(),
            ),
        )

        val types = repository.getAppointmentTypes().getOrThrow()
        assertEquals(2, types.size)
        assertEquals("New Patient", types[0].name)
        assertFalse(types[0].requiresReferral)
        assertEquals("Referral", types[1].name)
        assertTrue(types[1].requiresReferral)
    }

    @Test
    fun `getAppointments returns paginated result`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "data":[
                    {"id":1,"appointment_number":"APT-001","appointment_type":"New Patient",
                     "duration_minutes":30,"referring_source":null,"status":"confirmed",
                     "scheduled_at":"2026-07-28T10:00:00+08:00","contact_notes":null,
                     "last_reschedule_reason":null,"source":"mobile",
                     "assigned_optometrist":{"name":"Dr. Santos"}}
                  ],
                  "links":{"first":"?page=1","last":"?page=2","prev":null,"next":"?page=2"},
                  "meta":{"current_page":1,"last_page":2,"per_page":15,"total":20}
                }
                """.trimIndent(),
            ),
        )

        val result = repository.getAppointments().getOrThrow()
        assertEquals(1, result.data.size)
        assertEquals(1, result.currentPage)
        assertEquals(2, result.lastPage)
        assertEquals(20, result.total)
        assertTrue(result.hasMorePages)

        val appt = result.data.single()
        assertEquals("APT-001", appt.appointmentNumber)
        assertEquals("New Patient", appt.appointmentType)
        assertEquals(30, appt.durationMinutes)
        assertEquals(AppointmentStatus.CONFIRMED, appt.status)
        assertEquals("mobile", appt.source)
        assertEquals("Dr. Santos", appt.assignedOptometrist?.name)
    }

    @Test
    fun `getAppointments single page has no more pages`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"data":[],"links":{"first":"?page=1","last":"?page=1","prev":null,"next":null},
                 "meta":{"current_page":1,"last_page":1,"per_page":15,"total":0}}
                """.trimIndent(),
            ),
        )

        val result = repository.getAppointments().getOrThrow()
        assertFalse(result.hasMorePages)
        assertEquals(0, result.total)
    }

    @Test
    fun `getAppointmentAvailability maps appointment_type_id`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"data":{"date":"2026-07-28","timezone":"Asia/Manila","interval_minutes":30,
                 "appointment_type_id":1,"visit_duration_minutes":30,"optometrist_id":null,
                 "appointment_id":null,"day_status":"open","generated_at":"2026-07-27T10:00:00+08:00",
                 "slots":[
                   {"starts_at":"2026-07-28T09:00:00+08:00","ends_at":"2026-07-28T09:30:00+08:00",
                    "available":true,"reason":null}
                 ]}}
                """.trimIndent(),
            ),
        )

        val availability = repository.getAppointmentAvailability("2026-07-28", 1).getOrThrow()
        assertEquals(1, availability.visitReasonId)
        assertEquals("open", availability.dayStatus)
        assertEquals(1, availability.slots.size)
    }

    @Test
    fun `createAppointment sends appointment_type_id and referring_source`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """
                {"data":{"id":3,"appointment_number":"APT-003","appointment_type":"Referral",
                 "duration_minutes":30,"referring_source":"Dr. Smith","status":"pending",
                 "scheduled_at":"2026-07-28T10:00:00+08:00","contact_notes":null,
                 "last_reschedule_reason":null,"source":"mobile","assigned_optometrist":null}}
                """.trimIndent(),
            ),
        )

        val result = repository.createAppointment(4, "2026-07-28T10:00:00+08:00", null, "Dr. Smith")
        assertTrue(result.isSuccess)
        val appt = result.getOrThrow()
        assertEquals(3, appt.id)
        assertEquals("Referral", appt.appointmentType)
        assertEquals("Dr. Smith", appt.referringSource)
    }

    @Test
    fun `rescheduleAppointment returns updated appointment`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {"data":{"id":4,"appointment_number":"APT-004","appointment_type":"New Patient",
                 "duration_minutes":30,"referring_source":null,"status":"pending",
                 "scheduled_at":"2026-08-01T13:00:00+08:00","contact_notes":null,
                 "last_reschedule_reason":"Patient request","source":"mobile","assigned_optometrist":null}}
                """.trimIndent(),
            ),
        )

        val result = repository.rescheduleAppointment(4, "2026-08-01T13:00:00+08:00")
        assertTrue(result.isSuccess)
        assertEquals("2026-08-01T13:00:00+08:00", result.getOrThrow().scheduledAt)
    }
}
