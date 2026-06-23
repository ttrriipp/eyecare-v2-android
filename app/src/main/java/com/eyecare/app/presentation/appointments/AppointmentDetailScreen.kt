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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyecare.app.ui.theme.EyecareTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    onBack: () -> Unit,
    onLeaveFeedback: (Int) -> Unit,
    viewModel: AppointmentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Appointment Detail") },
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
                val appt = state.appointment
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    appt.visitReason.replace("_", " ").replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                StatusChip(appt.status)
                            }
                            DetailRow("Date & Time", appt.scheduledAt.take(16).replace("T", " "))
                            if (!appt.contactNotes.isNullOrBlank())
                                DetailRow("Your notes", appt.contactNotes)
                            if (!appt.staffNotes.isNullOrBlank())
                                DetailRow("Staff notes", appt.staffNotes)
                        }
                    }

                    if (appt.status == AppointmentStatus.COMPLETED && !state.hasFeedback) {
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { onLeaveFeedback(appt.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Leave Feedback")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}


@Preview(showBackground = true)
@Composable
private fun AppointmentDetailPreview() {
    EyecareTheme {
        AppointmentDetailScreen(onBack = {}, onLeaveFeedback = {})
    }
}


