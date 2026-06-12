package com.eyecare.app.domain.model

sealed class AuthError(message: String) : Exception(message) {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) :
        AuthError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")

    data object RateLimitError : AuthError("Too many attempts. Please try again later.")
    data class NetworkError(override val message: String) : AuthError(message)
}
