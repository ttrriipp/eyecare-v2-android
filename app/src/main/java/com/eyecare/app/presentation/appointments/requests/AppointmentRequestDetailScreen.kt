package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
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
    onViewConfirmedAppointment: (Int) -> Unit = {},
    onNavigateToMessages: () -> Unit = {},
    viewModel: AppointmentRequestDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(requestId, isLinked) {
        viewModel.setLinked(isLinked)
        viewModel.load(requestId)
    }

    val state by viewModel.state.collectAsState()
    var showCancelDialog by remember { mutableStateOf(false) }

    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel this request?",
            message = "This can't be undone. Your request will be cancelled.",
            confirmLabel = "Cancel request",
            dismissLabel = "Keep",
            onConfirm = {
                showCancelDialog = false
                viewModel.cancel()
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    when (val s = state) {
        is RequestDetailState.Loading -> RequestDetailLoadingContent(onBack = onBack)
        is RequestDetailState.Data -> RequestDetailDataContent(
            state = s,
            onBack = onBack,
            onCancelClick = { showCancelDialog = true },
            onViewConfirmed = { onViewConfirmedAppointment(it) },
            onRefresh = viewModel::refresh,
            onMessageClick = onNavigateToMessages,
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
    onViewConfirmed: (Int) -> Unit,
    onRefresh: () -> Unit,
    onMessageClick: () -> Unit,
) {
    val presentation = requestStatusPresentation(state.request.status)
    // Mirrors the list card's own type/duration join (AppointmentListScreen.kt) so the same
    // request reads identically on both surfaces.
    val durationMinutes = state.request.provisionalDurationMinutes
        ?: state.request.appointmentType?.durationMinutes
    val heroTitle = listOfNotNull(
        state.request.appointmentType?.name,
        durationMinutes?.let { "$it min" },
    ).joinToString(" · ").ifBlank { "Appointment request" }
    val showMessageAction = state.request.status == AppointmentRequestStatus.PENDING ||
        state.request.status == AppointmentRequestStatus.REJECTED
    val showCancel = presentation.showCancel && state.request.status.isCancellable
    val confirmedAppointmentId = state.request.appointmentId
        .takeIf { presentation.showViewConfirmed && state.isLinked }
    val showBottomBar = showMessageAction || showCancel || confirmedAppointmentId != null
    val bottomContentPadding = if (showBottomBar) 120.dp else 24.dp

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
                navigationIcon = { RequestBackIcon(onBack) },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
        // Mirrors AppointmentDetailScreen's own Box: scrollable content plus a fixed action
        // bar pinned to the bottom, instead of stacking action buttons into the scroll flow.
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.cancelError?.let { RequestActionError(it) }

                // Notice sits above the details card, mirroring AppointmentStatusGuidance on the
                // confirmed-appointment screen, rather than living inside the card as body text.
                RequestStatusNotice(state.request.status, presentation)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        // The hero lives inside the card, mirroring AppointmentDetailScreen's own
                        // Card header (appointment number + title + status badge) rather than
                        // sitting outside it as a separate page-level heading.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
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
                            AppointmentRequestStatusPill(state.request.status, presentation.label)
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
                                durationMinutes?.let {
                                    DetailMetadataRow(
                                        Icons.Outlined.AccessTime,
                                        "Duration",
                                        "$it min visit",
                                    )
                                }
                                DetailMetadataRow(
                                    Icons.Outlined.Info,
                                    "Reason for visit",
                                    state.request.reasonForVisit,
                                )
                            }
                        }

                        if (state.request.alternativeScheduledTimes.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Alternative times",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                state.request.alternativeScheduledTimes.forEachIndexed { index, time ->
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Alternative ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = formatDetailDateTime(time),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }
                        }

                        state.request.referringSource?.let {
                            DetailRow(label = "Referral source", value = it)
                        }

                        state.request.cancelledAt?.let {
                            DetailRow(label = "Cancelled", value = formatDetailDateTime(it))
                        }

                        if (state.request.status == AppointmentRequestStatus.REJECTED) {
                            state.request.rejectionReason?.takeIf { it.isNotBlank() }?.let { reason ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.errorContainer,
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = "Reason",
                                            style = MaterialTheme.typography.labelMedium,
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

            if (showBottomBar) {
                // Rounded top corners plus a hairline border read as an intentional sheet
                // lifted off the canvas (matching the app's border-before-shadow depth model)
                // instead of a flat rectangular slab with a hard shadow line cutting across it.
                Surface(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (confirmedAppointmentId != null) {
                            AppointmentPrimaryButton(
                                text = "View confirmed appointment",
                                onClick = { onViewConfirmed(confirmedAppointmentId) },
                                icon = Icons.Outlined.EventAvailable,
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
                                enabled = !state.isCancelling,
                                loading = state.isCancelling,
                                icon = Icons.Outlined.EventBusy,
                                isDestructive = true,
                            )
                        }
                    }
                }
            }
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
private fun RequestStatusNotice(status: AppointmentRequestStatus, presentation: RequestStatusPresentation) {
    val icon = when (status) {
        AppointmentRequestStatus.PENDING -> Icons.Outlined.AccessTime
        AppointmentRequestStatus.ACCEPTED -> Icons.Outlined.EventAvailable
        AppointmentRequestStatus.REJECTED,
        AppointmentRequestStatus.CANCELLED,
        AppointmentRequestStatus.EXPIRED,
        AppointmentRequestStatus.UNKNOWN -> Icons.Outlined.EventBusy
    }
    val isActive = status == AppointmentRequestStatus.PENDING || status == AppointmentRequestStatus.ACCEPTED

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
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
                tint = EyecareColors.current.accentText,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = presentation.label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = presentation.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
