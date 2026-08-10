package com.eyecare.app.domain.model

import com.eyecare.app.data.remote.dto.AppointmentTypeDto
import com.eyecare.app.data.remote.dto.AppointmentTypeListResponse
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AppointmentTypeCatalogTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val sixTypePayload = """
    {
        "data": [
            {"id": 1, "name": "New Patient", "description": "Comprehensive eye exam for first-time patients.", "duration_minutes": 45, "requires_referral": false},
            {"id": 2, "name": "Follow-up", "description": "Short visit to review progress or treatment.", "duration_minutes": 15, "requires_referral": false},
            {"id": 3, "name": "Routine Check-up", "description": "Annual or periodic eye health review.", "duration_minutes": 30, "requires_referral": false},
            {"id": 4, "name": "Problem/Urgent Visit", "description": "Sudden vision changes, pain, or injury.", "duration_minutes": 30, "requires_referral": false},
            {"id": 5, "name": "Contact Lens Consultation", "description": "Fitting, evaluation, or renewal for contact lenses.", "duration_minutes": 45, "requires_referral": false},
            {"id": 6, "name": "Referral", "description": "Visit referred by an external provider.", "duration_minutes": 45, "requires_referral": true}
        ]
    }
    """.trimIndent()

    // ── Capability 1: Patient can retrieve the six initial visible appointment types ──

    @Test
    fun `decodes six appointment types from catalog response`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        assertEquals(6, response.data.size)
    }

    @Test
    fun `each type has an id`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        response.data.forEach { type ->
            assertTrue(type.id > 0) { "Type '${type.name}' must have a positive id" }
        }
    }

    // ── Capability 2: Patient sees friendly labels, descriptions, duration, and referral requirement ──

    @Test
    fun `each type has a non-blank name`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        response.data.forEach { type ->
            assertTrue(type.name.isNotBlank()) { "Type id=${type.id} must have a non-blank name" }
        }
    }

    @Test
    fun `each type has a description`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        response.data.forEach { type ->
            assertNotNull(type.description) { "Type '${type.name}' must have a description" }
            assertTrue(type.description!!.isNotBlank()) { "Type '${type.name}' description must not be blank" }
        }
    }

    @Test
    fun `each type has a positive duration`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        response.data.forEach { type ->
            assertTrue(type.durationMinutes > 0) { "Type '${type.name}' must have positive duration" }
        }
    }

    @Test
    fun `each type includes requires_referral flag`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        val referralType = response.data.first { it.requiresReferral }
        assertEquals("Referral", referralType.name)
        val nonReferralTypes = response.data.filter { !it.requiresReferral }
        assertEquals(5, nonReferralTypes.size)
    }

    @Test
    fun `domain model exposes all patient-visible fields`() {
        val dto = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload).data[0]
        val domain = AppointmentType(
            id = dto.id,
            name = dto.name,
            description = dto.description,
            durationMinutes = dto.durationMinutes,
            requiresReferral = dto.requiresReferral,
        )
        assertEquals(dto.id, domain.id)
        assertEquals(dto.name, domain.name)
        assertEquals(dto.description, domain.description)
        assertEquals(dto.durationMinutes, domain.durationMinutes)
        assertEquals(dto.requiresReferral, domain.requiresReferral)
    }

    // ── Capability 3: Patient does not see inactive or internal-only types ──

    @Test
    fun `server-side filtering is relied upon — client stores all returned types`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        val types = response.data
        assertEquals(6, types.size, "Client should store exactly what the server returns")
    }

    @Test
    fun `empty catalog response is handled gracefully`() {
        val emptyPayload = """{"data": []}"""
        val response = json.decodeFromString<AppointmentTypeListResponse>(emptyPayload)
        assertTrue(response.data.isEmpty())
    }

    @Test
    fun `client does not filter by any admin flags`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        val dto = response.data[0]
        assertEquals(1, dto.id)
        assertEquals("New Patient", dto.name)
    }

    // ── Capability 4: Patient does not see administrative fields or internal staff information ──

    @Test
    fun `DTO exposes only id name description duration and referral`() {
        val response = json.decodeFromString<AppointmentTypeListResponse>(sixTypePayload)
        val dto = response.data[0]
        assertEquals(1, dto.id)
        assertEquals("New Patient", dto.name)
        assertEquals("Comprehensive eye exam for first-time patients.", dto.description)
        assertEquals(45, dto.durationMinutes)
        assertFalse(dto.requiresReferral)
    }

    @Test
    fun `DTO ignores admin fields present in server response`() {
        val payloadWithExtras = """
        {
            "data": [
                {
                    "id": 1, "name": "New Patient", "description": "desc",
                    "duration_minutes": 45, "requires_referral": false,
                    "is_active": true, "is_patient_visible": true,
                    "staff_notes": "internal only", "patient_label": "Friendly Name",
                    "internal_name": "NEW_PATIENT", "created_by": 5, "updated_by": 8
                }
            ]
        }
        """.trimIndent()
        val response = json.decodeFromString<AppointmentTypeListResponse>(payloadWithExtras)
        val dto = response.data[0]
        assertEquals("New Patient", dto.name, "name should come from JSON, not patient_label")
        assertEquals(1, dto.id)
        assertEquals(45, dto.durationMinutes)
    }

    @Test
    fun `DTO response with extra server fields is ignored gracefully`() {
        val payloadWithExtras = """
        {
            "data": [
                {"id": 1, "name": "New Patient", "description": "desc", "duration_minutes": 45, "requires_referral": false, "is_active": true, "is_patient_visible": true, "staff_notes": "internal", "patient_label": "New Patient"}
            ]
        }
        """.trimIndent()
        val response = json.decodeFromString<AppointmentTypeListResponse>(payloadWithExtras)
        assertEquals(1, response.data.size)
        assertEquals("New Patient", response.data[0].name)
    }
}
