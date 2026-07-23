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
import org.junit.jupiter.api.Assertions.assertNull
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
            {"data":{"id":2,"appointment_number":"APT-2026-000002","visit_reason":"follow_up",
            "status":"confirmed","scheduled_at":"2026-10-25T14:00:00Z","contact_notes":null,
            "staff_notes":"All good","last_reschedule_reason":"Doctor availability changed",
            "source":"mobile_app",
            "assigned_optometrist":{"id":7,"name":"Dr. Santos"}}}
        """.trimIndent()))

        val result = repository.getAppointment(2)
        assertTrue(result.isSuccess)
        val appt = result.getOrThrow()
        assertEquals(2, appt.id)
        assertEquals(AppointmentStatus.CONFIRMED, appt.status)
        assertEquals("All good", appt.staffNotes)
        assertEquals("Doctor availability changed", appt.lastRescheduleReason)
        assertEquals("APT-2026-000002", appt.appointmentNumber)
        assertEquals("mobile_app", appt.source)
        assertEquals("Dr. Santos", appt.assignedOptometrist?.name)
    }

    @Test
    fun `getAppointment maps arrived and no_show statuses`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":2,"visit_reason":"follow_up","status":"arrived",
            "scheduled_at":"2026-10-25T14:00:00Z","contact_notes":null,"staff_notes":null}}
        """.trimIndent()))

        val arrived = repository.getAppointment(2).getOrThrow()
        assertEquals(AppointmentStatus.ARRIVED, arrived.status)

        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":3,"visit_reason":"follow_up","status":"no_show",
            "scheduled_at":"2026-10-25T14:00:00Z","contact_notes":null,"staff_notes":null}}
        """.trimIndent()))

        val noShow = repository.getAppointment(3).getOrThrow()
        assertEquals(AppointmentStatus.NO_SHOW, noShow.status)
    }

    @Test
    fun `getAppointmentAvailability maps slot grid and booking query`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"date":"2026-07-13","timezone":"Asia/Manila","interval_minutes":15,
            "visit_reason_id":1,"visit_duration_minutes":30,"optometrist_id":null,
            "appointment_id":null,"day_status":"open","generated_at":"2026-07-13T08:12:04+08:00",
            "slots":[
              {"starts_at":"2026-07-13T09:00:00+08:00","ends_at":"2026-07-13T09:30:00+08:00",
               "available":true,"reason":null},
              {"starts_at":"2026-07-13T09:15:00+08:00","ends_at":"2026-07-13T09:45:00+08:00",
               "available":false,"reason":"capacity_reached"}
            ]}}
        """.trimIndent()))

        val availability = repository.getAppointmentAvailability("2026-07-13", 1).getOrThrow()

        val request = server.takeRequest()
        assertEquals("/appointments/availability?date=2026-07-13&visit_reason_id=1", request.path)
        assertEquals("Asia/Manila", availability.timezone)
        assertEquals(15, availability.intervalMinutes)
        assertEquals(30, availability.visitDurationMinutes)
        assertEquals(2, availability.slots.size)
        assertTrue(availability.slots.first().available)
        assertNull(availability.slots.first().reason)
        assertEquals("capacity_reached", availability.slots.last().reason)
    }

    @Test
    fun `getAppointmentAvailability maps closed day and reschedule query`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"date":"2026-07-19","timezone":"Asia/Manila","interval_minutes":15,
            "visit_reason_id":2,"visit_duration_minutes":15,"optometrist_id":null,
            "appointment_id":4,"day_status":"closed","generated_at":"2026-07-13T08:12:04+08:00",
            "slots":[]}}
        """.trimIndent()))

        val availability = repository.getAppointmentAvailability("2026-07-19", 2, 4).getOrThrow()

        val request = server.takeRequest()
        assertEquals(
            "/appointments/availability?date=2026-07-19&visit_reason_id=2&appointment_id=4",
            request.path,
        )
        assertEquals("closed", availability.dayStatus)
        assertEquals(4, availability.appointmentId)
        assertTrue(availability.slots.isEmpty())
    }

    @Test
    fun `createAppointment returns created appointment`() = runTest {
        server.enqueue(MockResponse().setResponseCode(201).setBody("""
            {"data":{"id":3,"visit_reason":"prescription_check","status":"pending",
            "scheduled_at":"2026-10-26T09:00:00Z","contact_notes":"please confirm","staff_notes":null}}
        """.trimIndent()))

        val result = repository.createAppointment(3, "2026-10-26T09:00:00Z", "please confirm")
        assertTrue(result.isSuccess)
        assertEquals(3, result.getOrThrow().id)
    }

    @Test
    fun `createAppointment 422 maps to ValidationError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""
            {"message":"Invalid date","errors":{"scheduled_at":["The scheduled at field is required."]}}
        """.trimIndent()))

        val result = repository.createAppointment(1, "", null)
        assertTrue(result.isFailure)
        assertInstanceOf(
            com.eyecare.app.domain.model.AppointmentError.ValidationError::class.java,
            result.exceptionOrNull()
        )
    }

    @Test
    fun `createAppointment preserves stale slot error code`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""
            {"message":"This time slot is not available","code":"SLOT_UNAVAILABLE",
            "errors":{"scheduled_at":["This time slot is not available."]},
            "availability":{"date":"2026-07-13","visit_reason_id":1}}
        """.trimIndent()))

        val error = repository.createAppointment(1, "2026-07-13T09:00:00+08:00", null).exceptionOrNull()

        assertInstanceOf(com.eyecare.app.domain.model.AppointmentError.ValidationError::class.java, error)
        assertEquals(
            "SLOT_UNAVAILABLE",
            (error as com.eyecare.app.domain.model.AppointmentError.ValidationError).code,
        )
    }

    @Test
    fun `rescheduleAppointment returns updated appointment pending for staff confirmation`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":4,"visit_reason":"eye_exam","status":"pending",
            "scheduled_at":"2026-11-01T13:00:00Z","contact_notes":null,"staff_notes":null}}
        """.trimIndent()))

        val result = repository.rescheduleAppointment(4, "2026-11-01T13:00:00Z")
        assertTrue(result.isSuccess)
        val appt = result.getOrThrow()
        assertEquals(4, appt.id)
        assertEquals(AppointmentStatus.PENDING, appt.status)
        assertEquals("2026-11-01T13:00:00Z", appt.scheduledAt)
    }

    @Test
    fun `rescheduleAppointment 422 maps to ValidationError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(422).setBody("""
            {"message":"This time slot is not available","errors":{"scheduled_at":["This time slot is not available."]}}
        """.trimIndent()))

        val result = repository.rescheduleAppointment(4, "2026-11-01T13:00:00Z")
        assertTrue(result.isFailure)
        assertInstanceOf(
            com.eyecare.app.domain.model.AppointmentError.ValidationError::class.java,
            result.exceptionOrNull()
        )
    }

    @Test
    fun `updateAppointmentContactNote sends dedicated patch and maps appointment`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":4,"visit_reason":"Follow-up","status":"confirmed",
            "scheduled_at":"2026-11-01T13:00:00+08:00","contact_notes":"Call before arrival",
            "staff_notes":null}}
        """.trimIndent()))

        val result = repository.updateAppointmentContactNote(4, "Call before arrival")

        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/appointments/4/contact-note", request.path)
        assertEquals("{\"contact_notes\":\"Call before arrival\"}", request.body.readUtf8())
        assertEquals("Call before arrival", result.getOrThrow().contactNotes)
        assertEquals(AppointmentStatus.CONFIRMED, result.getOrThrow().status)
    }

    @Test
    fun `updateAppointmentContactNote sends null to clear note`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""
            {"data":{"id":4,"visit_reason":"Follow-up","status":"pending",
            "scheduled_at":"2026-11-01T13:00:00+08:00","contact_notes":null,
            "staff_notes":null}}
        """.trimIndent()))

        val result = repository.updateAppointmentContactNote(4, null)

        assertEquals("{\"contact_notes\":null}", server.takeRequest().body.readUtf8())
        assertNull(result.getOrThrow().contactNotes)
    }
}
