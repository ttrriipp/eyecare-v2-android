package com.eyecare.app.presentation.appointments.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.VisitReason as DomainVisitReason
import java.time.LocalDate

// Time slots generated dynamically (30-min from 9:00-17:00)
private val TIME_SLOTS = BookAppointmentViewModel.generateTimeSlots()

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

        // Step progress bar — 4 segments
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            repeat(4) { index ->
                LinearProgressIndicator(
                    progress = { if (index < state.step) 1f else 0f },
                    modifier = Modifier.weight(1f).height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Spacer(Modifier.height(24.dp))

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                val forward = targetState > initialState
                slideInHorizontally { if (forward) it else -it } + fadeIn() togetherWith
                    slideOutHorizontally { if (forward) -it else it } + fadeOut()
            },
            label = "wizardStep",
        ) { step ->
            when (step) {
                1 -> Step1ReasonSelection(
                    visitReasons = state.visitReasons,
                    isLoading = state.visitReasonsLoading,
                    error = state.visitReasonsError,
                    onRetry = viewModel::retryVisitReasons,
                    onSelectReason = viewModel::selectReason,
                )
                2 -> Step2DateSelection(
                    selectedDate = state.selectedDate,
                    onSelectDate = viewModel::selectDate,
                )
                3 -> Step3TimeSelection(
                    selectedDate = state.selectedDate,
                    onSelectTime = viewModel::selectTime,
                )
                4 -> Step4ConfirmNotes(
                    state = state,
                    onSubmit = viewModel::submit,
                )
            }
        }
    }
}

@Composable
private fun Step1ReasonSelection(
    visitReasons: List<DomainVisitReason>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onSelectReason: (Int, String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Select Visit Reason", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (error != null) {
            Text(
                error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Retry")
            }
        } else {
            visitReasons.forEach { reason ->
                Card(
                    onClick = { onSelectReason(reason.id, reason.name) },
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(Icons.Outlined.RemoveRedEye, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Column {
                            Text(reason.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${reason.durationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step2DateSelection(
    selectedDate: String?,
    onSelectDate: (String) -> Unit,
) {
    val today = remember { LocalDate.now() }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let {
            runCatching { LocalDate.parse(it).toEpochDay() * 86400000L }.getOrNull()
        } ?: (today.toEpochDay() * 86400000L),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = java.time.Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                return !date.isBefore(today)
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Select Date", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        DatePicker(
            state = datePickerState,
            modifier = Modifier.fillMaxWidth(),
            title = null,
            headline = null,
            showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = Color.White,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                headlineContentColor = MaterialTheme.colorScheme.onSurface,
                weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                navigationContentColor = MaterialTheme.colorScheme.onSurface,
                yearContentColor = MaterialTheme.colorScheme.onSurface,
                currentYearContentColor = MaterialTheme.colorScheme.primary,
                selectedYearContainerColor = MaterialTheme.colorScheme.primary,
                selectedYearContentColor = Color.White,
                dayContentColor = MaterialTheme.colorScheme.onSurface,
                selectedDayContainerColor = MaterialTheme.colorScheme.primary,
                selectedDayContentColor = Color.White,
                todayContentColor = MaterialTheme.colorScheme.primary,
                todayDateBorderColor = MaterialTheme.colorScheme.primary,
                disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val millis = datePickerState.selectedDateMillis ?: return@Button
                val date = java.time.Instant.ofEpochMilli(millis)
                    .atZone(java.time.ZoneOffset.UTC).toLocalDate()
                onSelectDate(date.toString())
            },
            enabled = datePickerState.selectedDateMillis != null,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("Next Step")
        }
    }
}

@Composable
private fun Step3TimeSelection(
    selectedDate: String?,
    onSelectTime: (String) -> Unit,
) {
    var selectedTime by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
    ) {
        Text("Select Time", style = MaterialTheme.typography.headlineMedium)
        if (selectedDate != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                selectedDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(16.dp))

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
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                val t = selectedTime ?: return@Button
                onSelectTime(t)
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
private fun Step4ConfirmNotes(state: BookingState, onSubmit: (String?) -> Unit) {
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


