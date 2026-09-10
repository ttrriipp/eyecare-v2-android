package com.eyecare.app.presentation.appointments

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.filled.Star
import com.eyecare.app.presentation.appointments.components.VisitFeedbackDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.appointments.requests.AppointmentRequestListViewModel
import com.eyecare.app.presentation.appointments.requests.AppointmentRequestStatusPill
import com.eyecare.app.presentation.appointments.requests.RequestListState
import com.eyecare.app.presentation.appointments.requests.activeAppointmentRequestCount
import com.eyecare.app.presentation.appointments.requests.hasReachedActiveAppointmentRequestLimit
import com.eyecare.app.presentation.appointments.requests.requestStatusPresentation
import com.eyecare.app.ui.theme.EyecareTheme
import com.eyecare.app.ui.theme.EyecareColors
import kotlinx.coroutines.delay
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
    onNavigateToRequest: () -> Unit,
    onNavigateToRequestDetail: (Int) -> Unit = {},
    accountId: Int? = null,
    hasActivePatientLink: Boolean = true,
    viewModel: AppointmentListViewModel = hiltViewModel(),
    requestViewModel: AppointmentRequestListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestState by requestViewModel.state.collectAsStateWithLifecycle()
    val requestLimitReached = (requestState as? RequestListState.Data)
        ?.requests
        ?.let(::hasReachedActiveAppointmentRequestLimit) == true

    LaunchedEffect(hasActivePatientLink, accountId) {
        viewModel.refresh(
            hasActivePatientLink = hasActivePatientLink,
            accountId = accountId,
        )
    }

    RefreshOnResumeEffect(
        onRefresh = {
            viewModel.refresh(
                hasActivePatientLink = hasActivePatientLink,
                accountId = accountId,
            )
            requestViewModel.onScreenResumed()
        },
    )

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = (uiState as? AppointmentListUiState.Success)?.isRefreshing == true ||
                (requestState as? RequestListState.Data)?.isRefreshing == true,
            onRefresh = {
                viewModel.refresh(
                    hasActivePatientLink = hasActivePatientLink,
                    accountId = accountId,
                )
                requestViewModel.refresh()
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            AppointmentListContent(
                confirmedState = uiState,
                requestState = requestState,
                onRetryConfirmed = {
                    viewModel.refresh(
                        hasActivePatientLink = hasActivePatientLink,
                        accountId = accountId,
                    )
                },
                onLoadMoreAppointments = viewModel::loadMore,
                onNavigateToDetail = onNavigateToDetail,
                onNavigateToRequestDetail = onNavigateToRequestDetail,
                onLoadMoreRequests = requestViewModel::loadMore,
                onRefreshRequests = requestViewModel::refresh,
                onRateClick = { viewModel.showRatingDialog(it) },
            )
        }

        // Visit feedback dialog
        val successState = uiState as? AppointmentListUiState.Success
        if (successState?.ratingAppointmentId != null) {
            val appointment = successState.appointments.find { it.id == successState.ratingAppointmentId }
            val existingRating = appointment?.visitRating
            VisitFeedbackDialog(
                onSubmit = viewModel::submitRating,
                onDismiss = viewModel::dismissRatingDialog,
                initialRating = existingRating?.rating ?: 0,
                initialComment = existingRating?.comment ?: "",
                title = if (existingRating != null) "Update your rating" else "Rate your visit",
                isSubmitting = successState.isSubmittingRating,
                errorMessage = successState.ratingError,
            )
        }

        if (!requestLimitReached) {
            ExtendedFloatingActionButton(
                onClick = onNavigateToRequest,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 88.dp),
                icon = {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                    )
                },
                text = {
                    // "Book" promised a confirmed slot the clinic has not agreed to yet.
                    Text(
                        "Request appointment",
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                shape = RoundedCornerShape(50),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 3.dp,
                    focusedElevation = 2.dp,
                    hoveredElevation = 2.dp,
                ),
            )
        }
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
        verticalAlignment = Alignment.Top,
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
                    tint = if (dateFilterEnabled) EyecareColors.current.accentText
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
                    activeContentColor = EyecareColors.current.accentText,
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
    onTodayClick: () -> Unit,
    onClearFilter: () -> Unit,
) {
    val visibleWeekStart = weekDays.firstOrNull() ?: selectedDate
    val today = LocalDate.now()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeekNavigationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Previous week",
                    onClick = onPreviousWeek,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Week of ${appointmentWeekRangeLabel(appointmentWeekDays(visibleWeekStart))}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = onTodayClick,
                            enabled = selectedDate != today,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("Today")
                        }
                        TextButton(
                            onClick = onClearFilter,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text("Clear")
                        }
                    }
                }
                WeekNavigationButton(
                    icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Next week",
                    onClick = onNextWeek,
                )
            }

            AnimatedContent(
                targetState = visibleWeekStart,
                modifier = Modifier.fillMaxWidth(),
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    val enter = slideInHorizontally { width -> direction * width } + fadeIn()
                    val exit = slideOutHorizontally { width -> -direction * width } + fadeOut()
                    enter togetherWith exit using SizeTransform(clip = true)
                },
                label = "appointment-week-calendar",
            ) { weekStart ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        appointmentWeekDays(weekStart).forEach { date ->
                            val isSelected = date == selectedDate
                            val isToday = date == today
                            val appointmentCount = appointmentCounts[date] ?: 0
                            val dayDescription = buildString {
                                append(date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.US)))
                                append(if (isSelected) ", selected" else ", not selected")
                                append(". ")
                                append(
                                    when (appointmentCount) {
                                        0 -> "No appointments or requests"
                                        1 -> "1 appointment or request"
                                        else -> "$appointmentCount appointments or requests"
                                    },
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.Button,
                                        onClick = { onDateSelected(date) },
                                    )
                                    .semantics {
                                        contentDescription = dayDescription
                                        stateDescription = if (isSelected) "Selected" else "Not selected"
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                    border = when {
                                        isSelected -> null
                                        isToday -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                    },
                                    tonalElevation = if (isSelected) 0.dp else 1.dp,
                                    shadowElevation = if (isSelected) 0.dp else 1.dp,
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            text = date.format(DateTimeFormatter.ofPattern("EE", Locale.US)),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = date.dayOfMonth.toString(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.onPrimary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            maxLines = 1,
                                        )
                                        Surface(
                                            modifier = Modifier.size(4.dp),
                                            shape = CircleShape,
                                            color = when {
                                                isSelected && appointmentCount > 0 -> MaterialTheme.colorScheme.onPrimary
                                                appointmentCount > 0 -> EyecareColors.current.accentText
                                                else -> Color.Transparent
                                            },
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(6.dp),
                    shape = CircleShape,
                    color = EyecareColors.current.accentText,
                ) {}
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Dates with appointments or requests",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
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
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = EyecareColors.current.accentText,
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
                "Nothing for this day",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                "No appointments or requests on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM d", Locale.US))}.",
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
                if (tab == AppointmentListTab.UPCOMING) "No upcoming visits or requests" else "No appointment history",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                if (tab == AppointmentListTab.UPCOMING) {
                    "Request an appointment when you're ready. The clinic will confirm the visit."
                } else {
                    "Completed and cancelled visits will appear here."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    appointment: AppointmentV1,
    onClick: () -> Unit,
    onRateClick: (() -> Unit)? = null,
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
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppointmentStatusPill(appointment.status)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(9.dp),
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
                            formatAppointmentTitle(appointment.appointmentType),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }

                AppointmentInfoRow(
                    icon = Icons.Outlined.CalendarMonth,
                    text = formatAppointmentDate(appointment.scheduledAt),
                )
                AppointmentInfoRow(
                    icon = Icons.Outlined.AccessTime,
                    text = formatAppointmentTime(appointment.scheduledAt),
                )
                AppointmentInfoRow(
                    icon = Icons.Outlined.AccessTime,
                    text = "${appointment.durationMinutes} min visit",
                )

                if (onRateClick != null) {
                    SuggestionChip(
                        onClick = onRateClick,
                        label = { Text("Rate this visit") },
                        icon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                } else if (appointment.visitRating != null) {
                    SuggestionChip(
                        onClick = {},
                        enabled = false,
                        label = { Text("Rated") },
                        icon = {
                            Icon(
                                Icons.Filled.Star,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            iconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                }
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
        modifier = Modifier.fillMaxWidth(),
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
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal fun formatAppointmentTitle(visitReason: String): String = visitReason
    .replace("_", " ")
    .trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .joinToString(" ") { word ->
        word.lowercase(Locale.US).replaceFirstChar { char -> char.titlecase(Locale.US) }
    }
    .ifBlank { "Appointment" }

internal fun formatAppointmentDate(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    return parsed?.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        ?: runCatching {
            LocalDate.parse(scheduledAt.take(10))
                .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
        }.getOrDefault("Date TBD")
}

internal fun formatAppointmentTime(scheduledAt: String): String {
    val parsed = parseAppointmentDateTime(scheduledAt)
    val fallback = scheduledAt.drop(11).take(5)
    return parsed?.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
        ?: fallback.takeIf { it.matches(Regex("\\d{2}:\\d{2}")) } ?: "Time TBD"
}

internal fun appointmentRequestTitle(request: AppointmentRequest): String =
    if (request.requestType == AppointmentRequestType.RESCHEDULE) {
        "Reschedule request"
    } else {
        request.appointmentType?.name?.takeIf { it.isNotBlank() } ?: "Appointment details unavailable"
    }

internal fun appointmentRequestDurationLabel(request: AppointmentRequest): String? =
    (request.provisionalDurationMinutes ?: request.appointmentType?.durationMinutes)
        ?.let { "$it min visit" }

internal fun appointmentOccursOnDate(scheduledAt: String, date: LocalDate): Boolean =
    parseAppointmentDate(scheduledAt) == date

internal fun appointmentWeekDays(selectedDate: LocalDate): List<LocalDate> {
    val weekStart = selectedDate.minusDays((selectedDate.dayOfWeek.value - DayOfWeek.MONDAY.value).toLong())
    // Mon–Sat only — clinic is closed on Sundays
    return List(6) { index -> weekStart.plusDays(index.toLong()) }
}

internal fun appointmentWeekRangeLabel(weekDays: List<LocalDate>): String {
    val start = weekDays.firstOrNull() ?: LocalDate.now()
    val end = weekDays.lastOrNull() ?: start
    val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
    return "${start.format(formatter)} - ${end.format(formatter)}"
}

internal fun appointmentsForTab(
    appointments: List<AppointmentV1>,
    tab: AppointmentListTab,
    now: LocalDateTime = LocalDateTime.now(),
): List<AppointmentV1> {
    val terminalStatuses = setOf(
        AppointmentStatus.FULFILLED,
        AppointmentStatus.NO_SHOW,
        AppointmentStatus.CANCELLED,
        AppointmentStatus.UNKNOWN,
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

@Composable
private fun AppointmentListContent(
    confirmedState: AppointmentListUiState,
    requestState: RequestListState,
    onRetryConfirmed: () -> Unit = {},
    onLoadMoreAppointments: () -> Unit = {},
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToRequestDetail: (Int) -> Unit,
    onLoadMoreRequests: () -> Unit,
    onRefreshRequests: () -> Unit,
    onRateClick: (Int) -> Unit = {},
) {
    // rememberSaveable (not remember) so this survives the composable being torn down and
    // recreated - e.g. across a process-death config change, or if a caller ever routes
    // through a transient Loading state again in the future.
    var selectedTab by rememberSaveable { mutableStateOf(AppointmentListTab.UPCOMING) }
    var dateFilterEnabled by rememberSaveable { mutableStateOf(false) }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    val weekDays = remember(selectedDate) { appointmentWeekDays(selectedDate) }
    val confirmedSuccess = confirmedState as? AppointmentListUiState.Success
    val appointments = confirmedSuccess?.appointments.orEmpty()
    val requests = (requestState as? RequestListState.Data)?.requests.orEmpty()
    val activeRequestCount = activeAppointmentRequestCount(requests)
    val confirmedAppointmentIds = remember(appointments) { appointments.map { it.id }.toSet() }
    val appointmentsForSelectedTab = remember(appointments, selectedTab) {
        appointmentsForTab(appointments, selectedTab)
    }
    val requestsForSelectedTab = remember(requests, selectedTab, confirmedAppointmentIds) {
        appointmentRequestsForTab(requests, selectedTab, confirmedAppointmentIds)
    }
    val appointmentCounts = remember(appointmentsForSelectedTab, requestsForSelectedTab) {
        (appointmentsForSelectedTab.mapNotNull { parseAppointmentDate(it.scheduledAt) } +
            requestsForSelectedTab.mapNotNull { parseAppointmentDate(it.scheduledAt) })
            .groupingBy { it }
            .eachCount()
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
    val visibleRequests = remember(
        requestsForSelectedTab,
        selectedTab,
        dateFilterEnabled,
        selectedDate,
    ) {
        if (selectedTab == AppointmentListTab.UPCOMING && dateFilterEnabled) {
            requestsForSelectedTab.filter { request ->
                appointmentOccursOnDate(request.scheduledAt, selectedDate)
            }
        } else {
            requestsForSelectedTab
        }
    }
    val requestData = requestState as? RequestListState.Data
    val requestRefreshError = requestData?.error
    val confirmedRefreshError = confirmedSuccess?.refreshError
    val hasBlockingState = requestState is RequestListState.Loading ||
        requestState is RequestListState.Error ||
        confirmedState is AppointmentListUiState.Loading ||
        confirmedState is AppointmentListUiState.Error
    val hasStaleDataWarning = requestRefreshError != null || confirmedRefreshError != null
    val isInitialLoading = visibleAppointments.isEmpty() &&
        visibleRequests.isEmpty() &&
        (requestState is RequestListState.Loading || confirmedState is AppointmentListUiState.Loading)
    var showInitialLoading by remember { mutableStateOf(false) }

    LaunchedEffect(isInitialLoading) {
        showInitialLoading = false
        if (isInitialLoading) {
            delay(300)
            showInitialLoading = true
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
                    onTodayClick = { selectedDate = LocalDate.now() },
                    onClearFilter = {
                        dateFilterEnabled = false
                        selectedDate = LocalDate.now()
                    },
                )
            }
        }
        if (showInitialLoading) {
            item { AppointmentsLoadingCard() }
        }
        if (requestState is RequestListState.Error) {
            item {
                RequestListErrorRow(
                    message = requestState.message,
                    onRetry = onRefreshRequests,
                )
            }
        }
        if (requestRefreshError != null) {
            item {
                RequestListErrorRow(
                    message = requestRefreshError,
                    onRetry = onRefreshRequests,
                )
            }
        }
        if (confirmedState is AppointmentListUiState.Error) {
            item {
                RequestListErrorRow(
                    message = confirmedState.message,
                    onRetry = onRetryConfirmed,
                )
            }
        }
        if (confirmedRefreshError != null) {
            item {
                RequestListErrorRow(
                    message = confirmedRefreshError,
                    onRetry = onRetryConfirmed,
                )
            }
        }
        if (selectedTab == AppointmentListTab.UPCOMING &&
            hasReachedActiveAppointmentRequestLimit(requests)
        ) {
            item {
                AppointmentRequestLimitNotice(activeRequestCount = activeRequestCount)
            }
        }
        if (visibleRequests.isNotEmpty()) {
            item {
                Text(
                    text = "Appointment requests",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(visibleRequests, key = { "request-${it.id}" }) { request ->
                AppointmentRequestCard(
                    request = request,
                    onClick = { onNavigateToRequestDetail(request.id) },
                    onViewConfirmed = onNavigateToDetail,
                )
            }
        }
        if (visibleAppointments.isNotEmpty()) {
            item {
                Text(
                    text = "Confirmed appointments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            items(visibleAppointments, key = { "appointment-${it.id}" }) { appointment ->
                AppointmentCard(
                    appointment = appointment,
                    onClick = { onNavigateToDetail(appointment.id) },
                    onRateClick = if (appointment.isRateable && appointment.visitRating == null) {
                        { onRateClick(appointment.id) }
                    } else null,
                )
            }
        }
        if (visibleAppointments.isEmpty() &&
            visibleRequests.isEmpty() &&
            !hasBlockingState &&
            !hasStaleDataWarning
        ) {
            item {
                if (selectedTab == AppointmentListTab.UPCOMING && dateFilterEnabled) {
                    EmptyDayCard(selectedDate)
                } else {
                    EmptyAppointmentTab(selectedTab)
                }
            }
        }
        if (requestData?.hasMore == true) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    requestData.appendError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = onLoadMoreRequests,
                        enabled = !requestData.isLoadingMore,
                    ) {
                        if (requestData.isLoadingMore) CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        else Text("Load more requests")
                    }
                }
            }
        }
        if (confirmedSuccess?.hasMorePages == true) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    confirmedSuccess.loadMoreError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(
                        onClick = onLoadMoreAppointments,
                        enabled = !confirmedSuccess.isLoadingMore,
                    ) {
                        if (confirmedSuccess.isLoadingMore) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp))
                        } else {
                            Text("Load more confirmed appointments")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentsLoadingCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Loading appointments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "Checking your requests and confirmed visits",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun RequestListErrorRow(
    message: String,
    onRetry: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun AppointmentRequestLimitNotice(activeRequestCount: Int) {
    val requestLabel = if (activeRequestCount == 1) {
        "pending appointment request"
    } else {
        "pending appointment requests"
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Request limit reached",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "$activeRequestCount $requestLabel. " +
                        "Wait for a clinic response or cancel one to start another.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun AppointmentRequestCard(
    request: AppointmentRequest,
    onClick: () -> Unit,
    onViewConfirmed: (Int) -> Unit,
) {
    val presentation = requestStatusPresentation(request.status)
    val confirmedAppointmentId = request.appointmentId
    val durationLabel = appointmentRequestDurationLabel(request)
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppointmentRequestStatusPill(request.status, presentation.label)

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
                        text = appointmentRequestTitle(request),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Request ${request.requestNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                    Text(
                        text = if (request.requestType == AppointmentRequestType.RESCHEDULE) {
                            "Requested new time"
                        } else {
                            "Your preferred time"
                        },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                AppointmentInfoRow(Icons.Outlined.CalendarMonth, formatAppointmentDate(request.scheduledAt))
                AppointmentInfoRow(Icons.Outlined.AccessTime, formatAppointmentTime(request.scheduledAt))
                durationLabel?.let { duration ->
                    AppointmentInfoRow(Icons.Outlined.AccessTime, duration)
                }

                if (presentation.showViewConfirmed && confirmedAppointmentId != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { onViewConfirmed(confirmedAppointmentId) }) {
                            Text("View confirmed appointment")
                        }
                    }
                }
            }
        }
    }
}

internal fun appointmentRequestsForTab(
    requests: List<AppointmentRequest>,
    tab: AppointmentListTab,
    confirmedAppointmentIds: Set<Int> = emptySet(),
    now: LocalDateTime = LocalDateTime.now(),
): List<AppointmentRequest> {
    val visible = requests.filter { request ->
        val isAlreadyListedAsConfirmed = request.status == AppointmentRequestStatus.ACCEPTED &&
            request.appointmentId != null &&
            request.appointmentId in confirmedAppointmentIds
        if (isAlreadyListedAsConfirmed) return@filter false

        val scheduledAt = parseAppointmentDateTime(request.scheduledAt)
        val isUpcomingDate = scheduledAt == null || !scheduledAt.isBefore(now)
        when (tab) {
            AppointmentListTab.UPCOMING -> request.status == AppointmentRequestStatus.PENDING ||
                (request.status == AppointmentRequestStatus.ACCEPTED && isUpcomingDate)
            AppointmentListTab.HISTORY -> request.status != AppointmentRequestStatus.PENDING &&
                !(request.status == AppointmentRequestStatus.ACCEPTED && isUpcomingDate)
        }
    }

    return when (tab) {
        AppointmentListTab.UPCOMING -> visible.sortedBy { appointmentSortKey(it.scheduledAt) }
        AppointmentListTab.HISTORY -> visible.sortedByDescending { appointmentSortKey(it.scheduledAt) }
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
        AppointmentListScreen(onNavigateToDetail = {}, onNavigateToRequest = {})
    }
}
