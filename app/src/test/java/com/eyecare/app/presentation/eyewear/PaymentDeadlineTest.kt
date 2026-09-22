package com.eyecare.app.presentation.eyewear

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant

class PaymentDeadlineTest {

    @Test
    fun `computes remaining seconds from ISO timestamp`() {
        val now = Instant.parse("2026-09-20T02:00:00Z")

        assertEquals(
            90L,
            paymentSecondsRemaining("2026-09-20T02:01:30Z", now),
        )
    }

    @Test
    fun `expired deadline returns zero`() {
        val now = Instant.parse("2026-09-20T02:02:00Z")

        assertEquals(0L, paymentSecondsRemaining("2026-09-20T02:01:30Z", now))
    }

    @Test
    fun `malformed deadline is unavailable`() {
        assertNull(paymentSecondsRemaining("not-a-timestamp", Instant.EPOCH))
    }

    @Test
    fun `formats countdown with minutes and seconds`() {
        assertEquals("01:05", formatPaymentCountdown(65))
        assertEquals("00:00", formatPaymentCountdown(0))
    }
}
