package com.eyecare.app.domain.model

sealed class OrderError(message: String) : Exception(message) {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) :
        OrderError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")
    data class NetworkError(override val message: String) : OrderError(message)
}
