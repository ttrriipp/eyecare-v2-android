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
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.presentation.common.buildImageUrl
import coil3.compose.AsyncImage
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    onBack: () -> Unit,
    onNavigateToReservations: () -> Unit = {},
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
        AppConfirmationDialog(
            icon = Icons.Outlined.EventBusy,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel appointment?",
            message = "This can't be undone.",
            confirmLabel = "Cancel",
            dismissLabel = "Keep",
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
                isSubmitting = state.isRescheduling,
                errorMessage = state.rescheduleError,
                onSelectionChanged = viewModel::clearRescheduleError,
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
                    onNavigateToReservations = onNavigateToReservations,
                    onRateVisit = viewModel::showRatingDialog,
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
    onNavigateToReservations: () -> Unit,
    onRateVisit: () -> Unit = {},
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
            AppointmentStatusGuidance(appointment.status)

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
                        AppointmentDetailStatusBadge(appointment.status)
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
                    onViewDetails = onNavigateToReservations,
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

            state.rescheduleError?.let { AppointmentActionError(it) }
            state.cancelError?.let { AppointmentActionError(it) }

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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visitRating != null) {
                // Show existing rating
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    (1..5).forEach { star ->
                        Icon(
                            imageVector = if (star <= visitRating.rating) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (star <= visitRating.rating) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if ((visitRating.revisionNumber ?: 1) > 1) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Edited",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                TextButton(onClick = onRateClick) {
                    Text("Update rating")
                }
            } else {
                // Unrated — show prompt
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.RateReview,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "Rate your visit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onRateClick,
                    shape = RoundedCornerShape(50),
                ) {
                    Text("Rate your visit")
                }
            }
        }
    }
}

@Composable
private fun AppointmentStatusGuidance(status: AppointmentStatus) {
    val (title, message) = when (status) {
        AppointmentStatus.SCHEDULED ->
            "Appointment Scheduled" to "Please arrive a few minutes before your scheduled time."
        AppointmentStatus.CHECKED_IN ->
            "Checked in" to "You're checked in. The clinic will see you shortly."
        else -> return
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.EventAvailable,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = EyecareColors.current.accentText,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReservedFramesSection(
    reservations: List<FrameReservation>,
    onViewDetails: () -> Unit,
) {
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
                Column {
                    Text(
                        text = "Reserved Frames",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${reservations.size} reservation${if (reservations.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            reservations.forEachIndexed { index, reservation ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Reservation #${reservation.id}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        val statusLabel = reservation.status.name
                            .replace("_", " ")
                            .lowercase()
                            .replaceFirstChar { it.titlecase() }
                        val reservationColors = EyecareColors.current
                        val statusColor = when (reservation.status) {
                            ReservationStatus.PREPARED, ReservationStatus.TRIED_ON -> reservationColors.statusConfirmed
                            ReservationStatus.REQUESTED -> reservationColors.statusPending
                            ReservationStatus.CONVERTED -> reservationColors.statusInfo
                            ReservationStatus.RELEASED, ReservationStatus.CANCELLED -> reservationColors.statusCancelled
                            ReservationStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = statusColor.copy(alpha = 0.12f),
                        ) {
                            Text(
                                text = statusLabel,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                            )
                        }
                    }
                    reservation.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val imageUrl = item.images.firstOrNull()?.let(::buildImageUrl)
                            if (imageUrl != null) {
                                AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop,
                                )
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
                }
            }

            TextButton(
                onClick = onViewDetails,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
            ) {
                Text("View all reservations", style = MaterialTheme.typography.labelMedium)
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
private fun AppointmentDetailStatusBadge(status: AppointmentStatus) {
    val colors = EyecareColors.current
    val (label, color) = when (status) {
        AppointmentStatus.SCHEDULED -> "Scheduled" to colors.statusPending
        AppointmentStatus.CHECKED_IN -> "Checked in" to colors.statusInfo
        AppointmentStatus.FULFILLED -> "Completed" to MaterialTheme.colorScheme.onSurfaceVariant
        AppointmentStatus.CANCELLED -> "Cancelled" to colors.statusCancelled
        AppointmentStatus.NO_SHOW -> "No show" to colors.statusCancelled
        AppointmentStatus.UNKNOWN -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
private fun AppointmentActionError(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            message,
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


