package com.eyecare.app.presentation.auth

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthErrorMessagesTest {

    @Test
    fun `duplicate phone points to sign in`() {
        assertEquals(
            "This phone number already has an account. Sign in instead.",
            authErrorMessage(
                ApiDomainError(422, AuthApiCodes.CONTACT_ALREADY_OWNED, "Phone is already used."),
                "fallback",
            ),
        )
    }

    @Test
    fun `otp rate limit explains the recovery`() {
        assertEquals(
            "Too many code requests. Please wait for the cooldown before trying again.",
            authErrorMessage(
                ApiDomainError(429, AuthApiCodes.OTP_RATE_LIMIT_REACHED, "Rate limited."),
                "fallback",
            ),
        )
    }

    @Test
    fun `server message is preserved when no known code exists`() {
        assertEquals(
            "The service is temporarily unavailable.",
            authErrorMessage(
                ApiDomainError(503, "SERVICE_UNAVAILABLE", "The service is temporarily unavailable."),
                "fallback",
            ),
        )
    }
}
