package com.eyecare.app.domain.model

sealed class FrameReservationError(message: String) : Exception(message) {
    data class ValidationError(val fieldErrors: Map<String, List<String>>) :
        FrameReservationError(fieldErrors.values.flatten().firstOrNull() ?: "Validation failed")
}
