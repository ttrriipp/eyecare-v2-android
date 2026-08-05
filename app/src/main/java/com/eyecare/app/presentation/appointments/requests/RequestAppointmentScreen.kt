package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.WizardStepIndicator
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val wizardSteps = listOf("Schedule", "Details", "Review")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestAppointmentScreen(
    onBack: () -> Unit,
    onRequestCreated: (requestId: Int) -> Unit,
    requestIdentity: AppointmentRequestIdentity? = null,
    identityDetailsRequired: Boolean = false,
    viewModel: RequestAppointmentViewModel = hiltViewModel(),
) {
    val step by viewModel.step.collectAsState()

    when (val s = step) {
        is RequestStep.Schedule -> ScheduleContent(
            state = s,
            onDateSelected = { viewModel.selectDate(it) },
            onSelectSlot = { viewModel.selectSlot(it) },
            onRetry = { viewModel.retryAvailability() },
            onConfirm = {
                viewModel.confirmSchedule(
                    identityDetailsRequired = identityDetailsRequired,
                    initialIdentity = requestIdentity,
                )
            },
            onBack = onBack,
        )
        is RequestStep.ProfileAndReason -> ProfileAndReasonContent(
            state = s,
            onReasonChange = { viewModel.updateReason(it) },
            onEmailChange = { viewModel.updateIdentity(email = it) },
            onFirstNameChange = { viewModel.updateIdentity(firstName = it) },
            onMiddleNameChange = { viewModel.updateIdentity(middleName = it) },
            onLastNameChange = { viewModel.updateIdentity(lastName = it) },
            onDateOfBirthChange = { viewModel.updateIdentity(dateOfBirth = it) },
            onGenderChange = { viewModel.updateIdentity(gender = it) },
            onOccupationChange = { viewModel.updateIdentity(occupation = it) },
            onAddressChange = { viewModel.updateIdentity(address = it) },
            onConfirm = { viewModel.confirmProfileAndReason() },
            onBack = { viewModel.backToSchedule() },
        )
        is RequestStep.Review -> RequestReviewContent(
            state = s,
            onSubmit = { viewModel.submit() },
            onBack = { viewModel.backFromReview() },
        )
        is RequestStep.Submitting -> RequestSubmittingContent()
        is RequestStep.Success -> RequestSuccessContent(
            state = s,
            onContinue = { onRequestCreated(s.request.id) },
        )
        is RequestStep.SubmissionError -> RequestSubmissionErrorContent(
            state = s,
            onRetry = { viewModel.handleSubmissionError() },
            onBack = { viewModel.handleSubmissionError() },
        )
    }
}

@Composable
private fun RequestBackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleContent(
    state: RequestStep.Schedule,
    onDateSelected: (String) -> Unit,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.date?.toDatePickerMillis() ?: System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis).atZone(CLINIC_TIME_ZONE).toLocalDate()
                return !date.isBefore(today) && date.dayOfWeek != java.time.DayOfWeek.SUNDAY
            }
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request appointment") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Wizard step indicator
            WizardStepIndicator(
                currentStep = 0,
                steps = wizardSteps,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    text = "Choose a date and time for your appointment.",
                    style = MaterialTheme.typography.bodyLarge,
                )

                // Date selector
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Date")
                    AppointmentOutlinedButton(
                        text = state.date?.let { formatDate(it) } ?: "Select date",
                        onClick = { showDatePicker = true },
                        icon = Icons.Outlined.CalendarMonth,
                    )
                }

                // Time slots
                if (state.date != null) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionLabel("Available times")

                        when {
                            state.isLoadingAvailability -> {
                                LoadingContent(modifier = Modifier.fillMaxWidth().weight(1f))
                            }
                            else -> {
                                // Use real slots if available, otherwise show placeholder slots for UI preview
                                val realSlots = state.availability?.slots?.filter { it.available } ?: emptyList()
                                val availableSlots = realSlots.ifEmpty { placeholderSlots() }
                                val morningSlots = availableSlots.filter { slot ->
                                    val time = parseSlotTime(slot.startsAt)
                                    time != null && time.hour < 12
                                }
                                val afternoonSlots = availableSlots.filter { slot ->
                                    val time = parseSlotTime(slot.startsAt)
                                    time != null && time.hour >= 12
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    contentPadding = PaddingValues(bottom = 16.dp),
                                ) {
                                    if (morningSlots.isNotEmpty()) {
                                        item {
                                            TimePeriodHeader(
                                                icon = Icons.Outlined.WbSunny,
                                                label = "Morning",
                                                count = morningSlots.size,
                                            )
                                        }
                                        items(morningSlots) { slot ->
                                            SlotCard(
                                                slot = slot,
                                                isSelected = slot == state.selectedSlot,
                                                onClick = { onSelectSlot(slot) },
                                            )
                                        }
                                    }

                                    if (afternoonSlots.isNotEmpty()) {
                                        item {
                                            TimePeriodHeader(
                                                icon = Icons.Outlined.DarkMode,
                                                label = "Afternoon",
                                                count = afternoonSlots.size,
                                            )
                                        }
                                        items(afternoonSlots) { slot ->
                                            SlotCard(
                                                slot = slot,
                                                isSelected = slot == state.selectedSlot,
                                                onClick = { onSelectSlot(slot) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                AppointmentPrimaryButton(
                    text = "Continue",
                    onClick = onConfirm,
                    enabled = state.selectedSlot != null,
                )
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(CLINIC_TIME_ZONE).toLocalDate()
                        onDateSelected(date.toString())
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

@Composable
private fun TimePeriodHeader(
    icon: ImageVector,
    label: String,
    count: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = EyecareColors.current.accentText,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "$count available",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SlotCard(
    slot: AvailabilitySlot,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = slot.available, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isSelected) 0.dp else 1.dp,
        border = if (isSelected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        },
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = formatTimeRange(slot.startsAt, slot.endsAt),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) EyecareColors.current.accentText
                    else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${formatSlotDuration(slot.startsAt, slot.endsAt)} slot",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.EventAvailable,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestSuccessContent(
    state: RequestStep.Success,
    onContinue: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Request appointment") }) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(EyecareColors.current.statusConfirmed.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.EventAvailable,
                    contentDescription = null,
                    tint = EyecareColors.current.statusConfirmed,
                    modifier = Modifier.size(36.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Request sent",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Request ${state.request.requestNumber} is now awaiting clinic review. " +
                    "We'll notify you once it's confirmed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            AppointmentPrimaryButton(text = "View request", onClick = onContinue)
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
        val start = Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE)
        val end = Instant.parse(endsAt).atZone(CLINIC_TIME_ZONE)
        val fmt = DateTimeFormatter.ofPattern("h:mm a")
        "${start.format(fmt)} – ${end.format(fmt)}"
    } catch (_: Exception) {
        "$startsAt – $endsAt"
    }
}

private fun formatSlotDuration(startsAt: String, endsAt: String): String {
    return try {
        val start = Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE)
        val end = Instant.parse(endsAt).atZone(CLINIC_TIME_ZONE)
        val minutes = java.time.Duration.between(start, end).toMinutes()
        "${minutes}min"
    } catch (_: Exception) {
        ""
    }
}

private fun parseSlotTime(startsAt: String): LocalTime? {
    return try {
        Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).toLocalTime()
    } catch (_: Exception) {
        null
    }
}

private fun String.toDatePickerMillis(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDay(CLINIC_TIME_ZONE).toInstant().toEpochMilli()
}.getOrNull()

/**
 * Placeholder 30-min slots from 9:00 AM to 5:00 PM for UI preview.
 * Remove when the availability API is connected.
 */
private fun placeholderSlots(): List<AvailabilitySlot> {
    val baseDate = LocalDate.now().plusDays(1).toString()
    val slots = mutableListOf<AvailabilitySlot>()
    for (hour in 9..16) {
        for (minute in listOf(0, 30)) {
            val start = java.time.LocalTime.of(hour, minute)
            val end = start.plusMinutes(30)
            val startsAt = "${baseDate}T${start}:00+08:00"
            val endsAt = "${baseDate}T${end}:00+08:00"
            slots.add(
                AvailabilitySlot(
                    startsAt = startsAt,
                    endsAt = endsAt,
                    available = true,
                    reason = null,
                ),
            )
        }
    }
    return slots
}
