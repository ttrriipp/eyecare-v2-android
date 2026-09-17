package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val CLINIC_ZONE = ZoneId.of("Asia/Manila")
private val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", Locale.US)
private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppointmentScreen(
    uiState: MyAppointmentUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onRequestAppointment: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToRequestDetail: (Int) -> Unit,
    onNavigateToAppointmentDetail: (Int) -> Unit,
    onRequestDifferentTime: () -> Unit = {},
    onCancelRequest: (Int, String) -> Unit,
    onCancelAppointment: (String) -> Unit,
    onClearMutationError: () -> Unit,
    onClearMutationSuccess: () -> Unit,
    hasActivePatientLink: Boolean = true,
    onNavigateToLinkAccount: () -> Unit = {},
    onDismissReschedule: () -> Unit = {},
    onShowRescheduleWeek: (String) -> Unit = {},
    onRescheduleDateChanged: (String) -> Unit = {},
    onRetryRescheduleAvailability: () -> Unit = {},
    onRescheduleAppointment: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        val content = uiState as? MyAppointmentUiState.Content ?: return@LaunchedEffect
        content.mutationError?.let { snackbarHostState.showSnackbar(it); onClearMutationError() }
        content.mutationSuccess?.let { snackbarHostState.showSnackbar(it); onClearMutationSuccess() }
    }

    val rescheduleContent = uiState as? MyAppointmentUiState.Content
    val rescheduleJourney = rescheduleContent?.journey as? CurrentAppointmentJourney.Appointment
    if (rescheduleContent?.showRescheduleSheet == true && rescheduleJourney != null) {
        RescheduleBottomSheet(
            currentScheduledAt = rescheduleJourney.appointment.scheduledAt,
            weekStart = rescheduleContent.rescheduleWeekStart,
            dayAvailability = rescheduleContent.rescheduleDayAvailability,
            availabilityState = rescheduleContent.rescheduleAvailability,
            isSubmitting = rescheduleContent.isRescheduling,
            errorMessage = rescheduleContent.rescheduleError,
            title = "Request a different time",
            description = "Choose a new preferred time from tomorrow onward. Your current appointment stays confirmed until the clinic approves the request.",
            confirmationTitle = "Request this time change",
            confirmationMessage = { date, time ->
                "Request a move to $date at $time? Your current appointment remains unchanged until the clinic approves it."
            },
            onShowWeek = onShowRescheduleWeek,
            onDateChanged = onRescheduleDateChanged,
            onRetryAvailability = onRetryRescheduleAvailability,
            onDismiss = onDismissReschedule,
            onConfirm = onRescheduleAppointment,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Appointment") },
                actions = {
                    if (hasActivePatientLink && uiState is MyAppointmentUiState.Content) {
                        IconButton(onClick = onNavigateToHistory) {
                            Icon(Icons.Default.History, contentDescription = "Appointment history")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        when (uiState) {
            is MyAppointmentUiState.Loading -> {
                MyAppointmentLoadingContent(modifier = Modifier.fillMaxSize().padding(padding))
            }
            is MyAppointmentUiState.Error -> {
                ErrorContent(
                    message = uiState.message,
                    onRetry = onRetry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
            is MyAppointmentUiState.Content -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        uiState.refreshError?.let { error ->
                            RefreshErrorBanner(
                                message = error,
                                onRetry = onRefresh,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        JourneyContent(
                            journey = uiState.journey,
                            isMutating = uiState.isMutating,
                            hasActivePatientLink = hasActivePatientLink,
                            onNavigateToLinkAccount = onNavigateToLinkAccount,
                            onRequestAppointment = onRequestAppointment,
                            onNavigateToRequestDetail = onNavigateToRequestDetail,
                            onNavigateToAppointmentDetail = onNavigateToAppointmentDetail,
                            onRequestDifferentTime = onRequestDifferentTime,
                            onCancelRequest = onCancelRequest,
                            onCancelAppointment = onCancelAppointment,
                            modifier = Modifier.fillMaxWidth().weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyContent(
    journey: CurrentAppointmentJourney,
    isMutating: Boolean,
    hasActivePatientLink: Boolean,
    onNavigateToLinkAccount: () -> Unit,
    onRequestAppointment: () -> Unit,
    onNavigateToRequestDetail: (Int) -> Unit,
    onNavigateToAppointmentDetail: (Int) -> Unit,
    onRequestDifferentTime: () -> Unit,
    onCancelRequest: (Int, String) -> Unit,
    onCancelAppointment: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        when (journey) {
            is CurrentAppointmentJourney.None -> {
                NoneContent(onRequestAppointment = onRequestAppointment)
            }
            is CurrentAppointmentJourney.PendingRequest -> {
                PendingRequestContent(
                    request = journey.request,
                    isMutating = isMutating,
                    onViewDetails = { onNavigateToRequestDetail(journey.request.id) },
                    onCancel = { reason -> onCancelRequest(journey.request.id, reason) },
                )
            }
            is CurrentAppointmentJourney.Appointment -> {
                ConfirmedAppointmentContent(
                    appointment = journey.appointment,
                    originalRequest = journey.originalRequest,
                    pendingReschedule = journey.pendingReschedule,
                    isMutating = isMutating,
                    hasActivePatientLink = hasActivePatientLink,
                    onNavigateToLinkAccount = onNavigateToLinkAccount,
                    onViewRequestDetail = onNavigateToRequestDetail,
                    onViewAppointmentDetail = { onNavigateToAppointmentDetail(journey.appointment.id) },
                    onRequestDifferentTime = onRequestDifferentTime,
                    onCancel = onCancelAppointment,
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun MyAppointmentLoadingContent(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlaceholderBar(widthFraction = 0.32f, height = 24.dp)
        PlaceholderBar(widthFraction = 0.72f, height = 32.dp)
        Surface(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ) {}
        PlaceholderBar(widthFraction = 0.9f, height = 18.dp)
        PlaceholderBar(widthFraction = 0.62f, height = 18.dp)
    }
}

@Composable
private fun PlaceholderBar(
    widthFraction: Float,
    height: androidx.compose.ui.unit.Dp,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(widthFraction).height(height),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {}
}

@Composable
private fun RefreshErrorBanner(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun LinkRequiredNotice(
    onLinkAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Connect your clinic record to manage this appointment.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "You can see the confirmed details here. Linking your account enables history and appointment changes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = onLinkAccount) {
                Text("Link your account")
            }
        }
    }
}

@Composable
private fun SameDayCancellationNotice(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Same-day cancellation unavailable",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                text = SAME_DAY_CANCELLATION_MESSAGE,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun NoneContent(
    onRequestAppointment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(48.dp))
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No active appointment",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You can request an appointment when you're ready.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestAppointment) {
            Text("Request an appointment")
        }
    }
}

@Composable
private fun PendingRequestContent(
    request: AppointmentRequest,
    isMutating: Boolean,
    onViewDetails: () -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val sameDayCancellationBlocked = isSameDayInClinic(request.scheduledAt)

    Column(modifier = modifier.fillMaxWidth()) {
        StatusHeader(
            statusLabel = "Pending",
            statusColor = MaterialTheme.colorScheme.tertiary,
        )
        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text(
                text = "Request ${request.requestNumber}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            request.appointmentType?.let { type ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = type.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "${type.durationMinutes} min visit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            DateTimeRow(
                label = "Requested time",
                dateTime = request.scheduledAt,
                isPrimary = true,
            )
            request.alternativeScheduledTimes.forEachIndexed { index, time ->
                Spacer(Modifier.height(8.dp))
                DateTimeRow(
                    label = "Alternative ${index + 1}",
                    dateTime = time,
                    isPrimary = false,
                )
            }
            if (request.reasonForVisit.isNullOrBlank().not()) {
                Spacer(Modifier.height(12.dp))
                DetailRow(label = "Reason", value = request.reasonForVisit.orEmpty())
            }
            request.referringSource?.let { ref ->
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Referral", value = ref)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Your preferred times are awaiting clinic approval.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Spacer(Modifier.height(16.dp))
        if (sameDayCancellationBlocked) {
            SameDayCancellationNotice()
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = onViewDetails,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isMutating,
        ) {
            Text("Change requested time")
        }
        if (!sameDayCancellationBlocked) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMutating,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel request")
            }
        }
    }

    if (showCancelDialog) {
        CancelReasonDialog(
            title = "Cancel request",
            onConfirm = { reason ->
                showCancelDialog = false
                onCancel(reason)
            },
            onDismiss = { showCancelDialog = false },
        )
    }
}

@Composable
private fun ConfirmedAppointmentContent(
    appointment: AppointmentV1,
    originalRequest: AppointmentRequest?,
    pendingReschedule: AppointmentRequest?,
    isMutating: Boolean,
    hasActivePatientLink: Boolean,
    onNavigateToLinkAccount: () -> Unit,
    onViewRequestDetail: (Int) -> Unit,
    onViewAppointmentDetail: () -> Unit,
    onRequestDifferentTime: () -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCancelDialog by remember { mutableStateOf(false) }
    val actionPolicy = appointmentCurrentActionPolicy(
        appointment = appointment,
        hasPendingReschedule = pendingReschedule != null,
    )
    val statusPresentation = appointmentStatusPresentation(appointment.status)

    Column(modifier = modifier.fillMaxWidth()) {
        StatusHeader(
            statusLabel = statusPresentation.label,
            statusColor = statusPresentation.textColor,
        )
        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text(
                text = appointment.appointmentType,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            appointment.appointmentNumber?.let { number ->
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(12.dp))
            DateTimeRow(
                label = "Date & time",
                dateTime = appointment.scheduledAt,
                isPrimary = true,
            )
            Spacer(Modifier.height(8.dp))
            DetailRow(label = "Duration", value = "${appointment.durationMinutes} min")
            appointment.reasonForVisit?.let { reason ->
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Reason", value = reason)
            }
            appointment.assignedOptometrist?.let { optometrist ->
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Optometrist", value = optometrist.name)
            }
            appointment.referringSource?.let { ref ->
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Referral", value = ref)
            }
            appointment.contactNotes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(Modifier.height(8.dp))
                DetailRow(label = "Your booking note", value = notes)
            }
            appointment.lastRescheduleReason?.let { reason ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Schedule changed by clinic",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (pendingReschedule != null) {
            Spacer(Modifier.height(16.dp))
            PendingRescheduleSection(
                request = pendingReschedule,
                onViewDetail = { onViewRequestDetail(pendingReschedule.id) },
            )
        }

        Spacer(Modifier.height(12.dp))

        if (hasActivePatientLink && actionPolicy.canRequestDifferentTime) {
            Button(
                onClick = onRequestDifferentTime,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMutating,
            ) {
                Text("Request a different time")
            }
            Spacer(Modifier.height(8.dp))
        }

        if (hasActivePatientLink) {
            OutlinedButton(
                onClick = onViewAppointmentDetail,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMutating,
            ) {
                Text("View appointment details")
            }
            Spacer(Modifier.height(8.dp))
        } else {
            LinkRequiredNotice(
                onLinkAccount = onNavigateToLinkAccount,
            )
            Spacer(Modifier.height(8.dp))
        }

        if (actionPolicy.sameDayCancellationBlocked) {
            SameDayCancellationNotice()
            Spacer(Modifier.height(8.dp))
        }

        if (hasActivePatientLink && actionPolicy.canCancel) {
            OutlinedButton(
                onClick = { showCancelDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isMutating,
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text("Cancel appointment")
            }
            Spacer(Modifier.height(8.dp))
        }

        originalRequest?.let { request ->
            TextButton(
                onClick = { onViewRequestDetail(request.id) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("View original request")
            }
        }
    }

    if (showCancelDialog) {
        CancelReasonDialog(
            title = "Cancel appointment",
            onConfirm = { reason ->
                showCancelDialog = false
                onCancel(reason)
            },
            onDismiss = { showCancelDialog = false },
        )
    }
}

@Composable
private fun PendingRescheduleSection(
    request: AppointmentRequest,
    onViewDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time-change request pending",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
            ) {
                Text(
                    text = "NOT YET CONFIRMED",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
            Spacer(Modifier.height(8.dp))
            DateTimeRow(
                label = "Requested time",
                dateTime = request.scheduledAt,
                isPrimary = true,
            )
            request.alternativeScheduledTimes.forEachIndexed { index, time ->
                Spacer(Modifier.height(6.dp))
                DateTimeRow(
                    label = "Alternative ${index + 1}",
                    dateTime = time,
                    isPrimary = false,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Request ${request.requestNumber}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onViewDetail) {
                Text("View request details")
            }
        }
    }
}

@Composable
private fun StatusHeader(
    statusLabel: String,
    statusColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = statusColor.copy(alpha = 0.12f),
        ) {
            Text(
                text = statusLabel.uppercase(Locale.US),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = statusColor,
            )
        }
    }
}

@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun DateTimeRow(
    label: String,
    dateTime: String,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(dateTime) { parseDisplayDateTime(dateTime) }
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = parsed?.date ?: dateTime.take(10),
                style = if (isPrimary) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                else MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = parsed?.time ?: "",
            style = if (isPrimary) MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            else MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Unable to load your appointment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@Composable
private fun CancelReasonDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var reason by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; error = null },
                    label = { Text("Reason for cancelling") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = error != null,
                    supportingText = error?.let { e -> { Text(e) } },
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Up to 1,000 characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Keep")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (reason.trim().isBlank()) {
                                error = "Enter a reason for cancelling."
                            } else if (reason.trim().length > PATIENT_CANCELLATION_REASON_MAX_LENGTH) {
                                error = "Reason must be 1,000 characters or less."
                            } else {
                                onConfirm(reason.trim())
                            }
                        },
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

private data class DisplayDateTime(val date: String, val time: String)

private fun parseDisplayDateTime(iso: String): DisplayDateTime? = runCatching {
    val zoned = OffsetDateTime.parse(iso).atZoneSameInstant(CLINIC_ZONE)
    DisplayDateTime(
        date = zoned.format(dateFormatter),
        time = zoned.format(timeFormatter),
    )
}.getOrNull()
