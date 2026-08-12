package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.MAX_RESERVATION_ITEMS
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
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                tint = EyecareColors.current.accentText,
                                modifier = Modifier.size(32.dp),
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            text = "Scheduled visit required",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "You need a scheduled appointment to reserve a frame. " +
                                "The clinic will prepare your frame for your visit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp),
                        )

                        Spacer(Modifier.height(28.dp))

                        Button(
                            onClick = onBookAppointment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        ) {
                            Text(
                                text = "Book an appointment",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.height(48.dp),
                        ) {
                            Text(
                                text = "Go back",
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
                is CreateReservationUiState.Ready -> {
                    AppointmentSelectionContent(
                        state = state,
                        variantId = viewModel.variantId,
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
    variantId: Int,
    onSelectAppointment: (Int) -> Unit,
    onSubmit: () -> Unit,
    onBookAppointment: () -> Unit,
) {
    val existingReservation = state.selectedAppointmentId?.let { state.existingReservationsByAppointment[it] }
    val outcome = mergeOutcome(existingReservation, variantId)
    val canSubmit = outcome is MergeOutcome.None || outcome is MergeOutcome.Mergeable

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
                    existingReservation = state.existingReservationsByAppointment[appointment.id],
                    onSelect = { onSelectAppointment(appointment.id) },
                )
            }
        }

        Text(
            text = mergeOutcomeMessage(outcome),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(50),
            enabled = state.selectedAppointmentId != null && !state.isSubmitting && canSubmit,
        ) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
            } else {
                Text(if (outcome is MergeOutcome.Mergeable) "Add to reservation" else "Confirm reservation")
            }
        }
    }
}

private fun mergeOutcomeMessage(outcome: MergeOutcome): String = when (outcome) {
    MergeOutcome.None -> "Each appointment can have one reservation — pick the visit you'd like this frame ready for."
    is MergeOutcome.Mergeable -> "This visit already has a reservation. Adding will fold this frame into it."
    is MergeOutcome.AlreadyReserved -> "This frame is already reserved for this visit."
    is MergeOutcome.Full -> "This visit's reservation already has the maximum of $MAX_RESERVATION_ITEMS frames."
    is MergeOutcome.Blocked -> "The clinic is already handling this visit's reservation " +
        "(${reservationChipLabel(outcome.reservation.isHeld).lowercase()}) — ask them to add more frames at your visit."
}

@Composable
private fun AppointmentChoiceCard(
    appointment: AppointmentV1,
    isSelected: Boolean,
    existingReservation: FrameReservation?,
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
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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

            existingReservation?.let { reservation ->
                Spacer(Modifier.height(8.dp))
                ExistingReservationBadge(reservation)
            }
        }
    }
}

@Composable
private fun ExistingReservationBadge(reservation: FrameReservation) {
    val cancellable = !reservation.isHeld
    val label = if (cancellable) {
        val count = reservation.items.size
        if (count == 1) "Already has 1 frame — will add to it" else "Already has $count frames — will add to it"
    } else {
        "Clinic already handling this reservation"
    }
    val color = if (cancellable) EyecareColors.current.statusPending else EyecareColors.current.statusInfo

    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
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
