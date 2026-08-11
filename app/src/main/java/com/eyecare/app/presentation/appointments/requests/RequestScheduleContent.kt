package com.eyecare.app.presentation.appointments.requests

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val weekdayInitialFormat = DateTimeFormatter.ofPattern("EEEEE", Locale.US)

/**
 * The schedule step, in two phases.
 *
 * Phase one collects the single preferred time, so every row in the list means exactly one
 * thing. Only if the patient opts in does the list switch to phase two, where the same rows
 * become checkboxes for up to two numbered backups. Splitting the two is what removes the
 * "which half of this row did I hit" ambiguity that a per-row secondary action creates.
 */
@Composable
internal fun ScheduleContent(
    state: RequestStep.Schedule,
    onShowWeek: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onStartAddingAlternatives: () -> Unit,
    onFinishAddingAlternatives: () -> Unit,
    onToggleAlternative: (AvailabilitySlot) -> Unit,
    onRemoveAlternative: (AvailabilitySlot) -> Unit,
    onRetry: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val addingAlternatives = state.phase == SchedulePhase.ALTERNATIVES
    val alternativesFull = state.alternativeSlots.size >= maxAlternatives

    RequestStepScaffold(
        title = if (addingAlternatives) "Add backup times" else "Choose a time",
        stepLabels = requestStepLabels(state.identityRequired),
        currentStep = requestStepIndex(RequestStepId.SCHEDULE, state.identityRequired),
        onBack = if (addingAlternatives) onFinishAddingAlternatives else onBack,
        bottomBar = {
            if (addingAlternatives) {
                AppointmentPrimaryButton(
                    text = if (state.alternativeSlots.isEmpty()) {
                        "Done"
                    } else {
                        "Done · ${state.alternativeSlots.size} added"
                    },
                    onClick = onFinishAddingAlternatives,
                )
            } else {
                AppointmentPrimaryButton(
                    text = "Continue",
                    onClick = onConfirm,
                    enabled = state.primarySlot != null,
                )
                if (state.primarySlot == null) {
                    Text(
                        text = "Pick a time to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            WeekStrip(
                weekStart = state.weekStart,
                selectedDate = state.date,
                dayAvailability = state.dayAvailability,
                onShowWeek = onShowWeek,
                onDateSelected = onDateSelected,
            )

            if (!addingAlternatives && state.primarySlot != null) {
                PreferencesPanel(
                    primarySlot = state.primarySlot,
                    alternativeSlots = state.alternativeSlots,
                    canAddMore = !alternativesFull,
                    onAddAlternatives = onStartAddingAlternatives,
                    onRemoveAlternative = onRemoveAlternative,
                    modifier = Modifier.padding(horizontal = RequestStepMargin),
                )
            }

            if (addingAlternatives) {
                AlternativesBanner(
                    primarySlot = state.primarySlot,
                    count = state.alternativeSlots.size,
                    modifier = Modifier.padding(horizontal = RequestStepMargin),
                )
            }

            SlotList(
                state = state,
                addingAlternatives = addingAlternatives,
                alternativesFull = alternativesFull,
                onSelectSlot = onSelectSlot,
                onToggleAlternative = onToggleAlternative,
                onRetry = onRetry,
                // Weighted, so the date strip and preferences panel keep their measured height
                // and the list absorbs whatever is left rather than pushing them off-screen.
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
        }
    }
}

// ------------------------------------------------------------------ week strip

/**
 * Seven days with their availability already resolved, so a closed day is visible before it
 * costs a tap. Days the clinic cannot take are disabled rather than merely dimmed, and each
 * cell states its status in words for TalkBack instead of relying on the dot.
 */
@Composable
private fun WeekStrip(
    weekStart: String,
    selectedDate: String?,
    dayAvailability: Map<String, DayAvailability>,
    onShowWeek: (String) -> Unit,
    onDateSelected: (String) -> Unit,
) {
    val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
    val start = runCatching { LocalDate.parse(weekStart) }.getOrDefault(today)
    val canGoBack = start.isAfter(today)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onShowWeek(start.minusDays(availabilityWeekLength.toLong()).toString()) },
                enabled = canGoBack,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous week",
                )
            }
            Text(
                text = monthRangeLabel(start),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { onShowWeek(start.plusDays(availabilityWeekLength.toLong()).toString()) },
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week",
                )
            }
        }

        AnimatedContent(
            targetState = start,
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                (slideInHorizontally { width -> direction * width } + fadeIn()) togetherWith
                    (slideOutHorizontally { width -> -direction * width } + fadeOut())
            },
            label = "request-week-strip",
        ) { visibleStart ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RequestStepMargin),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                (0 until availabilityWeekLength).forEach { offset ->
                    val date = visibleStart.plusDays(offset.toLong())
                    DayCell(
                        date = date,
                        isPast = date.isBefore(today),
                        isSelected = date.toString() == selectedDate,
                        verdict = dayAvailability[date.toString()] ?: DayAvailability.UNKNOWN,
                        onClick = { onDateSelected(date.toString()) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        selectedDate?.let { date ->
            val verdict = dayAvailability[date] ?: DayAvailability.UNKNOWN
            val caption = when (verdict) {
                DayAvailability.OPEN -> "${formatRequestWeekday(date)}, ${formatRequestDate(date)}"
                DayAvailability.FULL ->
                    "${formatRequestWeekday(date)} is fully booked. Try another day."
                DayAvailability.CLOSED ->
                    "The clinic is closed on ${formatRequestWeekday(date)}. Try another day."
                DayAvailability.LOADING, DayAvailability.UNKNOWN ->
                    "${formatRequestWeekday(date)}, ${formatRequestDate(date)}"
            }
            Text(
                text = caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = RequestStepMargin)
                    .semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isPast: Boolean,
    isSelected: Boolean,
    verdict: DayAvailability,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = isPast ||
        verdict == DayAvailability.CLOSED ||
        verdict == DayAvailability.FULL
    val status = when {
        isPast -> "Past"
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
            .sizeIn(minHeight = 60.dp)
            .selectable(
                selected = isSelected,
                enabled = !unavailable,
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
                text = date.format(weekdayInitialFormat),
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
            AvailabilityDot(
                verdict = if (isPast) DayAvailability.CLOSED else verdict,
                isSelected = isSelected,
            )
        }
    }
}

@Composable
private fun AvailabilityDot(verdict: DayAvailability, isSelected: Boolean) {
    val dotColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        verdict == DayAvailability.OPEN -> EyecareColors.current.statusConfirmed
        else -> null
    }
    Box(modifier = Modifier.size(6.dp)) {
        if (dotColor != null) {
            Box(modifier = Modifier.fillMaxSize().background(dotColor, CircleShape))
        }
    }
}

// ------------------------------------------------------------ preference panel

@Composable
private fun PreferencesPanel(
    primarySlot: AvailabilitySlot,
    alternativeSlots: List<AvailabilitySlot>,
    canAddMore: Boolean,
    onAddAlternatives: () -> Unit,
    onRemoveAlternative: (AvailabilitySlot) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PreferenceRow(
                label = "Preferred",
                value = formatSlotWithDay(primarySlot),
                onRemove = null,
            )
            alternativeSlots.forEachIndexed { index, slot ->
                PreferenceRow(
                    label = "Backup ${index + 1}",
                    value = formatSlotWithDay(slot),
                    onRemove = { onRemoveAlternative(slot) },
                )
            }
            if (canAddMore) {
                AppointmentOutlinedButton(
                    text = if (alternativeSlots.isEmpty()) {
                        "Add backup times (optional)"
                    } else {
                        "Add another backup"
                    },
                    onClick = onAddAlternatives,
                )
                Text(
                    text = "Backup times give the clinic more ways to fit you in. " +
                        "They're optional.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    value: String,
    onRemove: (() -> Unit)?,
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
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onRemove != null) {
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Remove $label, $value",
                    tint = EyecareColors.current.accentText,
                )
            }
        }
    }
}

@Composable
private fun AlternativesBanner(
    primarySlot: AvailabilitySlot?,
    count: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Pick up to $maxAlternatives backup times",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            primarySlot?.let {
                Text(
                    text = "Your preferred time is ${formatSlotWithDay(it)}.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "$count of $maxAlternatives chosen",
                style = MaterialTheme.typography.bodySmall,
                color = EyecareColors.current.accentText,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

// -------------------------------------------------------------------- slot list

@Composable
private fun SlotList(
    state: RequestStep.Schedule,
    addingAlternatives: Boolean,
    alternativesFull: Boolean,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onToggleAlternative: (AvailabilitySlot) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val date = state.date
    if (date == null) {
        ScheduleMessage(
            text = "Choose a day above to see the times the clinic has open.",
            modifier = modifier,
        )
        return
    }

    when {
        state.isLoadingAvailability -> LoadingContent(modifier = modifier.fillMaxWidth())
        state.availabilityError != null -> ErrorContent(
            message = state.availabilityError,
            onRetry = onRetry,
            modifier = modifier.fillMaxWidth(),
        )
        else -> {
            val available = state.availability?.slots?.filter { it.available }.orEmpty()
            val dayStatus = state.availability?.dayStatus?.lowercase()
            when {
                dayStatus != null && dayStatus != "open" -> ScheduleMessage(
                    text = "The clinic is closed on ${formatRequestWeekday(date)}. " +
                        "Pick another day above.",
                    modifier = modifier,
                )
                available.isEmpty() -> ScheduleMessage(
                    text = "${formatRequestWeekday(date)} is fully booked. Pick another day above.",
                    modifier = modifier,
                )
                else -> {
                    val morning = available.filter { (parseSlotTime(it.startsAt)?.hour ?: 0) < 12 }
                    val afternoon = available.filter { (parseSlotTime(it.startsAt)?.hour ?: 0) >= 12 }

                    LazyColumn(
                        modifier = modifier,
                        contentPadding = PaddingValues(
                            start = RequestStepMargin,
                            end = RequestStepMargin,
                            bottom = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        slotSection(
                            icon = Icons.Outlined.WbSunny,
                            label = "Morning",
                            slots = morning,
                            state = state,
                            addingAlternatives = addingAlternatives,
                            alternativesFull = alternativesFull,
                            onSelectSlot = onSelectSlot,
                            onToggleAlternative = onToggleAlternative,
                        )
                        slotSection(
                            icon = Icons.Outlined.Bedtime,
                            label = "Afternoon",
                            slots = afternoon,
                            state = state,
                            addingAlternatives = addingAlternatives,
                            alternativesFull = alternativesFull,
                            onSelectSlot = onSelectSlot,
                            onToggleAlternative = onToggleAlternative,
                        )
                    }
                }
            }
        }
    }
}

private fun LazyListScope.slotSection(
    icon: ImageVector,
    label: String,
    slots: List<AvailabilitySlot>,
    state: RequestStep.Schedule,
    addingAlternatives: Boolean,
    alternativesFull: Boolean,
    onSelectSlot: (AvailabilitySlot) -> Unit,
    onToggleAlternative: (AvailabilitySlot) -> Unit,
) {
    if (slots.isEmpty()) return
    item(key = "header-$label") {
        TimePeriodHeader(icon = icon, label = label, count = slots.size)
    }
    items(slots, key = { it.startsAt }) { slot ->
        val alternativeIndex = state.alternativeSlots
            .indexOfFirst { it.startsAt == slot.startsAt }
            .takeIf { it >= 0 }
        val isPreferred = slot.startsAt == state.primarySlot?.startsAt

        if (addingAlternatives) {
            AlternativeSlotRow(
                slot = slot,
                alternativeNumber = alternativeIndex?.plus(1),
                isPreferred = isPreferred,
                // A full backup list still lets you untick what you already chose.
                enabled = !isPreferred && (alternativeIndex != null || !alternativesFull),
                onToggle = { onToggleAlternative(slot) },
            )
        } else {
            PreferredSlotRow(
                slot = slot,
                isSelected = isPreferred,
                alternativeNumber = alternativeIndex?.plus(1),
                onSelect = { onSelectSlot(slot) },
            )
        }
    }
}

@Composable
private fun PreferredSlotRow(
    slot: AvailabilitySlot,
    isSelected: Boolean,
    alternativeNumber: Int?,
    onSelect: () -> Unit,
) {
    SlotRowSurface(
        isActive = isSelected,
        modifier = Modifier.selectable(
            selected = isSelected,
            onClick = onSelect,
        ),
    ) {
        RadioButton(selected = isSelected, onClick = null)
        SlotRowText(slot = slot, isActive = isSelected)
        if (alternativeNumber != null) {
            SlotBadge(text = "Backup $alternativeNumber")
        }
    }
}

@Composable
private fun AlternativeSlotRow(
    slot: AvailabilitySlot,
    alternativeNumber: Int?,
    isPreferred: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val checked = alternativeNumber != null
    SlotRowSurface(
        isActive = checked || isPreferred,
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            onValueChange = { onToggle() },
        ),
    ) {
        Checkbox(checked = checked, onCheckedChange = null, enabled = enabled)
        SlotRowText(slot = slot, isActive = checked, dimmed = !enabled && !isPreferred)
        when {
            isPreferred -> SlotBadge(text = "Preferred")
            alternativeNumber != null -> SlotBadge(text = "Backup $alternativeNumber")
        }
    }
}

/**
 * One row shape for both phases: the whole surface is the tap target, so there is no dead
 * padding and no second hidden action competing for the same thumb.
 */
@Composable
private fun SlotRowSurface(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().then(modifier),
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun RowScope.SlotRowText(
    slot: AvailabilitySlot,
    isActive: Boolean,
    dimmed: Boolean = false,
) {
    val alpha = if (dimmed) 0.5f else 1f
    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = formatTimeRange(slot.startsAt, slot.endsAt),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
        )
        Text(
            text = formatSlotDuration(slot.startsAt, slot.endsAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
        )
    }
}

@Composable
private fun SlotBadge(text: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Text(
            text = text,
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clearAndSetSemantics { },
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = EyecareColors.current.accentText,
        )
    }
}

@Composable
private fun TimePeriodHeader(icon: ImageVector, label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
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
private fun ScheduleMessage(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = RequestStepMargin),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 24.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

private val fullMonthFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
private val shortMonthFormat = DateTimeFormatter.ofPattern("MMM")

private fun monthRangeLabel(start: LocalDate): String {
    val end = start.plusDays((availabilityWeekLength - 1).toLong())
    return if (start.month == end.month) {
        start.format(fullMonthFormat)
    } else {
        "${start.format(shortMonthFormat)} – ${end.format(shortMonthFormat)} ${end.year}"
    }
}
