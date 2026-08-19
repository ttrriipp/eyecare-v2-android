package com.eyecare.app.presentation.appointments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.eyecare.app.ui.theme.EyecareTheme
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.appointments.components.VisitFeedbackDialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.presentation.common.buildImageUrl
import coil3.compose.AsyncImage
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    onBack: () -> Unit,
    onNavigateToReservations: () -> Unit = {},
    onOpenReservation: (Int) -> Unit = {},
    viewModel: AppointmentDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCancelDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showCancelDialog) {
        val appointment = (uiState as? AppointmentDetailUiState.Success)?.appointment
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel appointment?",
            message = appointment?.let { detail ->
                "Cancel this ${formatAppointmentTitle(detail.appointmentType)} appointment on " +
                    formatAppointmentDate(detail.scheduledAt) + " at " +
                    formatAppointmentTime(detail.scheduledAt) + "? This can't be undone."
            } ?: "This can't be undone.",
            confirmLabel = "Cancel appointment",
            dismissLabel = "Keep appointment",
            onConfirm = {
                showCancelDialog = false
                viewModel.cancelAppointment()
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    if (uiState is AppointmentDetailUiState.Success) {
        val state = uiState as AppointmentDetailUiState.Success
        if (state.showRescheduleSheet) {
            RescheduleBottomSheet(
                currentScheduledAt = state.appointment.scheduledAt,
                weekStart = state.rescheduleWeekStart,
                dayAvailability = state.rescheduleDayAvailability,
                availabilityState = state.rescheduleAvailability,
                isSubmitting = state.isRescheduling,
                errorMessage = state.rescheduleError,
                onShowWeek = viewModel::loadRescheduleWeekAvailability,
                onDateChanged = viewModel::loadRescheduleAvailability,
                onRetryAvailability = {
                    (state.rescheduleAvailability as? RescheduleAvailabilityState.Error)
                        ?.date
                        ?.let(viewModel::loadRescheduleAvailability)
                },
                onDismiss = viewModel::dismissRescheduleSheet,
                onConfirm = viewModel::rescheduleAppointment,
            )
        }
        if (state.showRescheduleSuccessDialog) {
            AppConfirmationDialog(
                icon = Icons.Outlined.EventAvailable,
                title = "Appointment Rescheduled",
                message = "Your appointment is now set for " +
                    "${formatAppointmentDate(state.appointment.scheduledAt)} at " +
                    "${formatAppointmentTime(state.appointment.scheduledAt)}.",
                confirmLabel = "Got it",
                onConfirm = viewModel::dismissRescheduleSuccessDialog,
                onDismissRequest = viewModel::dismissRescheduleSuccessDialog,
            )
        }
        if (state.showRatingDialog) {
            val existingRating = state.appointment.visitRating
            VisitFeedbackDialog(
                onSubmit = viewModel::submitRating,
                onDismiss = viewModel::dismissRatingDialog,
                initialRating = existingRating?.rating ?: 0,
                initialComment = existingRating?.comment ?: "",
                title = if (existingRating != null) "Update your rating" else "Rate your visit",
                isSubmitting = state.isSubmittingRating,
                errorMessage = state.ratingError,
            )
        }
        if (state.showRatingSuccessDialog) {
            AppConfirmationDialog(
                icon = Icons.Outlined.RateReview,
                title = "Thanks for your feedback",
                message = "Your rating helps the clinic and other patients.",
                confirmLabel = "Got it",
                onConfirm = viewModel::dismissRatingSuccessDialog,
                onDismissRequest = viewModel::dismissRatingSuccessDialog,
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Appointment Details") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (val state = uiState) {
            is AppointmentDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is AppointmentDetailUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is AppointmentDetailUiState.Success -> {
                AppointmentDetailContent(
                    state = state,
                    onReschedule = viewModel::showRescheduleSheet,
                    onCancel = { showCancelDialog = true },
                    onOpenReservation = onOpenReservation,
                    onRateVisit = viewModel::showRatingDialog,
                    onRetry = viewModel::refresh,
                )
            }
        }
    }
}

@Composable
private fun AppointmentDetailContent(
    state: AppointmentDetailUiState.Success,
    onReschedule: () -> Unit,
    onCancel: () -> Unit,
    onOpenReservation: (Int) -> Unit = {},
    onRateVisit: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    val appointment = state.appointment
    val canCancel = appointment.status.canCancel
    val canReschedule = appointment.status.canReschedule
    val customerNote = appointment.contactNotes?.takeIf { it.isNotBlank() }
    val clinicNote: String? = null
    val rescheduleReason = displayableRescheduleReason(appointment.lastRescheduleReason)
    val bottomContentPadding = when {
        canCancel || canReschedule -> 120.dp
        else -> 24.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = bottomContentPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            state.actionMessage?.let { AppointmentActionMessage(it) }
            state.rescheduleError?.let { AppointmentActionError(it) }
            state.cancelError?.let { AppointmentActionError(it) }
            AppointmentStatusGuidance(appointment.status, onRetry)

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            appointment.appointmentNumber?.let { number ->
                                Text(
                                    number,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatAppointmentTitle(appointment.appointmentType),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        AppointmentStatusPill(appointment.status)
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
                            AppointmentMetadataRow(
                                icon = Icons.Outlined.CalendarMonth,
                                label = "Date",
                                value = formatAppointmentDate(appointment.scheduledAt),
                            )
                            AppointmentMetadataRow(
                                icon = Icons.Outlined.AccessTime,
                                label = "Time",
                                value = formatAppointmentTime(appointment.scheduledAt),
                            )
                            AppointmentMetadataRow(
                                icon = Icons.Outlined.AccessTime,
                                label = "Duration",
                                value = "${appointment.durationMinutes} min visit",
                            )
                            appointment.reasonForVisit
                                ?.takeIf { it.isNotBlank() }
                                ?.let { reason ->
                                    AppointmentMetadataRow(
                                        icon = Icons.Outlined.Info,
                                        label = "Reason for visit",
                                        value = reason,
                                    )
                                }
                        }
                    }

                    appointment.assignedOptometrist?.let { optometrist ->
                        AppointmentMetadataRow(
                            icon = Icons.Outlined.Person,
                            label = "Optometrist",
                            value = optometrist.name,
                        )
                    }
                }
            }

            rescheduleReason?.let { StaffRescheduleNotice(it) }

            if (state.frameReservations.isNotEmpty()) {
                ReservedFramesSection(
                    reservations = state.frameReservations,
                    onOpenReservation = onOpenReservation,
                )
            }

            if (customerNote != null || clinicNote != null) {
                AppointmentNotesSection(
                    modifier = Modifier.padding(top = 4.dp),
                    customerNote = customerNote,
                    clinicNote = clinicNote,
                    canEditCustomerNote = false,
                    isEditing = false,
                    isSaving = false,
                    error = null,
                    onEdit = {},
                    onDismissEditor = {},
                    onSave = {},
                )
            }

            // Visit feedback section
            if (appointment.isRateable) {
                VisitFeedbackCard(
                    visitRating = appointment.visitRating,
                    onRateClick = onRateVisit,
                )
            }

        }

        if (canCancel || canReschedule) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (canReschedule) {
                        Button(
                            onClick = onReschedule,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !state.isCancelling && !state.isRescheduling,
                            shape = RoundedCornerShape(50),
                        ) {
                            Icon(
                                Icons.Outlined.EditCalendar,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("Reschedule", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (canCancel) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            enabled = !state.isCancelling && !state.isRescheduling,
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            if (state.isCancelling) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Outlined.EventBusy,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("Cancel appointment", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun displayableRescheduleReason(reason: String?): String? =
    reason?.trim()?.takeIf { it.isNotEmpty() }

@Composable
private fun StaffRescheduleNotice(reason: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.EditCalendar,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = EyecareColors.current.accentText,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Schedule changed by clinic",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VisitFeedbackCard(
    visitRating: VisitRating?,
    onRateClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visitRating != null) {
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${visitRating.rating} out of 5 stars"
                    },
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= visitRating.rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (star <= visitRating.rating) EyecareColors.current.accentText
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                visitRating.comment?.takeIf { it.isNotBlank() }?.let { comment ->
                    Text(
                        text = comment,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                visitRating.createdAt?.takeIf { it.isNotBlank() }?.let { createdAt ->
                    Text(
                        text = "Rated on ${formatAppointmentDate(createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onRateClick) {
                    Text("Update rating")
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = EyecareColors.current.accentText,
                    )
                    Text(
                        "Rate your visit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onRateClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(50),
                ) {
                    Text("Rate now")
                }
            }
        }
    }
}

@Composable
private fun AppointmentActionMessage(message: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = EyecareColors.current.accentText,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

private data class AppointmentStatusGuidanceCopy(
    val title: String,
    val message: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

@Composable
private fun AppointmentStatusGuidance(
    status: AppointmentStatus,
    onRetry: () -> Unit,
) {
    val copy = when (status) {
        AppointmentStatus.SCHEDULED -> AppointmentStatusGuidanceCopy(
            title = "Appointment scheduled",
            message = "Arrive a few minutes early. Bring any details the clinic asked for.",
            icon = Icons.Outlined.EventAvailable,
        )
        AppointmentStatus.CHECKED_IN -> AppointmentStatusGuidanceCopy(
            title = "Checked in",
            message = "You're checked in. The clinic will see you shortly.",
            icon = Icons.Outlined.EventAvailable,
        )
        AppointmentStatus.FULFILLED -> AppointmentStatusGuidanceCopy(
            title = "Visit completed",
            message = "This appointment is complete. You can find the record in History.",
            icon = Icons.Outlined.EventAvailable,
        )
        AppointmentStatus.CANCELLED -> AppointmentStatusGuidanceCopy(
            title = "Appointment cancelled",
            message = "This appointment is no longer active. You can book a new visit when you're ready.",
            icon = Icons.Outlined.EventBusy,
        )
        AppointmentStatus.NO_SHOW -> AppointmentStatusGuidanceCopy(
            title = "Missed appointment",
            message = "This visit was marked as missed. Contact the clinic if you need help booking again.",
            icon = Icons.Outlined.EventBusy,
        )
        AppointmentStatus.UNKNOWN -> AppointmentStatusGuidanceCopy(
            title = "Status unavailable",
            message = "We couldn't confirm the latest status. Refresh to try again.",
            icon = Icons.Outlined.EventBusy,
        )
    }
    val isActive = status == AppointmentStatus.SCHEDULED || status == AppointmentStatus.CHECKED_IN

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
                imageVector = copy.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = EyecareColors.current.accentText,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = copy.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = copy.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status == AppointmentStatus.UNKNOWN) {
                    TextButton(
                        onClick = onRetry,
                        modifier = Modifier.padding(start = 0.dp),
                    ) {
                        Text("Refresh")
                    }
                }
            }
        }
    }
}
@Composable
private fun ReservedFramesSection(
    reservations: List<FrameReservation>,
    onOpenReservation: (Int) -> Unit,
) {
    val reservation = reservations.firstOrNull() ?: return

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            tint = EyecareColors.current.accentText,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(
                    text = "Reserved Frames",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            reservation.items.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val imageUrl = item.images.firstOrNull()?.let(::buildImageUrl)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp),
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = item.frameName,
                                modifier = Modifier.padding(6.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = EyecareColors.current.accentText,
                                )
                            }
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.frameName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${item.frameBrand} · ${item.variantName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            TextButton(
                onClick = { onOpenReservation(reservation.id) },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = "View reservation",
                    style = MaterialTheme.typography.labelMedium,
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun AppointmentMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = EyecareColors.current.accentText,
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
private fun AppointmentNotesSection(
    modifier: Modifier = Modifier,
    customerNote: String?,
    clinicNote: String?,
    canEditCustomerNote: Boolean,
    isEditing: Boolean,
    isSaving: Boolean,
    error: String?,
    onEdit: () -> Unit,
    onDismissEditor: () -> Unit,
    onSave: (String) -> Unit,
) {
    var draft by remember(customerNote, isEditing) { mutableStateOf(customerNote.orEmpty()) }
    val normalizedDraft = draft.trim().ifBlank { null }
    val normalizedOriginal = customerNote?.trim()?.ifBlank { null }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isEditing) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= CONTACT_NOTE_MAX_LENGTH) draft = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                    label = { Text("Your booking note") },
                    placeholder = { Text("Anything the clinic should know?") },
                    minLines = 3,
                    maxLines = 6,
                    supportingText = {
                        Text("${draft.length}/$CONTACT_NOTE_MAX_LENGTH")
                    },
                )
                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismissEditor,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving,
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onSave(draft) },
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving && normalizedDraft != normalizedOriginal,
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text("Save")
                        }
                    }
                }
            } else if (canEditCustomerNote) {
                EditableCustomerNote(note = customerNote, onEdit = onEdit)
            } else if (customerNote != null) {
                AppointmentNoteItem(label = "Your booking note", note = customerNote)
            }
            if ((customerNote != null || isEditing || canEditCustomerNote) && clinicNote != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            clinicNote?.let { note ->
                AppointmentNoteItem(label = "Clinic note", note = note)
            }
        }
    }
}

private const val CONTACT_NOTE_MAX_LENGTH = 1000

@Composable
private fun EditableCustomerNote(note: String?, onEdit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "Your booking note",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = note ?: "Add details for the clinic",
                style = MaterialTheme.typography.bodyMedium,
                color = if (note == null) {
                    EyecareColors.current.accentText
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Outlined.Edit,
                contentDescription = if (note == null) "Add appointment note" else "Edit appointment note",
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppointmentNoteItem(label: String, note: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun AppointmentActionError(message: String) {
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
@Preview(showBackground = true)
@Composable
private fun AppointmentDetailPreview() {
    EyecareTheme {
        AppointmentDetailScreen(onBack = {})
    }
}


