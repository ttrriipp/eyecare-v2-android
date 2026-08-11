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
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.WizardStepIndicator
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val wizardSteps = listOf("Type", "Schedule", "Details", "Review")
private const val maxAlternativeSlots = 2

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
        is RequestStep.Type -> TypePlaceholderContent(
            state = s,
            onSelectType = { viewModel.selectType(it) },
            onConfirm = { viewModel.confirmType() },
            onRetry = { viewModel.retryTypes() },
            onBack = onBack,
        )
        is RequestStep.Schedule -> ScheduleContent(
            state = s,
            onDateSelected = { viewModel.selectDate(it) },
            onSelectSlot = { viewModel.selectPrimarySlot(it) },
            onAddAlternative = { viewModel.addAlternative(it) },
            onRemoveAlternative = { viewModel.removeAlternative(it) },
            onRetry = { viewModel.retryAvailability() },
            onConfirm = {
                viewModel.confirmSchedule(
                    identityDetailsRequired = identityDetailsRequired,
                    initialIdentity = requestIdentity,
                )
            },
            onBack = onBack,
        )
        is RequestStep.Details -> ProfileAndReasonContent(
            state = s,
            onReasonChange = { viewModel.updateReason(it) },
            onReferringSourceChange = { viewModel.updateReferringSource(it) },
            onEmailChange = { viewModel.updateIdentity(email = it) },
            onFirstNameChange = { viewModel.updateIdentity(firstName = it) },
            onMiddleNameChange = { viewModel.updateIdentity(middleName = it) },
            onLastNameChange = { viewModel.updateIdentity(lastName = it) },
            onDateOfBirthChange = { viewModel.updateIdentity(dateOfBirth = it) },
            onGenderChange = { viewModel.updateIdentity(gender = it) },
            onOccupationChange = { viewModel.updateIdentity(occupation = it) },
            onAddressChange = { viewModel.updateIdentity(address = it) },
            onConfirm = { viewModel.confirmDetails() },
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
internal fun ScheduleContent(
    state: RequestStep.Schedule,
    onDateSelected: (String) -> Unit,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onAddAlternative: (AvailabilitySlot) -> Unit,
    onRemoveAlternative: (AvailabilitySlot) -> Unit,
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
                return !date.isBefore(today)
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
                currentStep = 1,
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
                    text = "Choose your preferred time, then add up to two alternatives.",
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

                state.primarySlot?.let { primarySlot ->
                    SelectedPreferences(
                        primarySlot = primarySlot,
                        alternativeSlots = state.alternativeSlots,
                        onRemoveAlternative = onRemoveAlternative,
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
                            state.availabilityError != null -> {
                                ErrorContent(
                                    message = state.availabilityError,
                                    onRetry = onRetry,
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                )
                            }
                            else -> {
                                val availableSlots = state.availability?.slots?.filter { it.available } ?: emptyList()
                                val dayStatus = state.availability?.dayStatus?.lowercase()
                                when {
                                    dayStatus != null && dayStatus != "open" -> {
                                        EmptyContent(
                                            message = availabilityUnavailableMessage(dayStatus),
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                        )
                                    }
                                    availableSlots.isEmpty() -> {
                                        EmptyContent(
                                            message = "No appointment times are available on this date.",
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                        )
                                    }
                                    else -> {
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
                                                    val alternativeOrder = state.alternativeSlots
                                                        .indexOfFirst { it.startsAt == slot.startsAt }
                                                        .takeIf { it >= 0 }
                                                        ?.plus(1)
                                                    SlotCard(
                                                        slot = slot,
                                                        isSelected = slot == state.primarySlot,
                                                        alternativeOrder = alternativeOrder,
                                                        canAddAlternative = state.primarySlot != null &&
                                                            alternativeOrder == null &&
                                                            state.alternativeSlots.size < maxAlternativeSlots,
                                                        onClick = { onSelectSlot(slot) },
                                                        onAddAlternative = { onAddAlternative(slot) },
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
                                                    val alternativeOrder = state.alternativeSlots
                                                        .indexOfFirst { it.startsAt == slot.startsAt }
                                                        .takeIf { it >= 0 }
                                                        ?.plus(1)
                                                    SlotCard(
                                                        slot = slot,
                                                        isSelected = slot == state.primarySlot,
                                                        alternativeOrder = alternativeOrder,
                                                        canAddAlternative = state.primarySlot != null &&
                                                            alternativeOrder == null &&
                                                            state.alternativeSlots.size < maxAlternativeSlots,
                                                        onClick = { onSelectSlot(slot) },
                                                        onAddAlternative = { onAddAlternative(slot) },
                                                    )
                                                }
                                            }
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
                    enabled = state.primarySlot != null,
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
private fun SelectedPreferences(
    primarySlot: AvailabilitySlot,
    alternativeSlots: List<AvailabilitySlot>,
    onRemoveAlternative: (AvailabilitySlot) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Selected preferences",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${alternativeSlots.size}/$maxAlternativeSlots alternatives",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Alternatives are submitted in the order you add them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PreferenceTimeRow(label = "Preferred time", slot = primarySlot)
            alternativeSlots.forEachIndexed { index, slot ->
                PreferenceTimeRow(
                    label = "Alternative ${index + 1}",
                    slot = slot,
                    onRemove = { onRemoveAlternative(slot) },
                )
            }
        }
    }
}

@Composable
private fun PreferenceTimeRow(
    label: String,
    slot: AvailabilitySlot,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = EyecareColors.current.accentText,
            )
            Text(
                text = formatPreferenceSlot(slot),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        onRemove?.let {
            TextButton(onClick = it) {
                Text("Remove ${label.lowercase()}")
            }
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
    alternativeOrder: Int?,
    canAddAlternative: Boolean,
    onClick: () -> Unit,
    onAddAlternative: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = slot.available, onClick = onClick),
            ) {
                when {
                    isSelected -> Text(
                        text = "Preferred time",
                        style = MaterialTheme.typography.labelMedium,
                        color = EyecareColors.current.accentText,
                    )
                }
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

            when {
                canAddAlternative -> TextButton(onClick = onAddAlternative) {
                    Text("Add as alternative")
                }
                isSelected -> SelectedSlotIndicator(contentDescription = "Preferred time selected")
                alternativeOrder != null -> SelectedSlotIndicator(
                    contentDescription = "Alternative $alternativeOrder selected",
                )
            }
        }
    }
}

@Composable
private fun SelectedSlotIndicator(contentDescription: String) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(MaterialTheme.colorScheme.primary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.EventAvailable,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp),
        )
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

private fun formatPreferenceSlot(slot: AvailabilitySlot): String {
    return try {
        val start = Instant.parse(slot.startsAt).atZone(CLINIC_TIME_ZONE)
        "${start.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))} · " +
            formatTimeRange(slot.startsAt, slot.endsAt)
    } catch (_: Exception) {
        formatTimeRange(slot.startsAt, slot.endsAt)
    }
}

private fun parseSlotTime(startsAt: String): LocalTime? {
    return try {
        Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).toLocalTime()
    } catch (_: Exception) {
        null
    }
}

private fun availabilityUnavailableMessage(dayStatus: String): String {
    return when (dayStatus) {
        "closed", "holiday" -> "The clinic is closed on this date."
        else -> "No appointment times are available on this date."
    }
}

private fun String.toDatePickerMillis(): Long? = runCatching {
    LocalDate.parse(this).atStartOfDay(CLINIC_TIME_ZONE).toInstant().toEpochMilli()
}.getOrNull()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TypePlaceholderContent(
    state: RequestStep.Type,
    onSelectType: (AppointmentType) -> Unit,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request appointment") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WizardStepIndicator(
                currentStep = 0,
                steps = listOf("Type", "Schedule", "Details", "Review"),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            when {
                state.isLoading -> LoadingContent(modifier = Modifier.fillMaxWidth().weight(1f))
                state.error != null -> ErrorContent(
                    message = state.error,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                else -> {
                    Text("Select appointment type", style = MaterialTheme.typography.bodyLarge)
                    state.types.forEach { type ->
                        val isSelected = type == state.selectedType
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onSelectType(type) },
                            shape = RoundedCornerShape(12.dp),
                            border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(type.name, style = MaterialTheme.typography.titleMedium)
                                type.description?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${type.durationMinutes} min", style = MaterialTheme.typography.bodySmall)
                                if (type.requiresReferral) {
                                    Text("Requires referral", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    AppointmentPrimaryButton(
                        text = "Continue",
                        onClick = onConfirm,
                        enabled = state.selectedType != null,
                    )
                }
            }
        }
    }
}
