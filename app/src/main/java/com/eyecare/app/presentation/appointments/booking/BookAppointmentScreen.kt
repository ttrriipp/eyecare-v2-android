package com.eyecare.app.presentation.appointments.booking

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.VisitReason as DomainVisitReason
import java.time.DayOfWeek
import java.time.LocalDate

// AM/PM and valid hour sets are defined per-composable — no file-level time slot list needed

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
                    modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            reason.name,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${reason.durationMinutes} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                // Block past dates and Sundays — clinic is closed on Sundays
                return !date.isBefore(today) && date.dayOfWeek != java.time.DayOfWeek.SUNDAY
            }
        },
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
                containerColor = MaterialTheme.colorScheme.surface,
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

// ── Clinic hours: 9:00 AM - 5:00 PM ──────────────────────────────────────────
private fun isValidClinicTime(hour12: Int, minute: Int, isPm: Boolean): Boolean {
    val hour24 = when {
        !isPm && hour12 == 12 -> 0
        isPm && hour12 != 12  -> hour12 + 12
        else                  -> hour12
    }
    return (hour24 * 60 + minute) in (9 * 60)..(17 * 60)
}

@Composable
private fun Step3TimeSelection(
    selectedDate: String?,
    onSelectTime: (String) -> Unit,
) {
    // Start at 9:00 AM — first valid slot
    var isPm by remember { mutableStateOf(false) }
    var hour by remember { mutableIntStateOf(9) }   // 12-hr format (1–12)
    var minute by remember { mutableIntStateOf(0) } // 0, 15, 30, 45

    // ── Hour navigation ──────────────────────────────────────────────────────
    // AM: 9→10→11→[flip to PM 12→1→…→5→wrap PM 12]
    // Going backwards: PM 12→[flip to AM 11→10→9→wrap AM 11]
    fun nextHour() {
        if (!isPm && hour == 11) {
            // Cross the AM/PM boundary going forward
            isPm = true
            hour = 12
        } else if (isPm) {
            hour = when (hour) { 12 -> 1; 5 -> 12; else -> hour + 1 }
        } else {
            hour += 1
        }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }
    fun prevHour() {
        if (isPm && hour == 12) {
            // Cross the AM/PM boundary going backward
            isPm = false
            hour = 11
        } else if (!isPm) {
            hour = when (hour) { 9 -> 11; else -> hour - 1 }
        } else {
            hour = when (hour) { 1 -> 12; else -> hour - 1 }
        }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }

    // ── Minute navigation (15-min steps, guarded by clinic end time) ────────
    fun nextMinute() {
        val n = if (minute >= 45) 0 else minute + 15
        if (isValidClinicTime(hour, n, isPm)) minute = n
    }
    fun prevMinute() {
        val p = if (minute <= 0) 45 else minute - 15
        if (isValidClinicTime(hour, p, isPm)) minute = p
    }

    // ── AM / PM switch — clamp hour into the new period's valid range ────────
    fun switchPeriod(newIsPm: Boolean) {
        isPm = newIsPm
        hour = if (newIsPm) { if (hour in 1..5 || hour == 12) hour else 12 }
               else         { if (hour in 9..11) hour else 9 }
        if (!isValidClinicTime(hour, minute, isPm)) minute = 0
    }

    // 24-hr string for submission
    val hour24 = when {
        !isPm && hour == 12 -> 0
        isPm && hour != 12  -> hour + 12
        else                -> hour
    }
    val timeString = "%02d:%02d".format(hour24, minute)

    val primary      = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val surfaceVar   = MaterialTheme.colorScheme.surfaceVariant
    val onSurfaceVar = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Select Time", style = MaterialTheme.typography.headlineMedium)
            Text(
                "9:00 AM – 5:00 PM",
                style = MaterialTheme.typography.labelSmall,
                color = primary,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(52.dp))

        // ── Digital clock row ─────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            // Hour segment
            TimeSegment(
                value = "%02d".format(hour),
                onUp = ::nextHour,
                onDown = ::prevHour,
                primary = primary,
                onSurface = onSurface,
            )

            // Colon separator
            Text(
                ":",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                ),
                color = onSurface,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 8.dp),
            )

            // Minute segment
            TimeSegment(
                value = "%02d".format(minute),
                onUp = ::nextMinute,
                onDown = ::prevMinute,
                primary = primary,
                onSurface = onSurface,
            )

            Spacer(Modifier.width(20.dp))

            // AM / PM stack
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                listOf(false to "AM", true to "PM").forEach { (pm, label) ->
                    val selected = isPm == pm
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) primary else surfaceVar)
                            .clickable { switchPeriod(pm) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else onSurfaceVar,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(72.dp))

        Button(
            onClick = { onSelectTime(timeString) },
            enabled = isValidClinicTime(hour, minute, isPm),
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("Next Step")
        }

        Spacer(Modifier.height(16.dp))
    }
}

/** Up-arrow → value text → down-arrow; used for both hour and minute. */
@Composable
private fun TimeSegment(
    value: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    primary: Color,
    onSurface: Color,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = onUp, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowUp,
                contentDescription = "Increase",
                tint = primary,
                modifier = Modifier.size(36.dp),
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 64.sp,
                fontWeight = FontWeight.Bold,
            ),
            color = onSurface,
        )
        IconButton(onClick = onDown, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Filled.KeyboardArrowDown,
                contentDescription = "Decrease",
                tint = primary,
                modifier = Modifier.size(36.dp),
            )
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


