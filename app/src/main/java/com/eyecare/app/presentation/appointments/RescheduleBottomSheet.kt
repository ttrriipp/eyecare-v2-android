package com.eyecare.app.presentation.appointments

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
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
    onShowWeek: (String) -> Unit,
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Reschedule appointment",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Choose a day and a time the clinic has confirmed as available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            RescheduleWeekStrip(
                weekStart = weekStart,
                selectedDate = selectedDate,
                dayAvailability = dayAvailability,
                onShowWeek = onShowWeek,
                onDateSelected = { date ->
                    if (date != selectedDate) {
                        selectedDate = date
                        onDateChanged(date)
                    }
                },
            )

            RescheduleSlotSection(
                availabilityState = availabilityState,
                availableSlots = availableSlots,
                selectedSlotStartsAt = selectedSlotStartsAt,
                isSubmitting = isSubmitting,
                onSelectSlot = { selectedSlotStartsAt = it },
                onRetryAvailability = onRetryAvailability,
            )

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

            AppointmentPrimaryButton(
                text = "Review reschedule",
                onClick = { showConfirmDialog = true },
                enabled = canConfirm,
                loading = isSubmitting,
            )
        }
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
    val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
    val currentWeekStart = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val start = weekStart
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: currentWeekStart
    val canGoBack = start.isAfter(currentWeekStart)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onShowWeek(start.minusDays(availabilityWeekLength.toLong()).toString()) },
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
                onClick = { onShowWeek(start.plusDays(availabilityWeekLength.toLong()).toString()) },
            ) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next week")
            }
        }

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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                (0 until availabilityWeekLength).forEach { offset ->
                    val date = visibleStart.plusDays(offset.toLong())
                    RescheduleDayCell(
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
private fun RescheduleDayCell(
    date: LocalDate,
    isPast: Boolean,
    isSelected: Boolean,
    verdict: DayAvailability,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unavailable = isPast || verdict == DayAvailability.CLOSED || verdict == DayAvailability.FULL
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
            .selectable(selected = isSelected, enabled = !unavailable, onClick = onClick)
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
            RescheduleDayMarker(verdict = if (isPast) DayAvailability.CLOSED else verdict, isSelected = isSelected)
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
    selectedSlotStartsAt: String?,
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
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
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

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (morning.isNotEmpty()) {
                        RescheduleTimePeriodHeader(Icons.Outlined.WbSunny, "Morning", morning.size)
                        morning.forEach { slot ->
                            RescheduleSlotRow(
                                slot = slot,
                                selected = slot.startsAt == selectedSlotStartsAt,
                                enabled = !isSubmitting,
                                onSelect = { onSelectSlot(slot.startsAt) },
                            )
                        }
                    }
                    if (afternoon.isNotEmpty()) {
                        RescheduleTimePeriodHeader(Icons.Outlined.Bedtime, "Afternoon", afternoon.size)
                        afternoon.forEach { slot ->
                            RescheduleSlotRow(
                                slot = slot,
                                selected = slot.startsAt == selectedSlotStartsAt,
                                enabled = !isSubmitting,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, onClick = onSelect),
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
            RadioButton(selected = selected, onClick = null, enabled = enabled)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTimeRange(slot.startsAt, slot.endsAt),
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
        }
    }
}

private fun formatRescheduleDate(startsAt: String): String = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).format(rescheduleDateFormatter)
}.getOrDefault(startsAt)

private fun formatRescheduleTime(startsAt: String): String = runCatching {
    Instant.parse(startsAt).atZone(CLINIC_TIME_ZONE).format(rescheduleTimeFormatter)
}.getOrDefault(startsAt)

private fun sameInstant(first: String, second: String): Boolean = runCatching {
    Instant.parse(first) == Instant.parse(second)
}.getOrDefault(first == second)
