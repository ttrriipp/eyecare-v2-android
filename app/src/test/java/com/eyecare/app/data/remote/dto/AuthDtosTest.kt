package com.eyecare.app.data.remote.dto

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `LoginResponse decodes OTP-required variant`() {
        val body = """{"data":{"step_up_required":true,"challenge_id":"abc","expires_at":"2026-08-01T10:00:00+08:00"}}"""
        val response = json.decodeFromString<LoginResponse>(body)
        val data = response.data
        assertEquals(true, data.stepUpRequired)
        assertEquals("abc", data.challengeId)
        assertNull(data.token)
        assertNull(data.user)
    }

    @Test
    fun `LoginResponse decodes trusted-device variant`() {
        val body = """{"data":{"step_up_required":false,"token":"1|abc","user":{"id":1,"name":"Ana","link_status":"linked"}}}"""
        val response = json.decodeFromString<LoginResponse>(body)
        val data = response.data
        assertEquals(false, data.stepUpRequired)
        assertEquals("1|abc", data.token)
        assertEquals(1, data.user?.id)
        assertEquals("linked", data.user?.linkStatus)
    }

    @Test
    fun `PatientAccountDto decodes linked account with linked_patient`() {
        val body = """{"id":1,"name":"Ana Reyes","first_name":"Ana","last_name":"Reyes","email":"ana@example.com","phone":"09171234567","role":"patient","date_of_birth":"1990-05-15","link_status":"linked","linked_patient":{"patient_number":"PAT-2026-000001","full_name":"Ana Reyes","date_of_birth":"1990-05-15","gender":"female","occupation":"Teacher","address":"123 Main St","phone":"09171234567","contact_email":"ana@example.com"}}"""
        val dto = json.decodeFromString<PatientAccountDto>(body)
        assertEquals(1, dto.id)
        assertEquals("linked", dto.linkStatus)
        assertNotNull(dto.linkedPatient)
        assertEquals("PAT-2026-000001", dto.linkedPatient?.patientNumber)
    }

    @Test
    fun `PatientAccountDto decodes unlinked account with null linked_patient`() {
        val body = """{"id":1,"name":"Ana","link_status":"unlinked","linked_patient":null}"""
        val dto = json.decodeFromString<PatientAccountDto>(body)
        assertEquals("unlinked", dto.linkStatus)
        assertNull(dto.linkedPatient)
    }

    @Test
    fun `PatientAccountDto maps unknown link_status`() {
        val body = """{"id":1,"name":"Ana","link_status":"something_new"}"""
        val dto = json.decodeFromString<PatientAccountDto>(body)
        assertEquals("something_new", dto.linkStatus)
    }

    @Test
    fun `PoliciesResponse decodes both entries`() {
        val body = """{"data":{"privacy_policy":{"version":"2026-08","url":"https://example.com/privacy","effective_date":"2026-08-01"},"terms_of_service":{"version":"2026-08","url":"https://example.com/terms","effective_date":"2026-08-01"}}}"""
        val response = json.decodeFromString<PoliciesResponse>(body)
        assertEquals("2026-08", response.data.privacyPolicy.version)
        assertEquals("https://example.com/privacy", response.data.privacyPolicy.url)
        assertEquals("2026-08", response.data.termsOfService.version)
    }

    @Test
    fun `AuthSessionResponse decodes token and user`() {
        val body = """{"data":{"token":"1|abc","user":{"id":1,"name":"Ana","link_status":"linked"}}}"""
        val response = json.decodeFromString<AuthSessionResponse>(body)
        assertEquals("1|abc", response.data.token)
        assertEquals(1, response.data.user.id)
    }

    @Test
    fun `RegistrationOtpResponse decodes challenge`() {
        val body = """{"data":{"challenge_id":"uuid-123","expires_at":"2026-08-01T10:10:00+08:00"}}"""
        val response = json.decodeFromString<RegistrationOtpResponse>(body)
        assertEquals("uuid-123", response.data.challengeId)
    }

    @Test
    fun `RegistrationVerifyResponse decodes proof token`() {
        val body = """{"data":{"registration_token":"hex-abc","expires_at":"2026-08-01T10:30:00+08:00","contact_type":"email"}}"""
        val response = json.decodeFromString<RegistrationVerifyResponse>(body)
        assertEquals("hex-abc", response.data.registrationToken)
        assertEquals("email", response.data.contactType)
    }
}
