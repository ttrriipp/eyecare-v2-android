package com.eyecare.app.presentation.appointments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.requests.formatRequestDate
import com.eyecare.app.presentation.appointments.requests.formatRequestWeekday
import com.eyecare.app.presentation.appointments.requests.formatSlotDuration
import com.eyecare.app.presentation.appointments.requests.formatTimeRange
import com.eyecare.app.presentation.appointments.requests.parseSlotTime
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.ui.theme.EyecareColors
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

private val rescheduleDateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US)
private val rescheduleTimeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val rescheduleWeekdayInitialFormat = DateTimeFormatter.ofPattern("EEEEE", Locale.US)
private val rescheduleFullMonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
private val rescheduleShortMonthFormat = DateTimeFormatter.ofPattern("MMM", Locale.US)
private const val RESCHEDULE_NOTE_MAX_LENGTH = 1000
private const val MAX_ALTERNATIVE_TIMES = 2
private const val OTHER_RESCHEDULE_REASON = "Other"
private val RESCHEDULE_REASON_PRESETS = listOf(
    "My schedule changed",
    "I have another commitment",
    "The current time no longer works",
)

private enum class RescheduleSelectionPhase { PREFERRED, ALTERNATIVES }

/**
 * Date and time in one continuous view — a week strip above a morning/afternoon slot list —
 * matching the schedule step of the appointment-request flow, rather than a tabbed
 * calendar-then-list pattern. There is no separate step for a visit reason here, so nothing is
 * lost by collapsing date and time onto one screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleBottomSheet(
    currentScheduledAt: String,
    weekStart: String?,
    dayAvailability: Map<String, DayAvailability>,
    availabilityState: RescheduleAvailabilityState,
    isSubmitting: Boolean,
    errorMessage: String?,
    title: String = "Reschedule appointment",
    description: String =
        "Choose a date from tomorrow onward and an available time.",
    currentTimeLabel: String = "Current appointment",
    currentTimeDescription: String =
        "Stays confirmed until approved.",
    confirmationTitle: String = "Request this time change",
    confirmationMessage: (date: String, time: String, alternatives: List<String>) -> String = { _, _, _ ->
        "The clinic must approve this request before confirming."
    },
    confirmLabel: String = "Send request",
    dismissLabel: String = "Keep current time",
    showReasonField: Boolean = false,
    onShowWeek: (String) -> Unit,
    onDateChanged: (String) -> Unit,
    onRetryAvailability: () -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (
        scheduledAt: String,
        alternativeScheduledTimes: List<String>,
        reasonForVisit: String?,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val currentDate = remember(currentScheduledAt) {
        val requestedDate = parseClinicDateTime(currentScheduledAt)?.toLocalDate()
            ?: runCatching { LocalDate.parse(currentScheduledAt.take(10)) }.getOrNull()
        maxOf(requestedDate ?: earliestAppointmentRequestDate(), earliestAppointmentRequestDate())
            .toString()
    }
    var selectedDate by remember(currentDate) { mutableStateOf(currentDate) }
    var selectionPhaseName by rememberSaveable { mutableStateOf(RescheduleSelectionPhase.PREFERRED.name) }
    val selectionPhase = RescheduleSelectionPhase.valueOf(selectionPhaseName)
    val addingAlternatives = selectionPhase == RescheduleSelectionPhase.ALTERNATIVES
    var selectedPrimarySlotStartsAt by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAlternativeSlotStartsAt by rememberSaveable {
        mutableStateOf<List<String>>(emptyList())
    }
    var reasonForVisit by rememberSaveable { mutableStateOf("") }
    var selectedReason by rememberSaveable { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val availability = (availabilityState as? RescheduleAvailabilityState.Success)?.availability
    val availabilityForSelectedDate = availability?.takeIf { it.date == selectedDate }
    val availableSlots = availabilityForSelectedDate?.slots?.filter { it.available }.orEmpty()
    val isCurrentSlot = selectedPrimarySlotStartsAt?.let { sameInstant(it, currentScheduledAt) } == true
    val validationMessage = if (isCurrentSlot) {
        "That is already your current time. Choose another slot."
    } else {
        null
    }
    val reasonValidationMessage = if (
        showReasonField &&
        selectedPrimarySlotStartsAt != null &&
        !isCurrentSlot &&
        reasonForVisit.isBlank()
    ) {
        "Choose a reason to continue."
    } else {
        null
    }
    val selectedSlotIsAvailable = selectedPrimarySlotStartsAt?.let { selected ->
        availabilityState is RescheduleAvailabilityState.Success &&
            availableSlots.any { sameInstant(it.startsAt, selected) }
    } == true
    val selectionValidationMessage = when {
        selectedPrimarySlotStartsAt == null -> "Choose an available time to continue."
        availabilityState is RescheduleAvailabilityState.Success && !selectedSlotIsAvailable ->
            "That time is no longer available. Choose another slot."
        else -> null
    }
    val bottomError = errorMessage ?: validationMessage
    val bottomGuidance = selectionValidationMessage ?: reasonValidationMessage
    val bottomNotice = bottomError ?: bottomGuidance
    val canConfirm = selectedSlotIsAvailable && !isCurrentSlot && !isSubmitting
    var actionBarHeightPx by remember { mutableIntStateOf(0) }
    val footerClearance = with(LocalDensity.current) {
        if (actionBarHeightPx == 0) {
            if (bottomNotice == null) 140.dp else 220.dp
        } else {
            actionBarHeightPx.toDp() + 24.dp
        }
    }

    fun clearSlotSelectionsForDateChange() {
        selectedPrimarySlotStartsAt = null
        selectedAlternativeSlotStartsAt = emptyList()
        selectionPhaseName = RescheduleSelectionPhase.PREFERRED.name
    }

    fun selectPreferredSlot(startsAt: String) {
        selectedPrimarySlotStartsAt = startsAt
        selectedAlternativeSlotStartsAt = selectedAlternativeSlotStartsAt - startsAt
    }

    fun toggleAlternativeSlot(startsAt: String) {
        if (startsAt == selectedPrimarySlotStartsAt) return
        selectedAlternativeSlotStartsAt = if (startsAt in selectedAlternativeSlotStartsAt) {
            selectedAlternativeSlotStartsAt - startsAt
        } else if (selectedAlternativeSlotStartsAt.size < MAX_ALTERNATIVE_TIMES) {
            selectedAlternativeSlotStartsAt + startsAt
        } else {
            selectedAlternativeSlotStartsAt
        }
    }

    fun selectSlot(startsAt: String) {
        if (addingAlternatives) toggleAlternativeSlot(startsAt) else selectPreferredSlot(startsAt)
    }

    fun startAddingAlternatives() {
        if (selectedPrimarySlotStartsAt != null) {
            selectionPhaseName = RescheduleSelectionPhase.ALTERNATIVES.name
        }
    }

    fun finishAddingAlternatives() {
        selectionPhaseName = RescheduleSelectionPhase.PREFERRED.name
    }

    val selectedPrimarySlot = selectedPrimarySlotStartsAt
    if (showConfirmDialog && selectedPrimarySlot != null) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventAvailable,
            title = confirmationTitle,
            message = confirmationMessage(
                formatRescheduleDate(selectedPrimarySlot),
                formatRescheduleTime(selectedPrimarySlot),
                selectedAlternativeSlotStartsAt,
            ),
            confirmLabel = confirmLabel,
            dismissLabel = dismissLabel,
            onConfirm = {
                showConfirmDialog = false
                onConfirm(
                    selectedPrimarySlot,
                    selectedAlternativeSlotStartsAt,
                    reasonForVisit.trim().takeIf { it.isNotEmpty() },
                )
            },
            onDismissRequest = { showConfirmDialog = false },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    // Leave room for the measured pinned action bar and its current notice.
                    .padding(bottom = footerClearance),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.semantics {
                            contentDescription = dismissLabel
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                        )
                    }
                }

                RescheduleCurrentTimeCard(
                    label = currentTimeLabel,
                    scheduledAt = currentScheduledAt,
                    description = currentTimeDescription,
                )

                RescheduleLimitNotice()

                Text(
                    text = if (addingAlternatives) {
                        "Choose up to two alternative times."
                    } else {
                        "Choose a preferred time. Alternatives are optional."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RescheduleWeekStrip(
                    weekStart = weekStart,
                    selectedDate = selectedDate,
                    dayAvailability = dayAvailability,
                    onShowWeek = onShowWeek,
                    onDateSelected = { date ->
                        if (date != selectedDate) {
                            clearSlotSelectionsForDateChange()
                            selectedDate = date
                            onDateChanged(date)
                        }
                    },
                )

                selectedPrimarySlot?.let { preferredTime ->
                    SelectedRescheduleTimesCard(
                        preferredTime = preferredTime,
                        alternativeTimes = selectedAlternativeSlotStartsAt,
                        addingAlternatives = addingAlternatives,
                        onAddAlternatives = ::startAddingAlternatives,
                        onChangePreferred = {
                            selectionPhaseName = RescheduleSelectionPhase.PREFERRED.name
                        },
                        onRemoveAlternative = { startsAt ->
                            selectedAlternativeSlotStartsAt = selectedAlternativeSlotStartsAt - startsAt
                        },
                    )
                }

                if (showReasonField) {
                    RescheduleReasonPicker(
                        selectedReason = selectedReason,
                        reason = reasonForVisit,
                        enabled = !isSubmitting,
                        onReasonSelected = { choice ->
                            selectedReason = choice
                            reasonForVisit = if (choice == OTHER_RESCHEDULE_REASON) "" else choice
                        },
                        onReasonChanged = { value ->
                            reasonForVisit = value.take(RESCHEDULE_NOTE_MAX_LENGTH)
                        },
                    )
                }

                RescheduleSlotSection(
                    availabilityState = availabilityState,
                    availableSlots = availableSlots,
                    selectionPhase = selectionPhase,
                    selectedPrimarySlotStartsAt = selectedPrimarySlotStartsAt,
                    selectedAlternativeSlotStartsAt = selectedAlternativeSlotStartsAt,
                    isSubmitting = isSubmitting,
                    onSelectSlot = ::selectSlot,
                    onRetryAvailability = onRetryAvailability,
                )
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { actionBarHeightPx = it.height }
                    .imePadding(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (bottomNotice != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    liveRegion = LiveRegionMode.Polite
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (bottomError != null) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                            },
                        ) {
                            Text(
                                text = bottomNotice,
                                modifier = Modifier.padding(12.dp),
                                color = if (bottomError != null) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (addingAlternatives) {
                        AppointmentPrimaryButton(
                            text = when (selectedAlternativeSlotStartsAt.size) {
                                0 -> "Done"
                                1 -> "Done · 1 alternative"
                                else -> "Done · ${selectedAlternativeSlotStartsAt.size} alternatives"
                            },
                            onClick = ::finishAddingAlternatives,
                            enabled = !isSubmitting,
                            loading = isSubmitting,
                        )
                    } else {
                        AppointmentPrimaryButton(
                            text = "Review reschedule",
                            onClick = { showConfirmDialog = true },
                            enabled = canConfirm &&
                                (!showReasonField || reasonForVisit.isNotBlank()),
                            loading = isSubmitting,
                            modifier = Modifier.semantics {
                                if (isSubmitting) {
                                    contentDescription = "Submitting reschedule request"
                                    liveRegion = LiveRegionMode.Polite
                                } else if (!canConfirm) {
                                    stateDescription = bottomNotice ?: "Choose an available time to continue."
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleLimitNotice(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EyecareColors.current.statusPending.copy(alpha = 0.16f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = EyecareColors.current.statusPendingText,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "One-time reschedule",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.statusPendingText,
                )
                Text(
                    text = "This appointment can only be rescheduled once. Choose your new time carefully.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EyecareColors.current.statusPendingText,
                )
            }
        }
    }
}

@Composable
private fun RescheduleCurrentTimeCard(
    label: String,
    scheduledAt: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${formatRescheduleDate(scheduledAt)} · ${formatRescheduleTime(scheduledAt)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RescheduleReasonPicker(
    selectedReason: String?,
    reason: String,
    enabled: Boolean,
    onReasonSelected: (String) -> Unit,
    onReasonChanged: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Why are you rescheduling?",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Required for clinic review.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { selectableGroup() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RESCHEDULE_REASON_PRESETS.forEach { preset ->
                FilterChip(
                    selected = selectedReason == preset,
                    onClick = { onReasonSelected(preset) },
                    enabled = enabled,
                    label = { Text(preset) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = rescheduleReasonChipColors(),
                )
            }
            FilterChip(
                selected = selectedReason == OTHER_RESCHEDULE_REASON,
                onClick = { onReasonSelected(OTHER_RESCHEDULE_REASON) },
                enabled = enabled,
                label = { Text(OTHER_RESCHEDULE_REASON) },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = rescheduleReasonChipColors(),
            )
        }

        if (selectedReason == OTHER_RESCHEDULE_REASON) {
            OutlinedTextField(
                value = reason,
                onValueChange = onReasonChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = enabled,
                label = { Text("Reason for rescheduling") },
                placeholder = { Text("Tell the clinic why you need a different time") },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text("${reason.length}/$RESCHEDULE_NOTE_MAX_LENGTH")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
        }
    }
}

@Composable
private fun rescheduleReasonChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = EyecareColors.current.accentText,
    selectedLeadingIconColor = EyecareColors.current.accentText,
)

@Composable
private fun SelectedRescheduleTimesCard(
    preferredTime: String,
    alternativeTimes: List<String>,
    addingAlternatives: Boolean,
    onAddAlternatives: () -> Unit,
    onChangePreferred: () -> Unit,
    onRemoveAlternative: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Your selected times",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${alternativeTimes.size} of $MAX_ALTERNATIVE_TIMES alternatives",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            SelectedRescheduleTimeRow(
                rank = "Preferred",
                startsAt = preferredTime,
            )
            alternativeTimes.forEachIndexed { index, startsAt ->
                SelectedRescheduleTimeRow(
                    rank = "Alternative ${index + 1}",
                    startsAt = startsAt,
                    onRemove = { onRemoveAlternative(startsAt) },
                )
            }

            Text(
                text = if (addingAlternatives) {
                    "Choose up to two alternative times."
                } else {
                    "Preferred time is sent first."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )

            if (addingAlternatives) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onChangePreferred) {
                        Text("Change preferred time")
                    }
                }
            } else {
                TextButton(
                    onClick = onAddAlternatives,
                    enabled = alternativeTimes.size < MAX_ALTERNATIVE_TIMES,
                ) {
                    Text(
                        text = if (alternativeTimes.isEmpty()) {
                            "Add alternative times"
                        } else {
                            "Change alternative times"
                        },
                        color = EyecareColors.current.accentText,
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedRescheduleTimeRow(
    rank: String,
    startsAt: String,
    onRemove: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (rank == "Preferred") {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ) {
            Text(
                text = rank,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clearAndSetSemantics { },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (rank == "Preferred") {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Text(
            text = "${formatRescheduleDate(startsAt)} · ${formatRescheduleTime(startsAt)}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        onRemove?.let {
            IconButton(onClick = it) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove $rank",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } ?: Spacer(Modifier.size(48.dp))
    }
}

// ------------------------------------------------------------------ week strip

/**
 * Seven days with their availability already resolved, so a closed or fully booked day is
 * visible before it costs a tap. Ported from the appointment-request flow's schedule step so
 * both scheduling surfaces read as one design rather than two.
 */
@Composable
private fun RescheduleWeekStrip(
    weekStart: String?,
    selectedDate: String,
    dayAvailability: Map<String, DayAvailability>,
    onShowWeek: (String) -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val earliestDate = remember { earliestAppointmentRequestDate() }
    val minimumWeekStart = remember {
        earliestDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
    val start = weekStart
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?.takeUnless { it.isBefore(minimumWeekStart) }
        ?: minimumWeekStart
    val canGoBack = start.isAfter(minimumWeekStart)

    fun showWeekAndSelectDate(nextStart: LocalDate) {
        val nextDate = nextStart.toString()
        onShowWeek(nextDate)
        onDateSelected(nextDate)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    showWeekAndSelectDate(start.minusDays(availabilityWeekLength.toLong()))
                },
                enabled = canGoBack,
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous week")
            }
            Text(
                text = rescheduleMonthRangeLabel(start),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    showWeekAndSelectDate(start.plusDays(availabilityWeekLength.toLong()))
                },
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            // Keep every day at least 48dp wide. On compact screens the strip scrolls instead of
            // shrinking seven controls below the platform touch-target minimum.
            val cellWidth = maxOf(48.dp, (maxWidth - 12.dp) / availabilityWeekLength)
            AnimatedContent(
                targetState = start,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -direction * width } + fadeOut())
                },
                label = "reschedule-week-strip",
            ) { visibleStart ->
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .semantics { selectableGroup() },
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    (0 until availabilityWeekLength).forEach { offset ->
                        val date = visibleStart.plusDays(offset.toLong())
                        RescheduleDayCell(
                            date = date,
                            isBeforeEarliestDate = date.isBefore(earliestDate),
                            isSelected = date.toString() == selectedDate,
                            verdict = dayAvailability[date.toString()] ?: DayAvailability.UNKNOWN,
                            onClick = { onDateSelected(date.toString()) },
                            modifier = Modifier.width(cellWidth),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RescheduleAvailabilityLegendItem(
                color = EyecareColors.current.statusConfirmed,
                label = "Available",
            )
            RescheduleAvailabilityLegendItem(
                color = MaterialTheme.colorScheme.outlineVariant,
                label = "Unavailable",
            )
        }

        val verdict = dayAvailability[selectedDate] ?: DayAvailability.UNKNOWN
        val caption = when (verdict) {
            DayAvailability.OPEN -> "${formatRequestWeekday(selectedDate)}, ${formatRequestDate(selectedDate)}"
            DayAvailability.FULL ->
                "${formatRequestWeekday(selectedDate)} is fully booked. Try another day."
            DayAvailability.CLOSED ->
                "The clinic is closed on ${formatRequestWeekday(selectedDate)}. Try another day."
            DayAvailability.LOADING, DayAvailability.UNKNOWN ->
                "${formatRequestWeekday(selectedDate)}, ${formatRequestDate(selectedDate)}"
        }
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun RescheduleAvailabilityLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RescheduleDayCell(
    date: LocalDate,
    isBeforeEarliestDate: Boolean,
    isSelected: Boolean,
    verdict: DayAvailability,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = isBeforeEarliestDate ||
        verdict == DayAvailability.CLOSED ||
        verdict == DayAvailability.FULL
    val status = when {
        isBeforeEarliestDate -> "Unavailable for requests"
        verdict == DayAvailability.CLOSED -> "Closed"
        verdict == DayAvailability.FULL -> "Fully booked"
        verdict == DayAvailability.OPEN -> "Times available"
        else -> "Checking availability"
    }

    val container = when {
        isSelected -> MaterialTheme.colorScheme.primary
        unavailable -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val content = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        unavailable -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        modifier = modifier
            .widthIn(min = 48.dp)
            .sizeIn(minHeight = 60.dp)
            .selectable(
                selected = isSelected,
                enabled = !unavailable,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics {
                contentDescription = "${date.format(DateTimeFormatter.ofPattern("EEEE d MMMM"))}, $status"
                stateDescription = if (isSelected) "Selected" else "Not selected"
            },
        shape = RoundedCornerShape(12.dp),
        color = container,
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = if (isSelected || unavailable) 0.dp else 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = date.format(rescheduleWeekdayInitialFormat),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = content,
                maxLines = 1,
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = content,
                maxLines = 1,
            )
            RescheduleDayMarker(
                verdict = if (isBeforeEarliestDate) DayAvailability.CLOSED else verdict,
                isSelected = isSelected,
            )
        }
    }
}

@Composable
private fun RescheduleDayMarker(verdict: DayAvailability, isSelected: Boolean) {
    val dotColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        verdict == DayAvailability.OPEN -> EyecareColors.current.statusConfirmed
        else -> null
    }
    Box(modifier = Modifier.size(8.dp), contentAlignment = Alignment.Center) {
        if (dotColor != null) {
            Box(modifier = Modifier.size(6.dp).background(dotColor, CircleShape))
        }
    }
}

private fun rescheduleMonthRangeLabel(start: LocalDate): String {
    val end = start.plusDays((availabilityWeekLength - 1).toLong())
    return if (start.month == end.month) {
        start.format(rescheduleFullMonthFormat)
    } else {
        "${start.format(rescheduleShortMonthFormat)} – ${end.format(rescheduleShortMonthFormat)} ${end.year}"
    }
}

// -------------------------------------------------------------------- slot list

@Composable
private fun RescheduleSlotSection(
    availabilityState: RescheduleAvailabilityState,
    availableSlots: List<AppointmentSlot>,
    selectionPhase: RescheduleSelectionPhase,
    selectedPrimarySlotStartsAt: String?,
    selectedAlternativeSlotStartsAt: List<String>,
    isSubmitting: Boolean,
    onSelectSlot: (String) -> Unit,
    onRetryAvailability: () -> Unit,
) {
    when (availabilityState) {
        RescheduleAvailabilityState.Idle -> {
            Text(
                text = "Select a date above to see the times the clinic has open.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is RescheduleAvailabilityState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Checking available times"
                        liveRegion = LiveRegionMode.Polite
                    },
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
            RescheduleAvailabilityError(message = availabilityState.message, onRetry = onRetryAvailability)
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
                val morning = availableSlots.filter { (parseSlotTime(it.startsAt)?.hour ?: 0) < 12 }
                val afternoon = availableSlots.filter { (parseSlotTime(it.startsAt)?.hour ?: 0) >= 12 }
                val alternativesFull = selectedAlternativeSlotStartsAt.size >= MAX_ALTERNATIVE_TIMES

                fun labelFor(slot: AppointmentSlot): String? {
                    return when {
                        slot.startsAt == selectedPrimarySlotStartsAt -> "Preferred"
                        selectionPhase == RescheduleSelectionPhase.ALTERNATIVES &&
                            slot.startsAt in selectedAlternativeSlotStartsAt ->
                            "Alternative ${selectedAlternativeSlotStartsAt.indexOf(slot.startsAt) + 1}"
                        else -> null
                    }
                }

                fun enabledFor(slot: AppointmentSlot): Boolean {
                    if (isSubmitting) return false
                    if (selectionPhase == RescheduleSelectionPhase.PREFERRED) return true
                    if (slot.startsAt == selectedPrimarySlotStartsAt) return false
                    return slot.startsAt in selectedAlternativeSlotStartsAt || !alternativesFull
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (morning.isNotEmpty()) {
                        RescheduleTimePeriodHeader(Icons.Outlined.WbSunny, "Morning", morning.size)
                        morning.forEach { slot ->
                            RescheduleSlotRow(
                                slot = slot,
                                selectionPhase = selectionPhase,
                                selected = slot.startsAt == selectedPrimarySlotStartsAt ||
                                    (selectionPhase == RescheduleSelectionPhase.ALTERNATIVES &&
                                        slot.startsAt in selectedAlternativeSlotStartsAt),
                                selectionLabel = labelFor(slot),
                                enabled = enabledFor(slot),
                                onSelect = { onSelectSlot(slot.startsAt) },
                            )
                        }
                    }
                    if (afternoon.isNotEmpty()) {
                        RescheduleTimePeriodHeader(Icons.Outlined.Schedule, "Afternoon", afternoon.size)
                        afternoon.forEach { slot ->
                            RescheduleSlotRow(
                                slot = slot,
                                selectionPhase = selectionPhase,
                                selected = slot.startsAt == selectedPrimarySlotStartsAt ||
                                    (selectionPhase == RescheduleSelectionPhase.ALTERNATIVES &&
                                        slot.startsAt in selectedAlternativeSlotStartsAt),
                                selectionLabel = labelFor(slot),
                                enabled = enabledFor(slot),
                                onSelect = { onSelectSlot(slot.startsAt) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RescheduleTimePeriodHeader(icon: ImageVector, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
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
private fun RescheduleAvailabilityError(message: String, onRetry: (() -> Unit)?) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Unable to load available times: $message"
                liveRegion = LiveRegionMode.Polite
            },
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
    selectionPhase: RescheduleSelectionPhase,
    selected: Boolean,
    selectionLabel: String?,
    enabled: Boolean,
    onSelect: () -> Unit,
) {
    val role = if (selectionPhase == RescheduleSelectionPhase.PREFERRED) {
        Role.RadioButton
    } else {
        Role.Checkbox
    }
    val timeLabel = formatTimeRange(slot.startsAt, slot.endsAt)
    val selectionState = when {
        selectionLabel != null -> "$selectionLabel selected"
        selectionPhase == RescheduleSelectionPhase.ALTERNATIVES && !enabled ->
            "Unavailable. Two alternatives are already selected."
        else -> "Not selected"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                enabled = enabled,
                role = role,
                onClick = onSelect,
            )
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(timeLabel)
                    selectionLabel?.let { append(", $it") }
                    slot.reason?.takeIf { it.isNotBlank() }?.let { append(", $it") }
                }
                stateDescription = selectionState
            },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (selectionPhase == RescheduleSelectionPhase.PREFERRED) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    enabled = enabled,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            } else {
                Checkbox(
                    checked = selected,
                    onCheckedChange = null,
                    enabled = enabled,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = formatSlotDuration(slot.startsAt, slot.endsAt),
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
            selectionLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = EyecareColors.current.accentText,
                    modifier = Modifier.clearAndSetSemantics { },
                )
            }
        }
    }
}

private fun formatRescheduleDate(startsAt: String): String =
    parseClinicDateTime(startsAt)?.format(rescheduleDateFormatter)
        ?: runCatching { LocalDate.parse(startsAt.take(10)).format(rescheduleDateFormatter) }
            .getOrDefault(startsAt)

private fun formatRescheduleTime(startsAt: String): String =
    parseClinicDateTime(startsAt)?.format(rescheduleTimeFormatter)
        ?: startsAt

private fun sameInstant(first: String, second: String): Boolean = runCatching {
    Instant.parse(first) == Instant.parse(second)
}.getOrDefault(first == second)
