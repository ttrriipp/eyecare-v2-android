package com.eyecare.app.domain.model

sealed class AppointmentError(message: String) : Exception(message) {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) :
        AppointmentError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")

    data class NetworkError(override val message: String) : AppointmentError(message)
}
