package com.eyecare.app.presentation.auth

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes

internal fun authErrorMessage(error: Throwable, fallback: String): String {
    val apiError = error as? ApiDomainError
    return when (apiError?.code) {
        AuthApiCodes.CONTACT_ALREADY_OWNED ->
            "This phone number already has an account. Sign in instead."
        AuthApiCodes.INVALID_OTP ->
            "That verification code is incorrect. Check the 6 digits and try again."
        AuthApiCodes.OTP_ATTEMPT_LIMIT_REACHED ->
            "Too many incorrect codes. Request a new code and try again."
        AuthApiCodes.OTP_RATE_LIMIT_REACHED ->
            "Too many code requests. Please wait for the cooldown before trying again."
        AuthApiCodes.CONTACT_NOT_VERIFIED ->
            "Verify your phone number before continuing."
        else -> apiError?.message
            ?.takeIf { it.isNotBlank() && it != "Something went wrong. Please try again." }
            ?: error.message?.takeIf(String::isNotBlank)
            ?: fallback
    }
}
