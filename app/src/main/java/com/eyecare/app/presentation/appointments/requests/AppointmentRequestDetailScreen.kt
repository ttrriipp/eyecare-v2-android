package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent

@Composable
fun AppointmentRequestDetailScreen(
    requestId: Int,
    isLinked: Boolean = false,
    onBack: () -> Unit,
    onViewConfirmedAppointment: (Int) -> Unit = {},
    viewModel: AppointmentRequestDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(requestId) {
        viewModel.load(requestId)
        viewModel.setLinked(isLinked)
    }

    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is RequestDetailState.Loading -> RequestDetailLoadingContent(onBack = onBack)
        is RequestDetailState.Data -> RequestDetailDataContent(
            state = s,
            onBack = onBack,
            onCancel = { viewModel.cancel() },
            onViewConfirmed = { onViewConfirmedAppointment(it) },
        )
        is RequestDetailState.Error -> ErrorContent(
            message = s.message,
            onRetry = { viewModel.retry() },
        )
        is RequestDetailState.NotFound -> RequestDetailNotFoundContent(onBack = onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailLoadingContent(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailDataContent(
    state: RequestDetailState.Data,
    onBack: () -> Unit,
    onCancel: () -> Unit,
    onViewConfirmed: (Int) -> Unit,
) {
    val presentation = requestStatusPresentation(state.request.status)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request ${state.request.requestNumber}") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DetailRow(label = "Status", value = presentation.label)
            DetailRow(label = "Requested date", value = formatDetailDate(state.request.scheduledAt))
            DetailRow(label = "Requested time", value = formatDetailTime(state.request.scheduledAt))
            DetailRow(label = "Reason for visit", value = state.request.reasonForVisit)

            state.request.expiresAt?.let {
                if (state.request.status == AppointmentRequestStatus.PENDING) {
                    DetailRow(label = "Expires", value = formatDetailDate(it))
                }
            }

            state.request.cancelledAt?.let {
                DetailRow(label = "Cancelled", value = formatDetailDateTime(it))
            }

            if (presentation.showCancel && state.request.status.isCancellable) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onCancel,
                    enabled = !state.isCancelling,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    if (state.isCancelling) CircularProgressIndicator() else Text("Cancel request")
                }
            }

            state.cancelError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            if (presentation.showViewConfirmed && state.isLinked && state.request.appointmentId != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onViewConfirmed(state.request.appointmentId) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("View confirmed appointment")
                }
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
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Request not found", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("This request may belong to another account or has been removed.")
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
    java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Manila"))
        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (_: Exception) { iso }

private fun formatDetailTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Manila"))
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
} catch (_: Exception) { iso }

private fun formatDetailDateTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(java.time.ZoneId.of("Asia/Manila"))
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
} catch (_: Exception) { iso }
