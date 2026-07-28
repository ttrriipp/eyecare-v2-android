package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentV1DtosTest {

    private val json = ApiContractFixtures.json

    @Test
    fun `appointment type decodes requires_referral`() {
        val dto = json.decodeFromString<AppointmentV1Dtos.AppointmentTypeDto>(
            """
            {
              "id": 4,
              "name": "Referral",
              "duration_minutes": 30,
              "requires_referral": true
            }
            """.trimIndent(),
        )

        assertEquals(4, dto.id)
        assertEquals("Referral", dto.name)
        assertEquals(30, dto.durationMinutes)
        assertTrue(dto.requiresReferral)
    }

    @Test
    fun `appointment type with requires_referral false`() {
        val dto = json.decodeFromString<AppointmentV1Dtos.AppointmentTypeDto>(
            """
            {
              "id": 1,
              "name": "New Patient",
              "duration_minutes": 30,
              "requires_referral": false
            }
            """.trimIndent(),
        )

        assertEquals(1, dto.id)
        assertEquals("New Patient", dto.name)
        assertFalse(dto.requiresReferral)
    }

    @Test
    fun `appointment list decodes pagination and name-only optometrist`() {
        val response = json.decodeFromString<AppointmentV1Dtos.AppointmentListResponse>(
            """
            {
              "data": [
                {
                  "id": 1,
                  "appointment_number": "APT-2026-000001",
                  "appointment_type": "New Patient",
                  "duration_minutes": 30,
                  "referring_source": null,
                  "status": "confirmed",
                  "scheduled_at": "2026-07-28T10:00:00+08:00",
                  "contact_notes": "Please call before arrival",
                  "last_reschedule_reason": null,
                  "source": "mobile",
                  "assigned_optometrist": { "name": "Dr. Maria Santos" }
                }
              ],
              "links": {
                "first": "https://example.test/api/v1/appointments?page=1",
                "last": "https://example.test/api/v1/appointments?page=1",
                "prev": null,
                "next": null
              },
              "meta": {
                "current_page": 1,
                "last_page": 1,
                "per_page": 15,
                "total": 1
              }
            }
            """.trimIndent(),
        )

        val appointment = response.data.single()
        assertEquals(1, appointment.id)
        assertEquals("APT-2026-000001", appointment.appointmentNumber)
        assertEquals("New Patient", appointment.appointmentType)
        assertEquals(30, appointment.durationMinutes)
        assertNull(appointment.referringSource)
        assertEquals("confirmed", appointment.status)
        assertEquals("mobile", appointment.source)
        assertEquals("Dr. Maria Santos", appointment.assignedOptometrist?.name)
        assertEquals("Please call before arrival", appointment.contactNotes)

        assertEquals(1, response.meta?.currentPage)
        assertEquals(15, response.meta?.perPage)
        assertEquals(1, response.meta?.total)
        assertNotNull(response.links)
    }

    @Test
    fun `availability uses appointment_type_id`() {
        val dto = json.decodeFromString<AppointmentV1Dtos.AppointmentAvailabilityDto>(
            """
            {
              "date": "2026-07-28",
              "timezone": "Asia/Manila",
              "interval_minutes": 30,
              "appointment_type_id": 1,
              "visit_duration_minutes": 30,
              "optometrist_id": null,
              "appointment_id": null,
              "day_status": "open",
              "generated_at": "2026-07-27T10:00:00+08:00",
              "slots": [
                {
                  "starts_at": "2026-07-28T09:00:00+08:00",
                  "ends_at": "2026-07-28T09:30:00+08:00",
                  "available": true,
                  "reason": null
                },
                {
                  "starts_at": "2026-07-28T09:30:00+08:00",
                  "ends_at": "2026-07-28T10:00:00+08:00",
                  "available": false,
                  "reason": "capacity_reached"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals(1, dto.appointmentTypeId)
        assertEquals(30, dto.visitDurationMinutes)
        assertEquals("open", dto.dayStatus)
        assertEquals(2, dto.slots.size)
        assertTrue(dto.slots[0].available)
        assertNull(dto.slots[0].reason)
        assertFalse(dto.slots[1].available)
        assertEquals("capacity_reached", dto.slots[1].reason)
    }

    @Test
    fun `create request uses appointment_type_id and optional referring_source`() {
        val request = AppointmentV1Dtos.CreateAppointmentRequest(
            appointmentTypeId = 4,
            scheduledAt = "2026-07-28T10:00:00+08:00",
            contactNotes = null,
            referringSource = "Dr. Smith",
        )

        val encoded = json.encodeToString(AppointmentV1Dtos.CreateAppointmentRequest.serializer(), request)

        assertTrue(encoded.contains("\"appointment_type_id\":4"))
        assertTrue(encoded.contains("\"referring_source\":\"Dr. Smith\""))
    }

    @Test
    fun `create request omits null referring_source`() {
        val request = AppointmentV1Dtos.CreateAppointmentRequest(
            appointmentTypeId = 1,
            scheduledAt = "2026-07-28T10:00:00+08:00",
        )

        val encoded = json.encodeToString(AppointmentV1Dtos.CreateAppointmentRequest.serializer(), request)

        assertTrue(encoded.contains("\"appointment_type_id\":1"))
        assertFalse(encoded.contains("referring_source"))
    }
}
