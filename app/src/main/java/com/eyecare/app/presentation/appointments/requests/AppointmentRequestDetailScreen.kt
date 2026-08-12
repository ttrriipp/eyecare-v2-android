package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun AppointmentRequestDetailScreen(
    requestId: Int,
    isLinked: Boolean = false,
    onBack: () -> Unit,
    onViewConfirmedAppointment: (Int) -> Unit = {},
    viewModel: AppointmentRequestDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(requestId, isLinked) {
        viewModel.setLinked(isLinked)
        viewModel.load(requestId)
    }

    val state by viewModel.state.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel this request?",
            message = "This can't be undone. Your request will be cancelled.",
            confirmLabel = "Cancel request",
            dismissLabel = "Keep",
            onConfirm = {
                showCancelDialog = false
                viewModel.cancel()
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    when (val s = state) {
        is RequestDetailState.Loading -> RequestDetailLoadingContent(onBack = onBack)
        is RequestDetailState.Data -> RequestDetailDataContent(
            state = s,
            onBack = onBack,
            onCancelClick = { showCancelDialog = true },
            onViewConfirmed = { onViewConfirmedAppointment(it) },
        )
        is RequestDetailState.Error -> ErrorContent(
            message = s.message,
            onRetry = { viewModel.retry() },
        )
        is RequestDetailState.NotFound -> RequestDetailNotFoundContent(onBack = onBack)
    }
}

@Composable
private fun RequestBackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailLoadingContent(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        LoadingContent(modifier = Modifier.fillMaxSize().padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailDataContent(
    state: RequestDetailState.Data,
    onBack: () -> Unit,
    onCancelClick: () -> Unit,
    onViewConfirmed: (Int) -> Unit,
) {
    val presentation = requestStatusPresentation(state.request.status)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request ${state.request.requestNumber}") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            presentation.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        AppointmentRequestStatusPill(state.request.status, presentation.label)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            DetailMetadataRow(
                                Icons.Outlined.CalendarMonth,
                                "Date",
                                formatDetailDate(state.request.scheduledAt),
                            )
                            DetailMetadataRow(
                                Icons.Outlined.AccessTime,
                                "Preferred time",
                                formatDetailTime(state.request.scheduledAt),
                            )
                            state.request.appointmentType?.let { type ->
                                DetailMetadataRow(
                                    Icons.Outlined.EventAvailable,
                                    "Type",
                                    "${type.name} (${type.durationMinutes} min)",
                                )
                            }
                        }
                    }

                    if (state.request.alternativeScheduledTimes.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Alternative times",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            state.request.alternativeScheduledTimes.forEachIndexed { index, time ->
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = "Alternative ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatDetailDateTime(time),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    DetailRow(label = "Reason for visit", value = state.request.reasonForVisit)

                    state.request.referringSource?.let {
                        DetailRow(label = "Referral source", value = it)
                    }

                    state.request.cancelledAt?.let {
                        DetailRow(label = "Cancelled", value = formatDetailDateTime(it))
                    }

                    if (state.request.status == AppointmentRequestStatus.REJECTED) {
                        state.request.rejectionReason?.takeIf { it.isNotBlank() }?.let { reason ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Reason",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                    Text(
                                        text = reason,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            state.cancelError?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }

            if (presentation.showCancel && state.request.status.isCancellable) {
                AppointmentOutlinedButton(
                    text = "Cancel request",
                    onClick = onCancelClick,
                    enabled = !state.isCancelling,
                    loading = state.isCancelling,
                    icon = Icons.Outlined.EventBusy,
                    isDestructive = true,
                )
            }

            if (presentation.showViewConfirmed && state.isLinked && state.request.appointmentId != null) {
                AppointmentPrimaryButton(
                    text = "View confirmed appointment",
                    onClick = { onViewConfirmed(state.request.appointmentId) },
                    icon = Icons.Outlined.EventAvailable,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailNotFoundContent(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        EmptyContent(
            message = "This request may belong to another account or has been removed.",
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
private fun DetailMetadataRow(icon: ImageVector, label: String, value: String) {
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
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatDetailDate(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (_: Exception) { iso }

private fun formatDetailTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
} catch (_: Exception) { iso }

private fun formatDetailDateTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
} catch (_: Exception) { iso }
