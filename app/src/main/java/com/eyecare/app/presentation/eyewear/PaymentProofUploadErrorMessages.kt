package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.ApiDomainError
import java.io.IOException

private const val GENERIC_PAYMENT_PROOF_UPLOAD_ERROR = "We couldn't upload your proof. Please try again."

internal fun paymentProofUploadErrorMessage(error: Throwable): String = when {
    error is ApiDomainError && error.httpStatus == 413 ->
        "The server rejected this image as too large. Try a smaller image or contact the clinic."

    error is ApiDomainError && error.code !in setOf("UNKNOWN_ERROR", "INTERNAL_SERVER_ERROR") ->
        error.fieldErrors.values.asSequence()
            .flatten()
            .firstOrNull(String::isNotBlank)
            ?: error.message.takeIf(String::isNotBlank)
            ?: GENERIC_PAYMENT_PROOF_UPLOAD_ERROR

    error is IOException ->
        "Upload couldn't reach the clinic. Check your connection and try again."

    else -> GENERIC_PAYMENT_PROOF_UPLOAD_ERROR
}
