package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.ui.theme.EyecareColors
import com.eyecare.app.presentation.common.components.ErrorContent
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFrameReservationScreen(
    frameId: Int,
    variantId: Int,
    onBack: () -> Unit,
    onSuccess: (reservationId: Int) -> Unit,
    onBookAppointment: () -> Unit = {},
) {
    val viewModel = hiltViewModel<CreateFrameReservationViewModel, CreateFrameReservationViewModel.Factory> {
        it.create(frameId, variantId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is CreateReservationUiState.Success -> {
            onSuccess(state.reservation.id)
            return
        }
        else -> {}
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Reserve Frame", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            when (val state = uiState) {
                is CreateReservationUiState.LoadingAppointments -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Finding available appointments...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                is CreateReservationUiState.AppointmentLoadError -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = viewModel::retryLoadAppointments,
                    )
                }
                is CreateReservationUiState.NoEligibleAppointments -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text(
                            "Scheduled visit required",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "You need a scheduled appointment to reserve a frame. The clinic will prepare your frame for your visit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = onBookAppointment,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text("Book an appointment")
                        }
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                        ) {
                            Text("Back")
                        }
                    }
                }
                is CreateReservationUiState.Ready -> {
                    AppointmentSelectionContent(
                        state = state,
                        onSelectAppointment = viewModel::selectAppointment,
                        onSubmit = viewModel::submit,
                        onBookAppointment = onBookAppointment,
                    )
                }
                is CreateReservationUiState.Success -> { /* handled above */ }
            }
        }
    }
}

@Composable
private fun AppointmentSelectionContent(
    state: CreateReservationUiState.Ready,
    onSelectAppointment: (Int) -> Unit,
    onSubmit: () -> Unit,
    onBookAppointment: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            "Select a visit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "The clinic will prepare your selected frame for the visit you choose.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        state.appointmentFieldError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        state.itemFieldError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        state.genericError?.let { error ->
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.eligibleAppointments) { appointment ->
                AppointmentChoiceCard(
                    appointment = appointment,
                    isSelected = appointment.id == state.selectedAppointmentId,
                    onSelect = { onSelectAppointment(appointment.id) },
                )
            }
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            enabled = state.selectedAppointmentId != null && !state.isSubmitting,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text("Confirm reservation")
            }
        }
    }
}

@Composable
private fun AppointmentChoiceCard(
    appointment: AppointmentV1,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = isSelected, onClick = onSelect)
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Outlined.Event, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    appointment.appointmentNumber ?: "Appointment #${appointment.id}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatAppointmentSchedule(appointment),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${appointment.durationMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (isSelected) {
                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = EyecareColors.current.accentText)
            }
        }
    }
}

private fun formatAppointmentSchedule(appointment: AppointmentV1): String {
    return try {
        val odt = OffsetDateTime.parse(appointment.scheduledAt)
        "${odt.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} at ${odt.format(DateTimeFormatter.ofPattern("h:mm a"))}"
    } catch (_: Exception) {
        appointment.scheduledAt.take(16)
    }
}
