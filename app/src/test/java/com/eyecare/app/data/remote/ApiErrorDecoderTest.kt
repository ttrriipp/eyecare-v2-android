package com.eyecare.app.data.remote

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ApiErrorDecoderTest {

    @Test
    fun `decodes known error code and message`() {
        val body = """{"error":{"code":"INVALID_OTP","message":"The code is invalid.","details":{}}}"""
        val error = ApiErrorDecoder.decode(422, body)
        assertEquals(422, error.httpStatus)
        assertEquals("INVALID_OTP", error.code)
        assertEquals("The code is invalid.", error.message)
    }

    @Test
    fun `decodes field errors`() {
        val body = """{"error":{"code":"VALIDATION","message":"Invalid.","details":{"email":["required"]}}}"""
        val error = ApiErrorDecoder.decode(422, body)
        assertEquals(listOf("required"), error.fieldErrors["email"])
    }

    @Test
    fun `decodes Laravel validation envelope`() {
        val body = """
            {
              "message": "The given data was invalid.",
              "errors": {
                "date_of_birth": ["The date of birth must be before today."]
              }
            }
        """.trimIndent()

        val error = ApiErrorDecoder.decode(422, body)

        assertEquals("VALIDATION", error.code)
        // The specific field message wins over Laravel's generic wrapper text, so callers that
        // just show `error.message` (rather than walking fieldErrors themselves) still surface
        // the actual validation reason instead of a blanket "The given data was invalid."
        assertEquals("The date of birth must be before today.", error.message)
        assertEquals(
            listOf("The date of birth must be before today."),
            error.fieldErrors["date_of_birth"],
        )
    }

    @Test
    fun `null body returns unknown error`() {
        val error = ApiErrorDecoder.decode(500, null)
        assertEquals("UNKNOWN_ERROR", error.code)
        assertEquals(500, error.httpStatus)
    }

    @Test
    fun `blank body returns unknown error`() {
        val error = ApiErrorDecoder.decode(400, "  ")
        assertEquals("UNKNOWN_ERROR", error.code)
    }

    @Test
    fun `malformed json returns unknown error`() {
        val error = ApiErrorDecoder.decode(422, "not json at all")
        assertEquals("UNKNOWN_ERROR", error.code)
    }

    @Test
    fun `empty error object returns unknown error`() {
        val body = """{"error":{}}"""
        val error = ApiErrorDecoder.decode(422, body)
        assertEquals("UNKNOWN_ERROR", error.code)
    }

    @Test
    fun `missing error key returns unknown error`() {
        val body = """{"data":{"id":1}}"""
        val error = ApiErrorDecoder.decode(200, body)
        assertEquals("UNKNOWN_ERROR", error.code)
    }

    @Test
    fun `error without code uses unknown`() {
        val body = """{"error":{"message":"Something happened."}}"""
        val error = ApiErrorDecoder.decode(500, body)
        assertEquals("UNKNOWN_ERROR", error.code)
        assertEquals("Something happened.", error.message)
    }
}
