package com.eyecare.app.presentation.appointments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareTheme
import com.eyecare.app.ui.theme.OnSurfaceVariant
import com.eyecare.app.ui.theme.StatusCancelled
import com.eyecare.app.ui.theme.StatusConfirmed
import com.eyecare.app.ui.theme.StatusInfo
import com.eyecare.app.ui.theme.StatusPending
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

internal enum class AppointmentListTab { UPCOMING, HISTORY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToBook: () -> Unit,
    viewModel: AppointmentListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = uiState is AppointmentListUiState.Loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = uiState) {
                is AppointmentListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AppointmentListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No appointments yet.", style = MaterialTheme.typography.bodyMedium)
                }
                is AppointmentListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is AppointmentListUiState.Success -> {
                    var selectedTab by remember { mutableStateOf(AppointmentListTab.UPCOMING) }
                    var dateFilterEnabled by remember { mutableStateOf(false) }
                    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
                    val weekDays = remember(selectedDate) { appointmentWeekDays(selectedDate) }
                    val appointmentCounts = remember(state.appointments) { appointmentCountsByDate(state.appointments) }
                    val appointmentsForSelectedTab = remember(state.appointments, selectedTab) {
                        appointmentsForTab(state.appointments, selectedTab)
                    }
                    val visibleAppointments = remember(
                        appointmentsForSelectedTab,
                        selectedTab,
                        dateFilterEnabled,
                        selectedDate,
                    ) {
                        if (selectedTab == AppointmentListTab.UPCOMING && dateFilterEnabled) {
                            appointmentsForSelectedTab.filter { appointment ->
                                appointmentOccursOnDate(appointment.scheduledAt, selectedDate)
                            }
                        } else {
                            appointmentsForSelectedTab
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            AppointmentListHeader(
                                dateFilterEnabled = dateFilterEnabled,
                                showCalendarAction = selectedTab == AppointmentListTab.UPCOMING,
                                onCalendarClick = { dateFilterEnabled = !dateFilterEnabled },
                            )
                            Spacer(Modifier.height(16.dp))
                            AppointmentListTabs(
                                selectedTab = selectedTab,
                                onTabSelected = {
                                    selectedTab = it
                                    if (it == AppointmentListTab.HISTORY) dateFilterEnabled = false
                                },
                            )
                        }
                        if (selectedTab == AppointmentListTab.UPCOMING && dateFilterEnabled) {
                            item {
                                WeeklyAppointmentCalendar(
                                    weekDays = weekDays,
                                    selectedDate = selectedDate,
                                    appointmentCounts = appointmentCounts,
                                    onPreviousWeek = { selectedDate = selectedDate.minusWeeks(1) },
                                    onNextWeek = { selectedDate = selectedDate.plusWeeks(1) },
                                    onDateSelected = { selectedDate = it },
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                ) {
                                    TextButton(onClick = { dateFilterEnabled = false }) {
                                        Text("Show all upcoming")
                                    }
                                }
                            }
                        }
                        if (visibleAppointments.isEmpty()) {
                            item {
                                if (selectedTab == AppointmentListTab.UPCOMING && dateFilterEnabled) {
                                    EmptyDayCard(selectedDate)
                                } else {
                                    EmptyAppointmentTab(selectedTab)
                                }
                            }
                        } else {
                            items(visibleAppointments, key = { it.id }) { appointment ->
                                AppointmentCard(
                                    appointment = appointment,
                                    onClick = { onNavigateToDetail(appointment.id) },
                                )
                            }
                        }
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = onNavigateToBook,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 116.dp),
            icon = {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = null,
                )
            },
            text = {
                Text(
                    "Book appointment",
                    fontWeight = FontWeight.SemiBold,
                )
            },
            shape = RoundedCornerShape(50),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

@Composable
private fun AppointmentListHeader(
    dateFilterEnabled: Boolean,
    showCalendarAction: Boolean,
    onCalendarClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Appointments", style = MaterialTheme.typography.displayLarge)
        IconButton(
            onClick = onCalendarClick,
            enabled = showCalendarAction,
            modifier = Modifier.size(48.dp),
        ) {
            if (showCalendarAction) {
                Icon(
                    Icons.Outlined.CalendarMonth,
                    contentDescription = if (dateFilterEnabled) "Close date filter" else "Filter by date",
                    tint = if (dateFilterEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun AppointmentListTabs(
    selectedTab: AppointmentListTab,
    onTabSelected: (AppointmentListTab) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        AppointmentListTab.entries.forEach { tab ->
            SegmentedButton(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                shape = SegmentedButtonDefaults.itemShape(index = tab.ordinal, count = AppointmentListTab.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    activeContentColor = MaterialTheme.colorScheme.primary,
                    activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                label = { Text(if (tab == AppointmentListTab.UPCOMING) "Upcoming" else "History") },
            )
        }
    }
}

@Composable
private fun WeeklyAppointmentCalendar(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    appointmentCounts: Map<LocalDate, Int>,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
) {
    val visibleWeekStart = weekDays.firstOrNull() ?: selectedDate

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        WeekNavigationButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "Previous week",
            onClick = onPreviousWeek,
        )

        AnimatedContent(
            targetState = visibleWeekStart,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val direction = if (targetState > initialState) 1 else -1
                val enter = slideInHorizontally { width -> direction * width } + fadeIn()
                val exit = slideOutHorizontally { width -> -direction * width } + fadeOut()
                enter togetherWith exit using SizeTransform(clip = false)
            },
            label = "appointment-week-calendar",
        ) { weekStart ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                appointmentWeekDays(weekStart).forEach { date ->
                    val isSelected = date == selectedDate
                    val hasAppointment = appointmentCounts.containsKey(date)
                    Surface(
                        onClick = { onDateSelected(date) },
                        modifier = Modifier.size(38.dp),
                        shape = CircleShape,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isSelected) 0.dp else 1.dp,
                        shadowElevation = if (isSelected) 0.dp else 1.dp,
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 2.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("E", Locale.US)).take(1).uppercase(Locale.US),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, lineHeight = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                            Text(
                                text = date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 14.sp),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                            )
                            Surface(
                                modifier = Modifier.size(4.dp),
                                shape = CircleShape,
                                color = when {
                                    isSelected && hasAppointment -> Color.White
                                    hasAppointment -> MaterialTheme.colorScheme.primary
                                    else -> Color.Transparent
                                },
                            ) {}
                        }
                    }
                }
            }
        }

        WeekNavigationButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "Next week",
            onClick = onNextWeek,
        )
    }
}
@Composable
private fun WeekNavigationButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun EmptyDayCard(selectedDate: LocalDate) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "No appointments",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "Nothing scheduled for ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EmptyAppointmentTab(tab: AppointmentListTab) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (tab == AppointmentListTab.UPCOMING) "No upcoming appointments" else "No appointment history",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (tab == AppointmentListTab.UPCOMING) "Book an appointment when you're ready."
                else "Completed and cancelled appointments will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AppointmentStatusPill(appointment.status)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Text(
                    formatAppointmentTitle(appointment.visitReason),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                AppointmentInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    text = formatAppointmentDate(appointment.scheduledAt),
                )
                AppointmentInfoRow(
                    icon = Icons.Outlined.AccessTime,
                    text = formatAppointmentTime(appointment.scheduledAt),
                )
            }
        }
    }
}

@Composable
private fun AppointmentInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AppointmentStatusPill(status: AppointmentStatus) {
    val (label, color) = appointmentStatusLabelAndColor(status)

    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            text = label.uppercase(Locale.US),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
fun StatusChip(status: AppointmentStatus, textColor: Color = Color.Unspecified) {
    val (label, color) = appointmentStatusLabelAndColor(status)
    SuggestionChip(
        onClick = {},
        modifier = Modifier.width(110.dp),
        label = {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        colors = SuggestionChipDefaults.suggestionChipColors(
            containerColor = color.copy(alpha = 0.15f),
        ),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = color),
    )
}

private fun appointmentStatusLabelAndColor(status: AppointmentStatus): Pair<String, Color> = when (status) {
    AppointmentStatus.PENDING -> "Pending" to StatusPending
    AppointmentStatus.CONFIRMED -> "Confirmed" to StatusConfirmed
    AppointmentStatus.ARRIVED -> "Arrived" to StatusInfo
    AppointmentStatus.COMPLETED -> "Completed" to OnSurfaceVariant
    AppointmentStatus.NO_SHOW -> "No Show" to StatusCancelled
    AppointmentStatus.CANCELLED -> "Cancelled" to StatusCancelled
}

internal fun formatAppointmentTitle(visitReason: String): String = visitReason
    .replace("_", " ")
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word ->
        word.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
    }

internal fun formatAppointmentDate(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    return parsed?.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        ?: scheduledAt.take(10)
}

internal fun formatAppointmentTime(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    return parsed?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        ?: scheduledAt.drop(11).take(5).ifBlank { "Time TBD" }
}

internal fun appointmentOccursOnDate(scheduledAt: String, date: LocalDate): Boolean =
    parseAppointmentDate(scheduledAt) == date

internal fun appointmentWeekDays(selectedDate: LocalDate): List<LocalDate> {
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    // Mon–Sat only — clinic is closed on Sundays
    return List(6) { index -> weekStart.plusDays(index.toLong()) }
}

private fun appointmentCountsByDate(appointments: List<Appointment>): Map<LocalDate, Int> =
    appointments.mapNotNull { parseAppointmentDate(it.scheduledAt) }.groupingBy { it }.eachCount()

internal fun appointmentsForTab(
    appointments: List<Appointment>,
    tab: AppointmentListTab,
    now: LocalDateTime = LocalDateTime.now(),
): List<Appointment> {
    val terminalStatuses = setOf(
        AppointmentStatus.COMPLETED,
        AppointmentStatus.NO_SHOW,
        AppointmentStatus.CANCELLED,
    )
    val (upcoming, history) = appointments.partition { appointment ->
        val dateTime = parseAppointmentDateTime(appointment.scheduledAt)
        appointment.status !in terminalStatuses && (dateTime == null || !dateTime.isBefore(now))
    }
    return when (tab) {
        AppointmentListTab.UPCOMING -> upcoming.sortedBy { appointmentSortKey(it.scheduledAt) }
        AppointmentListTab.HISTORY -> history.sortedByDescending { appointmentSortKey(it.scheduledAt) }
    }
}

private fun appointmentSortKey(scheduledAt: String): LocalDateTime =
    parseAppointmentDateTime(scheduledAt) ?: LocalDateTime.MIN

private fun parseAppointmentDate(value: String): LocalDate? = parseAppointmentDateTime(value)?.toLocalDate()

private fun parseAppointmentDateTime(value: String): LocalDateTime? =
    runCatching {
        OffsetDateTime.parse(value).atZoneSameInstant(CLINIC_TIME_ZONE).toLocalDateTime()
    }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(value.replace(" ", "T").removeSuffix("Z")) }.getOrNull()

@Preview(showBackground = true)
@Composable
private fun AppointmentListPreview() {
    EyecareTheme {
        AppointmentListScreen(onNavigateToDetail = {}, onNavigateToBook = {})
    }
}
