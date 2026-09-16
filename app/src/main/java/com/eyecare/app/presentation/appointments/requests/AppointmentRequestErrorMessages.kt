package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.presentation.appointments.SAME_DAY_CANCELLATION_MESSAGE
import com.eyecare.app.presentation.appointments.CANCELLATION_REASON_REQUIRED_MESSAGE
import com.eyecare.app.presentation.appointments.isSameDayCancellationError
import com.eyecare.app.presentation.appointments.isCancellationReasonValidationError

internal fun patientSafeAppointmentRequestError(
    error: Throwable,
    fallback: String,
): String {
    if (isSameDayCancellationError(error)) return SAME_DAY_CANCELLATION_MESSAGE
    if (isCancellationReasonValidationError(error)) return CANCELLATION_REASON_REQUIRED_MESSAGE

    return when ((error as? ApiDomainError)?.code) {
        AuthApiCodes.SLOT_UNAVAILABLE -> "That time is no longer available. Please choose another."
        AuthApiCodes.ACTIVE_REQUEST_LIMIT_REACHED -> {
            "You already have a pending appointment request. Please wait for the clinic to respond or cancel it before sending another."
        }
        AuthApiCodes.ACTIVE_APPOINTMENT_EXISTS -> {
            "You already have an active appointment. You may reschedule or cancel it before requesting another."
        }
        "IDENTITY_NOT_ALLOWED" -> {
            "Your account is already linked. Please start the appointment request again."
        }
        "PATIENT_RESOLUTION_REQUIRED" -> {
            "Your clinic link needs to be refreshed. Please sign in again and retry."
        }
        "ACTIVE_PATIENT_LINK_REQUIRED" -> {
            "Your clinic link is no longer active. Please refresh your session and retry."
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
