package com.eyecare.app.presentation.eyewear

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun parsePaymentDeadline(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrElse {
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
    }
}

internal fun paymentSecondsRemaining(expiresAt: String?, now: Instant = Instant.now()): Long? {
    val deadline = parsePaymentDeadline(expiresAt) ?: return null
    return Duration.between(now, deadline).seconds.coerceAtLeast(0)
}

internal fun formatPaymentCountdown(seconds: Long): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val remainingSeconds = safeSeconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

internal fun formatPaymentDeadline(value: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
    val instant = parsePaymentDeadline(value) ?: return value
    return DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a", Locale.US)
        .withZone(zoneId)
        .format(instant)
}
