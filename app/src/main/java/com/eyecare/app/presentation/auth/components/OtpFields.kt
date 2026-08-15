package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun OtpField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() }.take(6)
                onValueChange(filtered)
            },
            label = { Text("Verification code") },
            placeholder = { Text("000000") },
            singleLine = true,
            enabled = enabled,
            isError = !error.isNullOrBlank(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(error)
    }
}

@Composable
fun OtpExpiryRow(
    expiresAt: String?,
    canResend: Boolean,
    onResend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expiry = remember(expiresAt) { parseOtpExpiry(expiresAt) }
    var now by remember(expiresAt) { mutableStateOf(Instant.now()) }
    var resendAvailableAt by remember(expiresAt) {
        mutableStateOf(Instant.now().plusSeconds(30))
    }

    LaunchedEffect(expiresAt, resendAvailableAt) {
        while (true) {
            now = Instant.now()
            val expiryFinished = expiry == null || !now.isBefore(expiry)
            val cooldownFinished = !now.isBefore(resendAvailableAt)
            if (expiryFinished && cooldownFinished) break
            delay(1_000)
        }
    }

    val secondsUntilExpiry = expiry?.let { secondsUntil(it, now) }
    val secondsUntilResend = secondsUntil(resendAvailableAt, now)
    val resendEnabled = canResend && secondsUntilResend == 0L

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            when {
                expiry == null && expiresAt != null -> Text(
                    text = "Code expires at $expiresAt",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                expiry != null && secondsUntilExpiry != null && secondsUntilExpiry > 0 -> Text(
                    text = "Code expires in ${formatCountdown(secondsUntilExpiry)} · " +
                        formatOtpExpiry(expiry, ZoneId.systemDefault(), androidx.compose.ui.platform.LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                expiry != null -> Text(
                    text = "Code expired at " +
                        formatOtpExpiry(expiry, ZoneId.systemDefault(), androidx.compose.ui.platform.LocalConfiguration.current.locales[0]),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        if (canResend) {
            TextButton(
                enabled = resendEnabled,
                onClick = {
                    resendAvailableAt = Instant.now().plusSeconds(30)
                    onResend()
                },
            ) {
                Text(
                    text = if (resendEnabled) {
                        "Resend code"
                    } else {
                        "Resend in ${formatCountdown(secondsUntilResend)}"
                    },
                )
            }
        }
    }
}

internal fun parseOtpExpiry(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrElse {
        runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
    }
}

internal fun secondsUntil(target: Instant, now: Instant): Long =
    (target.epochSecond - now.epochSecond).coerceAtLeast(0L)

internal fun formatCountdown(totalSeconds: Long): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(Locale.US, minutes, seconds)
}

internal fun formatOtpExpiry(instant: Instant, zoneId: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern("MMM d, h:mm a", locale)
        .withZone(zoneId)
        .format(instant)


