package com.eyecare.app.presentation.eyewear

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PaymentQrImageUrlTest {

    @Test
    fun `resolves relative private QR API paths against the API origin`() {
        val apiBase = "https://api.example.test/api/v1/"
        val qrPath = "/api/v1/optical-orders/8/payment-instructions/gcash/qr"

        assertEquals(
            "https://api.example.test/api/v1/optical-orders/8/payment-instructions/gcash/qr",
            buildPaymentQrImageUrl(qrPath, apiBase),
        )
    }

    @Test
    fun `keeps absolute private QR URLs unchanged`() {
        val qrUrl = "https://api.example.test/api/v1/optical-orders/8/payment-instructions/gcash/qr"

        assertEquals(qrUrl, buildPaymentQrImageUrl(qrUrl, "https://other.example.test/api/v1"))
    }
}
