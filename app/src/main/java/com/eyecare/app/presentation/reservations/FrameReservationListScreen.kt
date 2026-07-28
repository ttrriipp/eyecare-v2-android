package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.isCancellable
import com.eyecare.app.presentation.common.components.ErrorContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameReservationListScreen(
    onBack: () -> Unit,
    viewModel: FrameReservationListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var cancelTarget by remember { mutableStateOf<FrameReservation?>(null) }

    if (cancelTarget != null) {
        AlertDialog(
            onDismissRequest = { cancelTarget = null },
            title = { Text("Cancel reservation?") },
            text = { Text("This will cancel your frame reservation.") },
            confirmButton = {
                TextButton(onClick = {
                    cancelTarget?.let { viewModel.cancelReservation(it.id) }
                    cancelTarget = null
                }) { Text("Cancel reservation") }
            },
            dismissButton = {
                TextButton(onClick = { cancelTarget = null }) { Text("Keep") }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Reservations", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        PullToRefreshBox(
            isRefreshing = uiState is ReservationListUiState.Loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = uiState) {
                is ReservationListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ReservationListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is ReservationListUiState.Success -> {
                    if (state.reservations.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No reservations yet", style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.reservations, key = { it.id }) { reservation ->
                                ReservationCard(
                                    reservation = reservation,
                                    onCancel = { cancelTarget = reservation },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationCard(
    reservation: FrameReservation,
    onCancel: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Reservation #${reservation.id}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                StatusChip(reservation.status)
            }

            Spacer(Modifier.height(8.dp))

            // Appointment context
            val appt = reservation.appointment
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Column {
                    Text(
                        appt.appointmentNumber ?: "Appointment #${appt.id}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        formatReservationSchedule(appt.scheduledAt, appt.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            reservation.items.forEach { item ->
                Text(
                    item.frameName,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    "${item.variantName} — ${item.frameBrand}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (reservation.isCancellable) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel reservation")
                }
            }
        }
    }
}

private fun formatReservationSchedule(scheduledAt: String, durationMinutes: Int): String {
    return try {
        val odt = java.time.OffsetDateTime.parse(scheduledAt)
        val dateStr = odt.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        val timeStr = odt.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
        "$dateStr at $timeStr · ${durationMinutes}min"
    } catch (_: Exception) {
        scheduledAt.take(16)
    }
}

@Composable
private fun StatusChip(status: ReservationStatus) {
    val (label, color) = when (status) {
        ReservationStatus.REQUESTED -> "Requested" to MaterialTheme.colorScheme.primary
        ReservationStatus.PREPARED -> "Prepared" to MaterialTheme.colorScheme.tertiary
        ReservationStatus.TRIED_ON -> "Tried on" to MaterialTheme.colorScheme.secondary
        ReservationStatus.CONVERTED -> "Converted" to MaterialTheme.colorScheme.primary
        ReservationStatus.RELEASED -> "Released" to MaterialTheme.colorScheme.onSurfaceVariant
        ReservationStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.error
        ReservationStatus.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.12f)),
        border = null,
    )
}
