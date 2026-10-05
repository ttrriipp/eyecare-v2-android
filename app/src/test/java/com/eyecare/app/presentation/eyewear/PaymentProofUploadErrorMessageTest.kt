package com.eyecare.app.presentation.eyewear

import com.eyecare.app.domain.model.ApiDomainError
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException

class PaymentProofUploadErrorMessageTest {

    @Test
    fun `shows a patient-safe proof validation message from the API`() {
        val error = ApiDomainError(
            httpStatus = 422,
            code = "VALIDATION",
            message = "The given data was invalid.",
            fieldErrors = mapOf("proof" to listOf("The selected proof must be an image.")),
        )

        assertEquals(
            "The selected proof must be an image.",
            paymentProofUploadErrorMessage(error),
        )
    }

    @Test
    fun `explains when the server rejects an oversized request`() {
        val error = ApiDomainError.unknown(httpStatus = 413)

        assertEquals(
            "The server rejected this image as too large. Try a smaller image or contact the clinic.",
            paymentProofUploadErrorMessage(error),
        )
    }

    @Test
    fun `does not expose internal server details`() {
        val error = ApiDomainError(
            httpStatus = 500,
            code = "INTERNAL_SERVER_ERROR",
            message = "Private database connection details",
        )

        assertEquals(
            "We couldn't upload your proof. Please try again.",
            paymentProofUploadErrorMessage(error),
        )
    }

    @Test
    fun `gives a useful message for connection failures`() {
        assertEquals(
            "Upload couldn't reach the clinic. Check your connection and try again.",
            paymentProofUploadErrorMessage(IOException("Connection reset")),
        )
    }
}
