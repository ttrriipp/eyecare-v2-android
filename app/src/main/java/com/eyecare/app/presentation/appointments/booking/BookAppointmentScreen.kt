package com.eyecare.app.presentation.appointments.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Demo: static slots — replace with API-driven availability in production
private val TIME_SLOTS = listOf("09:00", "10:00", "11:30", "14:00", "15:30")

private data class VisitReason(val id: String, val label: String, val icon: ImageVector)
private val VISIT_REASONS = listOf(
    VisitReason("eye_exam", "Eye Exam", Icons.Outlined.RemoveRedEye),
    VisitReason("follow_up", "Follow Up", Icons.Outlined.Vaccines),
    VisitReason("prescription_check", "Prescription Check", Icons.Outlined.Biotech),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    onBack: () -> Unit,
    onBooked: () -> Unit,
    viewModel: BookAppointmentViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.result) {
        if (state.result is BookingResult.Success) onBooked()
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Book Appointment") },
            navigationIcon = {
                IconButton(onClick = { if (state.step > 1) viewModel.goBack() else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        // Step progress bar — 3 segments
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(3) { index ->
                LinearProgressIndicator(
                    progress = { if (index < state.step) 1f else 0f },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        when (state.step) {
            1 -> Step1ReasonSelection(onSelectReason = viewModel::selectReason)
            2 -> Step2DateTimeSelection(
                selectedDateTime = state.selectedDateTime,
                onSelectDateTime = viewModel::selectDateTime,
            )
            3 -> Step3ConfirmNotes(
                state = state,
                onSubmit = viewModel::submit,
            )
        }
    }
}

@Composable
private fun Step1ReasonSelection(onSelectReason: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Select Visit Reason", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        VISIT_REASONS.forEach { reason ->
            Card(
                onClick = { onSelectReason(reason.id) },
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(reason.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(reason.label, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun Step2DateTimeSelection(
    selectedDateTime: String?,
    onSelectDateTime: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val dates = remember { (0..6).map { today.plusDays(it.toLong()) } }

    // Restore from ViewModel state when returning via back navigation
    val initialDate = remember(selectedDateTime) {
        selectedDateTime?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: dates.first()
    }
    val initialTime = remember(selectedDateTime) {
        selectedDateTime?.let { dt -> runCatching { dt.substring(11, 16) }.getOrNull() }
    }

    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(initialTime) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Select Date & Time", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Horizontal date chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(dates) { date ->
                val isSelected = date == selectedDate
                Surface(
                    onClick = { selectedDate = date },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                    shadowElevation = 2.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            date.format(DateTimeFormatter.ofPattern("MMM")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // 2-column time slot grid
        val rows = TIME_SLOTS.chunked(2)
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { time ->
                    val isSelected = time == selectedTime
                    OutlinedCard(
                        onClick = { selectedTime = time },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(
                            1.5.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        colors = CardDefaults.outlinedCardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
                        ),
                    ) {
                        Text(
                            text = formatTimeSlot(time),
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                // fill gap if odd count
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val t = selectedTime ?: return@Button
                val dateTime = "${selectedDate}T${t}:00Z"
                onSelectDateTime(dateTime)
            },
            enabled = selectedTime != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("Next Step")
        }
    }
}

@Composable
private fun Step3ConfirmNotes(state: BookingState, onSubmit: (String?) -> Unit) {
    var notes by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Review & Confirm", style = MaterialTheme.typography.headlineMedium)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Visit Reason", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    state.selectedReason?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text("Date & Time", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(state.selectedDateTime?.take(16)?.replace("T", " ") ?: "", style = MaterialTheme.typography.bodyMedium)
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { if (it.length <= 1000) notes = it },
            label = { Text("Contact notes (optional)") },
            placeholder = { Text("Any notes for the clinic…") },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            maxLines = 5,
        )

        if (state.result is BookingResult.Error) {
            Text((state.result as BookingResult.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onSubmit(notes.takeIf { it.isNotBlank() }) },
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            if (state.isLoading) CircularProgressIndicator(Modifier.height(20.dp))
            else Text("Confirm Booking")
        }
    }
}


