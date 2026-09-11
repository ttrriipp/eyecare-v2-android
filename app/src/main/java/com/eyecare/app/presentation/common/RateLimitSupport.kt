package com.eyecare.app.presentation.common

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes

private const val DEFAULT_RATE_LIMIT_SECONDS = 60L
private const val MAX_RATE_LIMIT_SECONDS = 15 * 60L

private val rateLimitCodes = setOf(
    AuthApiCodes.API_RATE_LIMIT_REACHED,
    AuthApiCodes.OTP_RATE_LIMIT_REACHED,
    AuthApiCodes.INVITATION_RATE_LIMIT_REACHED,
)

internal fun Throwable.isRateLimited(): Boolean {
    val apiError = this as? ApiDomainError ?: return false
    return apiError.httpStatus == 429 || apiError.code in rateLimitCodes
}

internal fun Throwable.rateLimitCooldownSeconds(): Int {
    if (!isRateLimited()) return 0
    val apiError = this as? ApiDomainError
    return (apiError?.retryAfterSeconds ?: DEFAULT_RATE_LIMIT_SECONDS)
        .coerceIn(1L, MAX_RATE_LIMIT_SECONDS)
        .toInt()
}

internal fun formatRateLimitCooldown(seconds: Int): String = when {
    seconds >= 60 -> {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        if (remainingSeconds == 0) {
            "$minutes minute${if (minutes == 1) "" else "s"}"
        } else {
            "$minutes min $remainingSeconds sec"
        }
    }
    else -> "$seconds second${if (seconds == 1) "" else "s"}"
}
