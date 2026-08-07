package com.eyecare.app.domain.model

sealed class AppointmentError(message: String) : Exception(message) {
    data object NotFound : AppointmentError("Appointment not found")

    data class ValidationError(
        val fieldErrors: Map<String, List<String>>,
        val code: String? = null,
    ) :
        AppointmentError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")

    data class NetworkError(override val message: String) : AppointmentError(message)
}
