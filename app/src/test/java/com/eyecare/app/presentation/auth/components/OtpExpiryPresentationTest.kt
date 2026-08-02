package com.eyecare.app.presentation.auth.components

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OtpExpiryPresentationTest {

    @Test
    fun `parses API expiry timestamps with an offset`() {
        assertEquals(
            Instant.parse("2026-08-01T02:10:00Z"),
            parseOtpExpiry("2026-08-01T10:10:00+08:00"),
        )
    }

    @Test
    fun `formats expiry in the user's local time`() {
        assertEquals(
            "Aug 1, 6:10 PM",
            formatOtpExpiry(
                instant = Instant.parse("2026-08-01T10:10:00Z"),
                zoneId = ZoneId.of("Asia/Manila"),
                locale = Locale.US,
            ),
        )
    }

    @Test
    fun `formats countdown with minutes and seconds`() {
        assertEquals("01:09", formatCountdown(69))
        assertEquals("00:00", formatCountdown(0))
    }
}
