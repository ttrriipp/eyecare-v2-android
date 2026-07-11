package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyecare.app.ui.theme.EyecareTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.ui.theme.OnSurfaceVariant
import com.eyecare.app.ui.theme.StatusCancelled
import com.eyecare.app.ui.theme.StatusConfirmed
import com.eyecare.app.ui.theme.StatusInfo
import com.eyecare.app.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    onBack: () -> Unit,
    onLeaveFeedback: (Int) -> Unit,
    viewModel: AppointmentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel Appointment",
            message = "Are you sure you want to cancel this appointment? This action cannot be undone.",
            confirmLabel = "Cancel Appointment",
            dismissLabel = "Keep Appointment",
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelAppointment()
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    if (uiState is AppointmentDetailUiState.Success) {
        val state = uiState as AppointmentDetailUiState.Success
        if (state.showRescheduleSheet) {
            RescheduleBottomSheet(
                isSubmitting = state.isRescheduling,
                errorMessage = state.rescheduleError,
                onDismiss = viewModel::dismissRescheduleSheet,
                onConfirm = viewModel::rescheduleAppointment,
            )
        }
        if (state.showRescheduleSuccessDialog) {
            AppConfirmationDialog(
                icon = Icons.Outlined.EventAvailable,
                title = "Appointment Rescheduled",
                message = "Your appointment is now set for " +
                    "${formatAppointmentDate(state.appointment.scheduledAt)} at " +
                    "${formatAppointmentTime(state.appointment.scheduledAt)}.",
                confirmLabel = "Got it",
                onConfirm = viewModel::dismissRescheduleSuccessDialog,
                onDismissRequest = viewModel::dismissRescheduleSuccessDialog,
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Appointment details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (val state = uiState) {
            is AppointmentDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is AppointmentDetailUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is AppointmentDetailUiState.Success -> {
                AppointmentDetailContent(
                    state = state,
                    onReschedule = viewModel::showRescheduleSheet,
                    onCancel = { showCancelDialog = true },
                    onLeaveFeedback = { onLeaveFeedback(state.appointment.id) },
                )
            }
        }
    }
}

@Composable
private fun AppointmentDetailContent(
    state: AppointmentDetailUiState.Success,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onLeaveFeedback: () -> Unit,
) {
    val appointment = state.appointment
    val canManage = appointment.status == AppointmentStatus.PENDING ||
        appointment.status == AppointmentStatus.CONFIRMED

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            formatAppointmentTitle(appointment.visitReason),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        appointment.appointmentNumber?.let { number ->
                            Text(
                                number,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    AppointmentDetailStatusBadge(appointment.status)
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AppointmentMetadataRow(
                            icon = Icons.Outlined.CalendarMonth,
                            label = "Date",
                            value = formatAppointmentDate(appointment.scheduledAt),
                        )
                        AppointmentMetadataRow(
                            icon = Icons.Outlined.AccessTime,
                            label = "Time",
                            value = formatAppointmentTime(appointment.scheduledAt),
                        )
                    }
                }

                appointment.assignedOptometrist?.let { optometrist ->
                    AppointmentMetadataRow(
                        icon = Icons.Outlined.Person,
                        label = "Optometrist",
                        value = optometrist.name,
                    )
                }
                appointment.source?.let { source ->
                    AppointmentMetadataRow(
                        icon = Icons.Outlined.Info,
                        label = "Booked via",
                        value = formatAppointmentSource(source),
                    )
                }
            }
        }

        appointment.contactNotes?.takeIf { it.isNotBlank() }?.let { note ->
            AppointmentNoteSection(title = "Your booking note", note = note)
        }
        appointment.staffNotes?.takeIf { it.isNotBlank() }?.let { note ->
            AppointmentNoteSection(title = "Clinic note", note = note)
        }

        state.rescheduleError?.let { AppointmentActionError(it) }
        state.cancelError?.let { AppointmentActionError(it) }

        if (canManage) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onReschedule,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isCancelling,
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Outlined.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Reschedule", fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    enabled = !state.isCancelling,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    if (state.isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.EventBusy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (appointment.status == AppointmentStatus.COMPLETED && !state.hasFeedback) {
            Button(
                onClick = onLeaveFeedback,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
            ) {
                Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Leave feedback", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun AppointmentMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
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
private fun AppointmentNoteSection(title: String, note: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                note,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppointmentDetailStatusBadge(status: AppointmentStatus) {
    val (label, color) = when (status) {
        AppointmentStatus.PENDING -> "Pending" to StatusPending
        AppointmentStatus.CONFIRMED -> "Confirmed" to StatusConfirmed
        AppointmentStatus.ARRIVED -> "Arrived" to StatusInfo
        AppointmentStatus.COMPLETED -> "Completed" to OnSurfaceVariant
        AppointmentStatus.NO_SHOW -> "No show" to StatusCancelled
        AppointmentStatus.CANCELLED -> "Cancelled" to StatusCancelled
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun AppointmentActionError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

internal fun formatAppointmentSource(source: String): String = source
    .replace("_", " ")
    .lowercase()
    .replaceFirstChar { it.uppercase() }


@Preview(showBackground = true)
@Composable
private fun AppointmentDetailPreview() {
    EyecareTheme {
        AppointmentDetailScreen(onBack = {}, onLeaveFeedback = {})
    }
}


