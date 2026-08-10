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
    fun `decodes appointment types with nullable description and referral flag`() {
        val body = """{"data":[{"id":1,"name":"First eye examination","description":"For your first examination at the clinic.","duration_minutes":45,"requires_referral":false},{"id":4,"name":"Referral","description":null,"duration_minutes":45,"requires_referral":true}]}"""
        val response = json.decodeFromString<AppointmentTypeListResponse>(body)
        assertEquals(2, response.data.size)
        assertEquals(1, response.data[0].id)
        assertEquals("First eye examination", response.data[0].name)
        assertEquals("For your first examination at the clinic.", response.data[0].description)
        assertEquals(45, response.data[0].durationMinutes)
        assertFalse(response.data[0].requiresReferral)
        assertEquals(4, response.data[1].id)
        assertNull(response.data[1].description)
        assertTrue(response.data[1].requiresReferral)
    }

    @Test
    fun `decodes type-specific availability with visit_duration and type_id`() {
        val body = """{"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":15,"slot_duration_minutes":45,"visit_duration_minutes":45,"appointment_type_id":1,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[{"starts_at":"2026-08-10T09:00:00+08:00","ends_at":"2026-08-10T09:45:00+08:00","available":true}]}}"""
        val response = json.decodeFromString<AppointmentRequestAvailabilityResponse>(body)
        assertEquals("2026-08-10", response.data.date)
        assertEquals(15, response.data.intervalMinutes)
        assertEquals(45, response.data.visitDurationMinutes)
        assertEquals(1, response.data.appointmentTypeId)
        assertEquals(1, response.data.slots.size)
        assertTrue(response.data.slots[0].available)
    }

    @Test
    fun `decodes availability with legacy nullable fields`() {
        val body = """{"data":{"date":"2026-08-10","timezone":"Asia/Manila","interval_minutes":30,"slot_duration_minutes":30,"day_status":"open","generated_at":"2026-08-09T10:00:00+08:00","slots":[]}}"""
        val response = json.decodeFromString<AppointmentRequestAvailabilityResponse>(body)
        assertNull(response.data.visitDurationMinutes)
        assertNull(response.data.appointmentTypeId)
    }

    @Test
    fun `decodes request list with expanded fields`() {
        val body = """{"data":[{"id":1,"request_number":"APR-2026-000001","status":"pending","patient_id":null,"appointment_type":{"id":1,"name":"First eye examination","duration_minutes":45},"scheduled_at":"2026-08-10T10:00:00+08:00","alternative_scheduled_times":["2026-08-10T14:00:00+08:00"],"provisional_duration_minutes":45,"reason_for_visit":"Blurred vision","referring_source":null,"time_preferences_are_reserved":false,"expires_at":"2026-08-11T10:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00","appointment":null}],"meta":{"current_page":1,"last_page":1,"per_page":15,"total":1}}"""
        val response = json.decodeFromString<AppointmentRequestListResponse>(body)
        val dto = response.data[0]
        assertEquals(1, dto.appointmentType?.id)
        assertEquals("First eye examination", dto.appointmentType?.name)
        assertEquals(45, dto.appointmentType?.durationMinutes)
        assertEquals(listOf("2026-08-10T14:00:00+08:00"), dto.alternativeScheduledTimes)
        assertEquals(45, dto.provisionalDurationMinutes)
        assertNull(dto.referringSource)
        assertFalse(dto.timePreferencesAreReserved)
    }

    @Test
    fun `decodes legacy request without expanded fields`() {
        val body = """{"data":{"id":1,"request_number":"APR-2026-000001","status":"pending","patient_id":null,"scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Blurred vision","expires_at":"2026-08-11T10:00:00+08:00","created_at":"2026-08-09T10:00:00+08:00","appointment":null}}"""
        val response = json.decodeFromString<AppointmentRequestResponse>(body)
        assertNull(response.data.appointmentType)
        assertNull(response.data.alternativeScheduledTimes)
        assertNull(response.data.provisionalDurationMinutes)
        assertNull(response.data.referringSource)
        assertFalse(response.data.timePreferencesAreReserved)
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
    fun `create request encodes required type ID and primary time`() {
        val request = CreateAppointmentRequest(
            appointmentTypeId = 1,
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurred vision in left eye",
        )
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"appointment_type_id\":1"))
        assertTrue(encoded.contains("scheduled_at"))
        assertTrue(encoded.contains("reason_for_visit"))
        assertFalse(encoded.contains("\"identity\""))
        assertFalse(encoded.contains("alternative"))
        assertFalse(encoded.contains("referring"))
    }

    @Test
    fun `create request encodes alternatives and referral source`() {
        val request = CreateAppointmentRequest(
            appointmentTypeId = 4,
            scheduledAt = "2026-08-10T09:00:00+08:00",
            alternativeScheduledTimes = listOf("2026-08-10T10:30:00+08:00", "2026-08-11T09:00:00+08:00"),
            reasonForVisit = "Blurred vision in left eye",
            referringSource = "Dr. Smith",
        )
        val encoded = json.encodeToString(request)
        assertTrue(encoded.contains("\"appointment_type_id\":4"))
        assertTrue(encoded.contains("alternative_scheduled_times"))
        assertTrue(encoded.contains("2026-08-10T10:30:00+08:00"))
        assertTrue(encoded.contains("2026-08-11T09:00:00+08:00"))
        assertTrue(encoded.contains("\"referring_source\":\"Dr. Smith\""))
    }

    @Test
    fun `create request encodes the expanded identity object`() {
        val request = CreateAppointmentRequest(
            appointmentTypeId = 1,
            scheduledAt = "2026-08-10T09:00:00+08:00",
            reasonForVisit = "Blurred vision in left eye",
            identity = AppointmentRequestIdentityDto(
                phone = "+639171234567",
                email = "alex@example.com",
                firstName = "Alex",
                middleName = "M",
                lastName = "Rivera",
                dateOfBirth = "1990-05-15",
                gender = "female",
                occupation = "Teacher",
                address = "123 Main St, Manila",
            ),
        )

        val encoded = json.encodeToString(request)

        assertTrue(encoded.contains("\"identity\""))
        assertTrue(encoded.contains("\"first_name\":\"Alex\""))
        assertTrue(encoded.contains("\"middle_name\":\"M\""))
        assertTrue(encoded.contains("\"last_name\":\"Rivera\""))
        assertTrue(encoded.contains("\"date_of_birth\":\"1990-05-15\""))
        assertTrue(encoded.contains("\"phone\":\"+639171234567\""))
        assertTrue(encoded.contains("\"email\":\"alex@example.com\""))
        assertTrue(encoded.contains("\"gender\":\"female\""))
        assertTrue(encoded.contains("\"occupation\":\"Teacher\""))
        assertTrue(encoded.contains("\"address\":\"123 Main St, Manila\""))
    }

    @Test
    fun `decodes unknown status without breaking`() {
        val body = """{"data":{"id":1,"request_number":"APR-2026-000001","status":"something_new","scheduled_at":"2026-08-10T10:00:00+08:00","reason_for_visit":"Test","created_at":"2026-08-09T10:00:00+08:00"}}"""
        val response = json.decodeFromString<AppointmentRequestResponse>(body)
        assertEquals("something_new", response.data.status)
    }
}
