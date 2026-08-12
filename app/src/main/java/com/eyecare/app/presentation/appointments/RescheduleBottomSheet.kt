package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.ui.theme.EyecareColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val rescheduleDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val rescheduleTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleBottomSheet(
    currentScheduledAt: String,
    availabilityState: RescheduleAvailabilityState,
    isSubmitting: Boolean,
    errorMessage: String?,
    onDateChanged: (String) -> Unit,
    onRetryAvailability: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (scheduledAt: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentDate = remember(currentScheduledAt) {
        parseClinicDateTime(currentScheduledAt)
            ?.toLocalDate()
            ?.toString()
            ?: currentScheduledAt.take(10)
    }
    var selectedDate by remember(currentDate) { mutableStateOf(currentDate) }
    var selectedSlotStartsAt by remember { mutableStateOf<String?>(null) }
    var tabIndex by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val availability = (availabilityState as? RescheduleAvailabilityState.Success)?.availability
    val availableSlots = availability?.slots?.filter { it.available }.orEmpty()
    val selectedSlot = availableSlots.firstOrNull { it.startsAt == selectedSlotStartsAt }
    val isCurrentSlot = selectedSlot?.let { sameInstant(it.startsAt, currentScheduledAt) } == true
    val canConfirm = selectedSlot != null && !isCurrentSlot && !isSubmitting

    LaunchedEffect(availabilityState) {
        selectedSlotStartsAt = null
    }

    if (showConfirmDialog && selectedSlot != null) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventAvailable,
            title = "Confirm reschedule",
            message = "Move this appointment to ${formatRescheduleDate(selectedSlot.startsAt)} at " +
                "${formatRescheduleTime(selectedSlot.startsAt)}?",
            confirmLabel = "Reschedule appointment",
            dismissLabel = "Keep current time",
            onConfirm = {
                showConfirmDialog = false
                onConfirm(selectedSlot.startsAt)
            },
            onDismissRequest = { showConfirmDialog = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Reschedule appointment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Choose a date, then select a time the clinic has confirmed as available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SecondaryTabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Date") },
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Available times") },
                )
            }

            if (tabIndex == 0) {
                RescheduleDatePicker(
                    selectedDate = selectedDate,
                    onSelectDate = { date ->
                        if (date != selectedDate) {
                            selectedDate = date
                            onDateChanged(date)
                        }
                    },
                )
                RescheduleAvailabilitySummary(availabilityState, onRetryAvailability)
            } else {
                RescheduleSlotPicker(
                    availabilityState = availabilityState,
                    availableSlots = availableSlots,
                    selectedSlotStartsAt = selectedSlotStartsAt,
                    isSubmitting = isSubmitting,
                    onSelectSlot = { selectedSlotStartsAt = it },
                    onRetryAvailability = onRetryAvailability,
                )
            }

            if (errorMessage != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            liveRegion = LiveRegionMode.Polite
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            if (isCurrentSlot) {
                Text(
                    text = "That is already your current appointment time. Choose another slot.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = {
                    if (tabIndex == 0) {
                        tabIndex = 1
                    } else if (canConfirm) {
                        showConfirmDialog = true
                    }
                },
                enabled = !isSubmitting && (tabIndex == 0 || canConfirm),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(if (tabIndex == 0) "Continue to available times" else "Review reschedule")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RescheduleDatePicker(
    selectedDate: String,
    onSelectDate: (String) -> Unit,
) {
    val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.toPickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(ZoneOffset.UTC)
                    .toLocalDate()
                return !date.isBefore(today) && date.dayOfWeek != DayOfWeek.SUNDAY
            }
        },
    )

    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString() }
            ?.let(onSelectDate)
    }

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
            currentYearContentColor = EyecareColors.current.accentText,
            selectedYearContainerColor = MaterialTheme.colorScheme.primary,
            selectedYearContentColor = MaterialTheme.colorScheme.onPrimary,
            dayContentColor = MaterialTheme.colorScheme.onSurface,
            selectedDayContainerColor = MaterialTheme.colorScheme.primary,
            selectedDayContentColor = MaterialTheme.colorScheme.onPrimary,
            todayContentColor = EyecareColors.current.accentText,
            todayDateBorderColor = EyecareColors.current.accentText,
            disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
        ),
    )
}

@Composable
private fun RescheduleAvailabilitySummary(state: RescheduleAvailabilityState, onRetryAvailability: () -> Unit) {
    when (state) {
        RescheduleAvailabilityState.Idle -> {
            Text(
                text = "Select a date to check clinic availability.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is RescheduleAvailabilityState.Loading -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text(
                    text = "Checking available times...",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        is RescheduleAvailabilityState.Error -> {
            RescheduleAvailabilityError(
                message = state.message,
                onRetry = onRetryAvailability,
            )
        }

        is RescheduleAvailabilityState.Success -> {
            val count = state.availability.slots.count { it.available }
            Text(
                text = if (count == 0) {
                    "No times are available on this date. Try another date."
                } else {
                    "$count available time${if (count == 1) "" else "s"}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (count == 0) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    EyecareColors.current.accentText
                },
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RescheduleSlotPicker(
    availabilityState: RescheduleAvailabilityState,
    availableSlots: List<AppointmentSlot>,
    selectedSlotStartsAt: String?,
    isSubmitting: Boolean,
    onSelectSlot: (String) -> Unit,
    onRetryAvailability: () -> Unit,
) {
    when (availabilityState) {
        RescheduleAvailabilityState.Idle -> {
            Text(
                text = "Select a date first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is RescheduleAvailabilityState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator()
                    Text("Checking available times...")
                }
            }
        }

        is RescheduleAvailabilityState.Error -> {
            RescheduleAvailabilityError(
                message = availabilityState.message,
                onRetry = onRetryAvailability,
            )
        }

        is RescheduleAvailabilityState.Success -> {
            if (availableSlots.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.EventBusy,
                            contentDescription = null,
                            tint = EyecareColors.current.accentText,
                        )
                        Text(
                            text = "No appointment times are available on this date. Try another date.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                Text(
                    text = "Available times",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    availableSlots.forEach { slot ->
                        val isSelected = slot.startsAt == selectedSlotStartsAt
                        RescheduleSlotRow(
                            slot = slot,
                            selected = isSelected,
                            enabled = !isSubmitting,
                            onSelect = { onSelectSlot(slot.startsAt) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleAvailabilityError(
    message: String,
    onRetry: (() -> Unit)?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
            )
            onRetry?.let {
                TextButton(onClick = it) {
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
private fun RescheduleSlotRow(
    slot: AppointmentSlot,
    selected: Boolean,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val label = "${formatRescheduleDate(slot.startsAt)} at ${formatRescheduleTime(slot.startsAt)}"
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onSelect)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = label
            },
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) {
                EyecareColors.current.accentText
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                enabled = enabled,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatRescheduleTime(slot.startsAt),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${formatRescheduleDate(slot.startsAt)} - ${formatRescheduleTime(slot.endsAt)}" +
                        " (${formatSlotDuration(slot)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                slot.reason?.takeIf { it.isNotBlank() }?.let { reason ->
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun formatRescheduleDate(startsAt: String): String = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).format(rescheduleDateFormatter)
}.getOrDefault(startsAt)

private fun formatRescheduleTime(startsAt: String): String = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).format(rescheduleTimeFormatter)
}.getOrDefault(startsAt)

private fun formatSlotDuration(slot: AppointmentSlot): String = runCatching {
    val minutes = java.time.Duration.between(
        Instant.parse(slot.startsAt),
        Instant.parse(slot.endsAt),
    ).toMinutes()
    "$minutes min"
}.getOrDefault("visit")

private fun String.toPickerMillis(): Long? = runCatching {
    LocalDate.parse(this).toEpochDay() * 86_400_000L
}.getOrNull()

private fun sameInstant(first: String, second: String): Boolean = runCatching {
    Instant.parse(first) == Instant.parse(second)
}.getOrDefault(first == second)
