package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.WizardStepIndicator
import com.eyecare.app.ui.theme.EyecareColors
import java.time.Instant
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                WizardStepIndicator(
                    currentStep = 2,
                    steps = listOf("Schedule", "Details", "Review"),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 16.dp, bottom = 92.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "Review your appointment request",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    ReviewMetadataRow(Icons.Outlined.CalendarMonth, "Date", formatDate(state.date))
                                    ReviewMetadataRow(
                                        Icons.Outlined.AccessTime,
                                        "Time",
                                        formatTimeSlot(state.slot.startsAt, state.slot.endsAt),
                                    )
                                }
                            }
                            ReviewRow(label = "Reason for visit", value = state.reason)
                        }
                    }

                    state.identity?.let { identity ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = "Requester details",
                                    style = MaterialTheme.typography.titleSmall,
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
                                ReviewRow(label = "Date of birth", value = identity.dateOfBirth.orEmpty())
                                ReviewRow(label = "Phone number", value = identity.phone.orEmpty())
                                identity.email?.takeIf(String::isNotBlank)?.let {
                                    ReviewRow(label = "Email", value = it)
                                }
                                ReviewRow(label = "Gender", value = identity.gender?.label.orEmpty())
                                ReviewRow(label = "Occupation", value = identity.occupation.orEmpty())
                                ReviewRow(label = "Home address", value = identity.address.orEmpty())
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    AppointmentPrimaryButton(text = "Submit request", onClick = onSubmit)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestSubmittingContent() {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Request appointment") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Submitting your request…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Unable to submit request",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(24.dp))
            AppointmentPrimaryButton(text = "Try again", onClick = onRetry)
        }
    }
}

@Composable
private fun ReviewMetadataRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EyecareColors.current.accentText,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
        val start = Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE)
        val end = Instant.parse(endsAt).atZone(CLINIC_TIME_ZONE)
        val fmt = DateTimeFormatter.ofPattern("h:mm a")
        "${start.format(fmt)} – ${end.format(fmt)}"
    } catch (_: Exception) {
        "$startsAt – $endsAt"
    }
}
