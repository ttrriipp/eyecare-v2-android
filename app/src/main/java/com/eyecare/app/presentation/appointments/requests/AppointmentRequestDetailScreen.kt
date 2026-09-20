package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.PATIENT_CANCELLATION_REASON_MAX_LENGTH
import com.eyecare.app.presentation.appointments.RescheduleBottomSheet
import com.eyecare.app.presentation.appointments.SAME_DAY_CANCELLATION_MESSAGE
import com.eyecare.app.presentation.appointments.isSameDayInClinic
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.CancellationReasonChoice
import com.eyecare.app.presentation.appointments.components.CancellationReasonPicker
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun AppointmentRequestDetailScreen(
    requestId: Int,
    isLinked: Boolean = false,
    onBack: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    viewModel: AppointmentRequestDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(requestId, isLinked) {
        viewModel.setLinked(isLinked)
        viewModel.load(requestId)
    }

    val state by viewModel.state.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }
    var cancelReasonChoice by remember { mutableStateOf<CancellationReasonChoice>(CancellationReasonChoice.Other) }
    val isCancelling = (state as? RequestDetailState.Data)?.isCancelling == true

    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel this request?",
            message = "This can't be undone. Please provide a reason for the clinic.",
            supportingContent = {
                CancellationReasonPicker(
                    choice = cancelReasonChoice,
                    reason = cancelReason,
                    onChoiceChange = { cancelReasonChoice = it },
                    onReasonChange = { cancelReason = it },
                    enabled = !isCancelling,
                )
            },
            confirmLabel = "Cancel request",
            dismissLabel = "Keep",
            confirmEnabled = cancelReason.isNotBlank() &&
                cancelReason.length <= PATIENT_CANCELLATION_REASON_MAX_LENGTH &&
                !isCancelling,
            onConfirm = {
                val reason = cancelReason.trim()
                showCancelDialog = false
                if (reason.isNotBlank()) viewModel.cancel(reason)
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    when (val s = state) {
        is RequestDetailState.Loading -> RequestDetailLoadingContent(onBack = onBack)
        is RequestDetailState.Data -> RequestDetailDataContent(
            state = s,
            onBack = onBack,
            onCancelClick = {
                cancelReasonChoice = CancellationReasonChoice.Other
                cancelReason = ""
                showCancelDialog = true
            },
            onRefresh = viewModel::refresh,
            onMessageClick = onNavigateToMessages,
            onEditScheduleClick = viewModel::showScheduleSheet,
            onDismissScheduleSheet = viewModel::dismissScheduleSheet,
            onShowScheduleWeek = viewModel::loadScheduleWeekAvailability,
            onScheduleDateChanged = viewModel::loadScheduleAvailability,
            onRetryScheduleAvailability = viewModel::retryScheduleAvailability,
            onUpdateSchedule = viewModel::updateSchedule,
        )
        is RequestDetailState.Error -> ErrorContent(
            message = s.message,
            onRetry = { viewModel.retry() },
        )
        is RequestDetailState.NotFound -> RequestDetailNotFoundContent(onBack = onBack)
    }
}

@Composable
private fun RequestBackIcon(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailLoadingContent(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        LoadingContent(modifier = Modifier.fillMaxSize().padding(padding))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailDataContent(
    state: RequestDetailState.Data,
    onBack: () -> Unit,
    onCancelClick: () -> Unit,
    onRefresh: () -> Unit,
    onMessageClick: () -> Unit,
    onEditScheduleClick: () -> Unit,
    onDismissScheduleSheet: () -> Unit,
    onShowScheduleWeek: (String) -> Unit,
    onScheduleDateChanged: (String) -> Unit,
    onRetryScheduleAvailability: () -> Unit,
    onUpdateSchedule: (String, List<String>) -> Unit,
) {
    val presentation = requestStatusPresentation(state.request.status)
    // Keep the request detail's type/duration presentation consistent with the current-journey
    // request surface.
    val durationMinutes = state.request.provisionalDurationMinutes
        ?: state.request.appointmentType?.durationMinutes
    val isRebooking = state.request.requestType == AppointmentRequestType.RESCHEDULE
    val heroTitle = if (isRebooking) {
        "Reschedule request"
    } else {
        listOfNotNull(
            state.request.appointmentType?.name,
            durationMinutes?.let { "$it min" },
        ).joinToString(" · ").ifBlank { "Appointment request" }
    }
    val showMessageAction = state.request.status == AppointmentRequestStatus.PENDING ||
        state.request.status == AppointmentRequestStatus.REJECTED
    val sameDayCancellationBlocked = state.request.status.isCancellable &&
        isSameDayInClinic(state.request.scheduledAt)
    val showCancel = presentation.showCancel && state.request.status.isCancellable &&
        !sameDayCancellationBlocked
    val showScheduleAction = state.request.status.isCancellable
    val showBottomBar = showMessageAction || showCancel || showScheduleAction

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
        bottomBar = {
            if (showBottomBar) {
                RequestDetailBottomBar(
                    showMessageAction = showMessageAction,
                    showCancel = showCancel,
                    showScheduleAction = showScheduleAction,
                    isCancelling = state.isCancelling,
                    isUpdatingSchedule = state.isUpdatingSchedule,
                    onMessageClick = onMessageClick,
                    onCancelClick = onCancelClick,
                    onEditScheduleClick = onEditScheduleClick,
                )
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.cancelError?.let { RequestActionError(it) }

                // Notice sits above the details card, mirroring AppointmentStatusGuidance on the
                // confirmed-appointment screen, rather than living inside the card as body text.
                RequestStatusNotice(
                    status = state.request.status,
                    presentation = presentation,
                    sameDayCancellationBlocked = sameDayCancellationBlocked,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // Keep the request number and title together as the card's visual anchor.
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Request ${state.request.requestNumber}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = heroTitle,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (isRebooking) {
                                    state.request.originalScheduledAt?.let { originalTime ->
                                        DetailMetadataRow(
                                            Icons.Outlined.CalendarMonth,
                                            "Current appointment",
                                            formatDetailDateTime(originalTime),
                                        )
                                    }
                                    DetailMetadataRow(
                                        Icons.Outlined.AccessTime,
                                        if (state.request.selectedScheduledAt != null) "Approved time" else "Requested new time",
                                        formatDetailDateTime(
                                            state.request.selectedScheduledAt ?: state.request.scheduledAt,
                                        ),
                                    )
                                } else {
                                    DetailMetadataRow(
                                        Icons.Outlined.CalendarMonth,
                                        "Date",
                                        formatDetailDate(state.request.scheduledAt),
                                    )
                                    DetailMetadataRow(
                                        Icons.Outlined.AccessTime,
                                        "Preferred time",
                                        formatDetailTime(state.request.scheduledAt),
                                    )
                                }
                                durationMinutes?.let {
                                    DetailMetadataRow(
                                        Icons.Outlined.AccessTime,
                                        "Duration",
                                        "$it min visit",
                                    )
                                }
                                state.request.reasonForVisit?.takeIf { it.isNotBlank() }?.let { reason ->
                                    DetailMetadataRow(
                                        Icons.Outlined.Info,
                                        "Reason for visit",
                                        reason,
                                    )
                                }
                            }
                        }

                        if (state.request.alternativeScheduledTimes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Alternative times",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                state.request.alternativeScheduledTimes.forEachIndexed { index, time ->
                                    DetailMetadataRow(
                                        icon = Icons.Outlined.AccessTime,
                                        label = "Option ${index + 1}",
                                        value = formatDetailDateTime(time),
                                    )
                                }
                            }
                        }

                        state.request.referringSource?.let {
                            DetailRow(label = "Referral source", value = it)
                        }

                        state.request.cancelledAt?.let {
                            DetailRow(label = "Cancelled", value = formatDetailDateTime(it))
                        }

                        state.request.cancellationReason
                            ?.takeIf { it.isNotBlank() }
                            ?.let { reason ->
                                DetailRow(label = "Cancellation reason", value = reason)
                            }

                        if (state.request.status == AppointmentRequestStatus.REJECTED) {
                            state.request.rejectionReason?.takeIf { it.isNotBlank() }?.let { reason ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "Clinic response",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
                                            )
                                            Text(
                                                text = reason,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onErrorContainer,
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
    }

    if (state.showScheduleSheet) {
        RescheduleBottomSheet(
            currentScheduledAt = state.request.scheduledAt,
            weekStart = state.scheduleWeekStart,
            dayAvailability = state.scheduleDayAvailability,
            availabilityState = state.scheduleAvailability,
            isSubmitting = state.isUpdatingSchedule,
            errorMessage = state.scheduleError,
            title = "Change requested time",
            description = "Choose a new preferred time from tomorrow onward. Your request stays pending until the clinic reviews it.",
            currentTimeLabel = "Current requested time",
            currentTimeDescription = "",
            confirmationTitle = "Update requested time",
            confirmationMessage = { _, _, _ ->
                "The clinic will review this request before confirming a new time."
            },
            confirmLabel = "Update request",
            dismissLabel = "Keep current",
            onShowWeek = onShowScheduleWeek,
            onDateChanged = onScheduleDateChanged,
            onRetryAvailability = onRetryScheduleAvailability,
            onDismiss = onDismissScheduleSheet,
            onConfirm = { scheduledAt, alternatives, _ ->
                onUpdateSchedule(scheduledAt, alternatives)
            },
        )
    }
}

@Composable
private fun RequestDetailBottomBar(
    showMessageAction: Boolean,
    showCancel: Boolean,
    showScheduleAction: Boolean,
    isCancelling: Boolean,
    isUpdatingSchedule: Boolean,
    onMessageClick: () -> Unit,
    onCancelClick: () -> Unit,
    onEditScheduleClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showScheduleAction) {
                AppointmentPrimaryButton(
                    text = "Change requested time",
                    onClick = onEditScheduleClick,
                    enabled = !isUpdatingSchedule,
                    loading = isUpdatingSchedule,
                    icon = Icons.Outlined.EditCalendar,
                )
            }
            if (showMessageAction) {
                AppointmentOutlinedButton(
                    text = "Message the clinic",
                    onClick = onMessageClick,
                    icon = Icons.AutoMirrored.Outlined.Chat,
                )
            }
            if (showCancel) {
                AppointmentOutlinedButton(
                    text = "Cancel request",
                    onClick = onCancelClick,
                    enabled = !isCancelling,
                    loading = isCancelling,
                    icon = Icons.Outlined.EventBusy,
                    isDestructive = true,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RequestDetailNotFoundContent(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        EmptyContent(
            message = "This request may belong to another account or has been removed.",
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@Composable
private fun RequestStatusNotice(
    status: AppointmentRequestStatus,
    presentation: RequestStatusPresentation,
    sameDayCancellationBlocked: Boolean = false,
) {
    val icon = when (status) {
        AppointmentRequestStatus.PENDING -> Icons.Outlined.AccessTime
        AppointmentRequestStatus.ACCEPTED -> Icons.Outlined.EventAvailable
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED,
        AppointmentRequestStatus.UNKNOWN -> Icons.Outlined.EventBusy
    }
    val containerColor = when (status) {
        AppointmentRequestStatus.PENDING -> EyecareColors.current.statusPending.copy(alpha = 0.16f)
        AppointmentRequestStatus.ACCEPTED -> MaterialTheme.colorScheme.tertiaryContainer
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED -> MaterialTheme.colorScheme.errorContainer
        AppointmentRequestStatus.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (status) {
        AppointmentRequestStatus.PENDING -> EyecareColors.current.statusPendingText
        AppointmentRequestStatus.ACCEPTED -> MaterialTheme.colorScheme.onTertiaryContainer
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED -> MaterialTheme.colorScheme.onErrorContainer
        AppointmentRequestStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = when (status) {
        AppointmentRequestStatus.PENDING -> EyecareColors.current.statusPendingText
        AppointmentRequestStatus.ACCEPTED -> EyecareColors.current.statusConfirmed
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED -> MaterialTheme.colorScheme.error
        AppointmentRequestStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val noticeTitle = if (sameDayCancellationBlocked) {
        "Same-day cancellation unavailable"
    } else {
        presentation.label
    }
    val noticeMessage = if (sameDayCancellationBlocked) {
        SAME_DAY_CANCELLATION_MESSAGE
    } else {
        presentation.description
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = iconTint,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = noticeTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
                Text(
                    text = noticeMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor,
                )
            }
        }
    }
}

@Composable
private fun RequestActionError(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun DetailMetadataRow(icon: ImageVector, label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = EyecareColors.current.accentText,
            modifier = Modifier.size(20.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatDetailDate(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"))
} catch (_: Exception) { iso }

private fun formatDetailTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
} catch (_: Exception) { iso }

private fun formatDetailDateTime(iso: String): String = try {
    java.time.Instant.parse(iso).atZone(CLINIC_TIME_ZONE)
        .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a"))
} catch (_: Exception) { iso }
