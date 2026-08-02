package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.presentation.common.components.ErrorContent
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAppointmentScreen(
    onBack: () -> Unit,
    onRequestCreated: (requestId: Int) -> Unit,
    viewModel: RequestAppointmentViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsState()

    when (val s = step) {
        is RequestStep.ChooseDate -> RequestDateContent(
            onDateSelected = { viewModel.selectDate(it) },
            onBack = onBack,
        )
        is RequestStep.ChooseSlot -> RequestSlotContent(
            state = s,
            onSelectSlot = { viewModel.selectSlot(it) },
            onConfirm = { viewModel.confirmSlot() },
            onRetry = { viewModel.retryAvailability() },
            onBack = onBack,
        )
        is RequestStep.EnterReason -> RequestReasonContent(
            state = s,
            onReasonChange = { viewModel.updateReason(it) },
            onConfirm = { viewModel.confirmReason() },
            onBack = { viewModel.backToSlotSelection() },
        )
        is RequestStep.Review -> RequestReviewContent(
            state = s,
            onSubmit = { viewModel.submit() },
            onBack = { viewModel.backToReason() },
        )
        is RequestStep.Submitting -> RequestSubmittingContent()
        is RequestStep.Success -> {
            onRequestCreated(s.request.id)
        }
        is RequestStep.SubmissionError -> RequestSubmissionErrorContent(
            state = s,
            onRetry = { viewModel.handleSubmissionError() },
            onBack = { viewModel.handleSubmissionError() },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDateContent(
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
    )

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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Choose a date for your appointment request.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Select date")
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("Asia/Manila"))
                            .toLocalDate()
                        if (date.isAfter(LocalDate.now().minusDays(1))) {
                            onDateSelected(date.toString())
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestSlotContent(
    state: RequestStep.ChooseSlot,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onConfirm: () -> Unit,
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
        ) {
            Text(
                text = "Available times for ${formatDate(state.date)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(16.dp))

            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
                state.error != null -> {
                    ErrorContent(message = state.error, onRetry = onRetry)
                }
                state.availability == null || state.availability.slots.isEmpty() -> {
                    Text("No available times for this date. Please choose another date.")
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(state.availability.slots) { slot ->
                            SlotCard(
                                slot = slot,
                                isSelected = slot == state.selectedSlot,
                                onClick = { onSelectSlot(slot) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = state.selectedSlot != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Continue")
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotCard(
    slot: AvailabilitySlot,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = slot.available, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = formatTimeRange(slot.startsAt, slot.endsAt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                )
                if (!slot.available && slot.reason != null) {
                    Text(
                        text = "Unavailable",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestReasonContent(
    state: RequestStep.EnterReason,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "What is the reason for your visit?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            androidx.compose.material3.OutlinedTextField(
                value = state.reason,
                onValueChange = onReasonChange,
                label = { Text("Reason for visit") },
                placeholder = { Text("e.g., Blurred vision in left eye") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                isError = state.reasonError != null,
                supportingText = state.reasonError?.let { { Text(it) } },
                maxLines = 5,
            )
            Text(
                text = "${state.reason.length}/1000",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    } catch (_: Exception) {
        dateStr
    }
}

private fun formatTimeRange(startsAt: String, endsAt: String): String {
    return try {
        val start = Instant.parse(startsAt).atZone(ZoneId.of("Asia/Manila"))
        val end = Instant.parse(endsAt).atZone(ZoneId.of("Asia/Manila"))
        val fmt = DateTimeFormatter.ofPattern("h:mm a")
        "${start.format(fmt)} – ${end.format(fmt)}"
    } catch (_: Exception) {
        "$startsAt – $endsAt"
    }
}
