package com.eyecare.app.data.remote.dto

import com.eyecare.app.data.remote.ApiContractFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class PatientIntakeDtosTest {

    private val json = ApiContractFixtures.json

    @Test
    fun `intake response decodes complete draft intake`() {
        val dto = json.decodeFromString<PatientIntakeDtos.PatientIntakeResponse>(
            """
            {
              "data": {
                "id": 1,
                "patient_id": 1,
                "appointment_id": 1,
                "status": "draft",
                "appointment_type": "New Patient",
                "full_name": "Ana Reyes",
                "date_of_birth": "1990-05-15",
                "gender": "female",
                "occupation": "Teacher",
                "address": "123 Main St",
                "phone": "09171234567",
                "email": "ana@example.com",
                "chief_complaint": "Blurred vision",
                "past_ocular_history": null,
                "past_surgical_history": null,
                "past_medical_history": null,
                "allergies": null,
                "medications": null,
                "submitted_at": null,
                "verified_at": null,
                "created_at": "2026-07-27T10:00:00+08:00",
                "updated_at": "2026-07-27T10:00:00+08:00"
              }
            }
            """.trimIndent(),
        )

        val intake = dto.data!!
        assertEquals(1, intake.id)
        assertEquals(1, intake.patientId)
        assertEquals(1, intake.appointmentId)
        assertEquals("draft", intake.status)
        assertEquals("New Patient", intake.appointmentType)
        assertEquals("Ana Reyes", intake.fullName)
        assertEquals("1990-05-15", intake.dateOfBirth)
        assertEquals("female", intake.gender)
        assertEquals("Teacher", intake.occupation)
        assertEquals("123 Main St", intake.address)
        assertEquals("09171234567", intake.phone)
        assertEquals("ana@example.com", intake.email)
        assertEquals("Blurred vision", intake.chiefComplaint)
        assertNull(intake.pastOcularHistory)
        assertNull(intake.submittedAt)
        assertNull(intake.verifiedAt)
    }

    @Test
    fun `intake response decodes null data for no intake`() {
        val dto = json.decodeFromString<PatientIntakeDtos.PatientIntakeResponse>(
            """{"data": null}""",
        )

        assertNull(dto.data)
    }

    @Test
    fun `intake response decodes submitted status`() {
        val dto = json.decodeFromString<PatientIntakeDtos.PatientIntakeResponse>(
            """
            {
              "data": {
                "id": 1,
                "patient_id": 1,
                "appointment_id": 1,
                "status": "submitted",
                "full_name": "Ana Reyes",
                "submitted_at": "2026-07-27T10:00:00+08:00",
                "verified_at": null
              }
            }
            """.trimIndent(),
        )

        assertEquals("submitted", dto.data!!.status)
        assertEquals("2026-07-27T10:00:00+08:00", dto.data!!.submittedAt)
    }

    @Test
    fun `intake response decodes verified status`() {
        val dto = json.decodeFromString<PatientIntakeDtos.PatientIntakeResponse>(
            """
            {
              "data": {
                "id": 1,
                "patient_id": 1,
                "appointment_id": 1,
                "status": "verified",
                "full_name": "Ana Reyes",
                "submitted_at": "2026-07-27T10:00:00+08:00",
                "verified_at": "2026-07-27T11:00:00+08:00"
              }
            }
            """.trimIndent(),
        )

        assertEquals("verified", dto.data!!.status)
        assertEquals("2026-07-27T11:00:00+08:00", dto.data!!.verifiedAt)
    }

    @Test
    fun `save request serializes only documented fields`() {
        val request = PatientIntakeDtos.SaveIntakeRequest(
            fullName = "Ana Reyes",
            dateOfBirth = "1990-05-15",
            gender = "female",
            occupation = "Teacher",
            address = "123 Main St",
            phone = "09171234567",
            email = "ana@example.com",
            chiefComplaint = "Blurred vision",
            pastOcularHistory = null,
            pastSurgicalHistory = null,
            pastMedicalHistory = null,
            allergies = null,
            medications = null,
        )

        val encoded = json.encodeToString(PatientIntakeDtos.SaveIntakeRequest.serializer(), request)

        assert(encoded.contains("\"full_name\":\"Ana Reyes\""))
        assert(encoded.contains("\"date_of_birth\":\"1990-05-15\""))
        assert(encoded.contains("\"gender\":\"female\""))
        assert(encoded.contains("\"chief_complaint\":\"Blurred vision\""))
    }

    @Test
    fun `save request omits null fields`() {
        val request = PatientIntakeDtos.SaveIntakeRequest(
            fullName = "Ana Reyes",
        )

        val encoded = json.encodeToString(PatientIntakeDtos.SaveIntakeRequest.serializer(), request)

        assert(encoded.contains("\"full_name\":\"Ana Reyes\""))
        assert(!encoded.contains("chief_complaint"))
        assert(!encoded.contains("allergies"))
    }
}
