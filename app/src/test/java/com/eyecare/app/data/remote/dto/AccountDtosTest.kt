package com.eyecare.app.data.remote.dto

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AccountDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `ContactsResponse decodes masked contacts`() {
        val body = """{"data":[{"id":1,"type":"email","masked_value":"a***@example.com","is_primary":true,"verified_at":"2026-08-01T10:00:00+08:00"},{"id":2,"type":"phone","masked_value":"0917***4567","is_primary":false,"verified_at":null}]}"""
        val response = json.decodeFromString<ContactsResponse>(body)
        assertEquals(2, response.data.size)
        assertEquals("a***@example.com", response.data[0].maskedValue)
        assertTrue(response.data[0].isPrimary)
        assertFalse(response.data[1].isPrimary)
        assertNull(response.data[1].verifiedAt)
    }

    @Test
    fun `StepUpOtpResponse decodes challenge`() {
        val body = """{"data":{"challenge_id":"uuid-1","expires_at":"2026-08-01T10:15:00+08:00","contact_type":"email","masked_contact":"a***@example.com"}}"""
        val response = json.decodeFromString<StepUpOtpResponse>(body)
        assertEquals("uuid-1", response.data.challengeId)
        assertEquals("email", response.data.contactType)
        assertEquals("a***@example.com", response.data.maskedContact)
    }

    @Test
    fun `StepUpOtpRequest includes required purpose`() {
        val encoded = json.encodeToString(StepUpOtpRequest(purpose = "sensitive_change"))

        assertEquals("""{"purpose":"sensitive_change"}""", encoded)
    }

    @Test
    fun `StepUpVerifyResponse decodes token`() {
        val body = """{"data":{"step_up_token":"hex-abc","expires_in":900}}"""
        val response = json.decodeFromString<StepUpVerifyResponse>(body)
        assertEquals("hex-abc", response.data.stepUpToken)
        assertEquals(900, response.data.expiresIn)
    }

    @Test
    fun `LinkStateResponse decodes linked status`() {
        val body = """{"data":{"status":"linked","linked_at":"2026-08-01T10:00:00+08:00"}}"""
        val response = json.decodeFromString<LinkStateResponse>(body)
        assertEquals("linked", response.data.status)
        assertEquals("2026-08-01T10:00:00+08:00", response.data.linkedAt)
    }

    @Test
    fun `LinkStateResponse decodes unlinked status`() {
        val body = """{"data":{"status":"unlinked"}}"""
        val response = json.decodeFromString<LinkStateResponse>(body)
        assertEquals("unlinked", response.data.status)
        assertNull(response.data.linkedAt)
    }

    @Test
    fun `InvitationAcceptResponse decodes linked result`() {
        val body = """{"data":{"status":"linked","linked_at":"2026-08-01T10:00:00+08:00"}}"""
        val response = json.decodeFromString<InvitationAcceptResponse>(body)
        assertEquals("linked", response.data.status)
    }

    @Test
    fun `PasswordChangeResponse decodes message`() {
        val body = """{"data":{"message":"Password changed successfully."}}"""
        val response = json.decodeFromString<PasswordChangeResponse>(body)
        assertEquals("Password changed successfully.", response.data.message)
    }
}
