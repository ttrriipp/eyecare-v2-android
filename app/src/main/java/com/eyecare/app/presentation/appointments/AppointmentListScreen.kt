package com.eyecare.app.presentation.appointments

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.eyecare.app.ui.theme.EyecareTheme
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.ui.theme.StatusCancelled
import com.eyecare.app.ui.theme.StatusConfirmed
import com.eyecare.app.ui.theme.StatusInfo
import com.eyecare.app.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToBook: () -> Unit,
    viewModel: AppointmentListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState is AppointmentListUiState.Loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = uiState) {
                is AppointmentListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AppointmentListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No appointments yet.", style = MaterialTheme.typography.bodyMedium)
                }
                is AppointmentListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is AppointmentListUiState.Success -> LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text("Appointments", style = MaterialTheme.typography.displayLarge)
                        Spacer(Modifier.height(8.dp))
                    }
                    items(state.appointments, key = { it.id }) { appt ->
                        AppointmentCard(appointment = appt, onClick = { onNavigateToDetail(appt.id) })
                    }
                }
            }
        }

        Surface(
            onClick = onNavigateToBook,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 96.dp),
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 2.dp,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    "Book New",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun AppointmentCard(appointment: Appointment, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    appointment.visitReason.replace("_", " ").replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                )
                StatusChip(appointment.status)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                appointment.scheduledAt.take(10), // show date part
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StatusChip(status: AppointmentStatus) {
    val (label, color) = when (status) {
        AppointmentStatus.PENDING -> "Pending" to StatusPending
        AppointmentStatus.CONFIRMED -> "Confirmed" to StatusConfirmed
        AppointmentStatus.COMPLETED -> "Completed" to StatusConfirmed
        AppointmentStatus.CANCELLED -> "Cancelled" to StatusCancelled
        AppointmentStatus.RESCHEDULED -> "Rescheduled" to StatusInfo
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.15f)),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = color),
    )
}


@Preview(showBackground = true)
@Composable
private fun AppointmentListPreview() {
    EyecareTheme {
        AppointmentListScreen(onNavigateToDetail = {}, onNavigateToBook = {})
    }
}
