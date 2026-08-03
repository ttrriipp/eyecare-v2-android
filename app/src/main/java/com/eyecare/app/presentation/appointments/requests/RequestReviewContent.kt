package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestReviewContent(
    state: RequestStep.Review,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review request") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Review your appointment request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            ReviewRow(label = "Requested date", value = formatDate(state.date))
            ReviewRow(label = "Requested time", value = formatTimeSlot(state.slot.startsAt, state.slot.endsAt))
            ReviewRow(label = "Reason for visit", value = state.reason)
            state.identity?.let { identity ->
                Text(
                    text = "Requester details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                ReviewRow(
                    label = "Name",
                    value = listOfNotNull(
                        identity.firstName,
                        identity.middleName,
                        identity.lastName,
                    ).joinToString(" "),
                )
                ReviewRow(
                    label = "Date of birth",
                    value = identity.dateOfBirth.orEmpty(),
                )
                ReviewRow(
                    label = "Phone number",
                    value = identity.phone.orEmpty(),
                )
                identity.email?.takeIf(String::isNotBlank)?.let {
                    ReviewRow(label = "Email", value = it)
                }
                ReviewRow(
                    label = "Gender",
                    value = identity.gender?.label.orEmpty(),
                )
                ReviewRow(
                    label = "Occupation",
                    value = identity.occupation.orEmpty(),
                )
                ReviewRow(
                    label = "Home address",
                    value = identity.address.orEmpty(),
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Submit request")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSubmittingContent() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Request appointment") })
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Submitting your request…")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSubmissionErrorContent(
    state: RequestStep.SubmissionError,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request appointment") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Unable to submit request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        java.time.LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatTimeSlot(startsAt: String, endsAt: String): String {
    return try {
        val start = Instant.parse(startsAt).atZone(ZoneId.of("Asia/Manila"))
        val end = Instant.parse(endsAt).atZone(ZoneId.of("Asia/Manila"))
        val fmt = DateTimeFormatter.ofPattern("h:mm a")
        "${start.format(fmt)} – ${end.format(fmt)}"
    } catch (_: Exception) {
        "$startsAt – $endsAt"
    }
}
