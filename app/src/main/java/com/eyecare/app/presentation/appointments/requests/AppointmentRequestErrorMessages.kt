package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError

internal fun patientSafeAppointmentRequestError(
    error: Throwable,
    fallback: String,
): String {
    return when ((error as? ApiDomainError)?.code) {
        "SLOT_UNAVAILABLE" -> "That time is no longer available. Please choose another."
        "ACTIVE_REQUEST_LIMIT_REACHED" -> {
            "You already have pending requests. Please wait for them to be resolved or cancel one."
        }
        else -> fallback
    }
}

internal fun ApiDomainError.hasAppointmentRequestFieldError(field: String): Boolean {
    return fieldErrors.keys.any { it == field || it.endsWith(".$field") }
}

internal fun ApiDomainError.isAppointmentTypeUnavailable(): Boolean {
    return httpStatus == 404 ||
        hasAppointmentRequestFieldError("appointment_type_id") ||
        code in setOf(
            "APPOINTMENT_TYPE_UNAVAILABLE",
            "APPOINTMENT_TYPE_INACTIVE",
            "APPOINTMENT_TYPE_NOT_FOUND",
        )
}

internal fun ApiDomainError.isReferralValidationFailure(): Boolean {
    return hasAppointmentRequestFieldError("referring_source") || code == "REFERRAL_SOURCE_REQUIRED"
}
