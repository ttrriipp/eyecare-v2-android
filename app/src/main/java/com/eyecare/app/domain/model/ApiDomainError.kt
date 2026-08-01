package com.eyecare.app.domain.model

class ApiDomainError(
    val httpStatus: Int,
    val code: String,
    override val message: String,
    val fieldErrors: Map<String, List<String>> = emptyMap(),
) : Exception(message) {
    companion object {
        fun unknown(httpStatus: Int, fallbackMessage: String = "Something went wrong. Please try again.") =
            ApiDomainError(
                httpStatus = httpStatus,
                code = "UNKNOWN_ERROR",
                message = fallbackMessage,
            )
    }
}

object AuthApiCodes {
    const val INVALID_OTP = "INVALID_OTP"
    const val OTP_ATTEMPT_LIMIT_REACHED = "OTP_ATTEMPT_LIMIT_REACHED"
    const val OTP_RATE_LIMIT_REACHED = "OTP_RATE_LIMIT_REACHED"
    const val CONTACT_ALREADY_OWNED = "CONTACT_ALREADY_OWNED"
    const val INVITATION_INVALID = "INVITATION_INVALID"
    const val ACCOUNT_ALREADY_LINKED = "ACCOUNT_ALREADY_LINKED"
    const val PATIENT_ALREADY_LINKED = "PATIENT_ALREADY_LINKED"
    const val CONTACT_NOT_VERIFIED = "CONTACT_NOT_VERIFIED"
    const val LAST_CONTACT_REMAINING = "LAST_CONTACT_REMAINING"
    const val ACTIVE_PATIENT_LINK_REQUIRED = "ACTIVE_PATIENT_LINK_REQUIRED"
    const val LINK_REQUEST_PENDING = "LINK_REQUEST_PENDING"
    const val SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE"
    const val ACTIVE_REQUEST_LIMIT_REACHED = "ACTIVE_REQUEST_LIMIT_REACHED"
    const val REQUEST_NOT_CANCELLABLE = "REQUEST_NOT_CANCELLABLE"
    const val REQUEST_NOT_OWNED = "REQUEST_NOT_OWNED"
    const val REQUEST_TERMINAL = "REQUEST_TERMINAL"
    const val PATIENT_RESOLUTION_REQUIRED = "PATIENT_RESOLUTION_REQUIRED"
}
