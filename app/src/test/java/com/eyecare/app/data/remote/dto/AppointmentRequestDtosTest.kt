package com.eyecare.app.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentRequestDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes request availability with slots`() {
        val body = """{"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":30,"slot_duration_minutes":30,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[{"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:30:00+08:00","available":true},{"starts_at":"2026-08-10T09:30:00+08:00","ends_at":"2026-08-10T10:00:00+08:00","available":false,"reason":"capacity_reached"}]}}"""
        val response = json.decodeFromString<AppointmentRequestAvailabilityResponse>(body)
        assertEquals("2026-08-10", response.data.date)
        assertEquals(2, response.data.slots.size)
        assertTrue(response.data.slots[0].available)
        assertFalse(response.data.slots[1].available)
        assertEquals("capacity_reached", response.data.slots[1].reason)
    }

    @Test
    fun `decodes request list with pagination`() {
        val body = """{"data":[{"id":1,"request_number":"APR-2026-000001","status":"pending","patient_id":null,"scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Blurred vision","expires_at":"2026-08-11T10:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00","appointment":null}],"links":{"first":"...","last":"...","prev":null,"next":null},"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}"""
        val response = json.decodeFromString<AppointmentRequestListResponse>(body)
        assertEquals(1, response.data.size)
        assertEquals("APR-2026-000001", response.data[0].requestNumber)
        assertEquals("pending", response.data[0].status)
        assertNull(response.data[0].patientId)
        assertNull(response.data[0].appointment)
        assertEquals(1, response.meta?.total)
    }

    @Test
    fun `decodes accepted request with appointment reference`() {
        val body = """{"data":{"id":1,"request_number":"APR-2026-000001","status":"accepted","patient_id":1,"scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Blurred vision","created_at":"2026-08-09T10:00:00+08:00","appointment":{"id":42}}}"""
        val response = json.decodeFromString<AppointmentRequestResponse>(body)
        assertEquals("accepted", response.data.status)
        assertEquals(42, response.data.appointment?.id)
    }

    @Test
    fun `decodes cancelled request with cancelled_at`() {
        val body = """{"data":{"id":1,"request_number":"APR-2026-000001","status":"cancelled","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Blurred vision","cancelled_at":"2026-08-09T11:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00","appointment":null}}"""
        val response = json.decodeFromString<AppointmentRequestResponse>(body)
        assertEquals("cancelled", response.data.status)
        assertEquals("2026-08-09T11:00:00+08:00", response.data.cancelledAt)
    }

    @Test
    fun `create request encodes only scheduled_at and reason_for_visit`() {
        val request = CreateAppointmentRequest(
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurred vision in left eye",
        )
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("scheduled_at"))
        assertTrue(encoded.contains("reason_for_visit"))
        assertFalse(encoded.contains("appointment_type"))
        assertFalse(encoded.contains("contact_notes"))
        assertFalse(encoded.contains("patient_id"))
        assertFalse(encoded.contains("optometrist"))
    }

    @Test
    fun `decodes unknown status without breaking`() {
        val body = """{"data":{"id":1,"request_number":"APR-2026-000001","status":"something_new","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","created_at":"2026-08-09T10:00:00+08:00"}}"""
        val response = json.decodeFromString<AppointmentRequestResponse>(body)
        assertEquals("something_new", response.data.status)
    }
}
