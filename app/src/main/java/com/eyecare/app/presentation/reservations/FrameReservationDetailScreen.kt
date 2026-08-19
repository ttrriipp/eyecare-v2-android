package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.canAddItems
import com.eyecare.app.domain.model.canRemoveItems
import com.eyecare.app.domain.model.isCancellable
import com.eyecare.app.presentation.appointments.components.AppointmentOutlinedButton
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameReservationDetailScreen(
    onBack: () -> Unit,
    onViewAppointment: (Int) -> Unit,
    onViewFrame: (Int) -> Unit,
    onAddFrame: () -> Unit,
    viewModel: FrameReservationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Consume the terminal Deleted state exactly once — navigate back.
    LaunchedEffect(uiState) {
        if (uiState is ReservationDetailUiState.Deleted) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Reservation", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is ReservationDetailUiState.Loading -> LoadingContent(
                Modifier.fillMaxSize().padding(padding),
            )

            is ReservationDetailUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = viewModel::retry,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is ReservationDetailUiState.NotFound -> EmptyContent(
                message = "This reservation isn't available anymore.",
                actionLabel = "Back to reservations",
                onAction = onBack,
                modifier = Modifier.fillMaxSize().padding(padding),
            )

            is ReservationDetailUiState.Success -> ReservationDetailContent(
                state = state,
                onViewAppointment = onViewAppointment,
                onViewFrame = onViewFrame,
                onDelete = viewModel::deleteReservation,
                onRemoveItem = viewModel::removeItem,
                onAddFrame = onAddFrame,
                onDismissDeleteError = viewModel::dismissDeleteError,
                onDismissRemoveItemError = viewModel::dismissRemoveItemError,
                modifier = Modifier.padding(padding),
            )

            // Terminal state — LaunchedEffect navigates back; nothing to render.
            is ReservationDetailUiState.Deleted -> Unit
        }
    }
}

@Composable
fun ReservationDetailContent(
    state: ReservationDetailUiState.Success,
    onViewAppointment: (Int) -> Unit,
    onViewFrame: (Int) -> Unit,
    onDelete: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDismissDeleteError: () -> Unit,
    onDismissRemoveItemError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reservation = state.reservation
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pendingRemoveItem by remember { mutableStateOf<FrameReservationItem?>(null) }

    if (showDeleteDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.Cancel,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Cancel reservation?",
            message = "The clinic will stop holding " +
                if (reservation.items.size == 1) "this frame for your visit." else "these frames for your visit.",
            confirmLabel = "Cancel reservation",
            dismissLabel = "Keep reservation",
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismissRequest = { showDeleteDialog = false },
        )
    }

    pendingRemoveItem?.let { item ->
        AppConfirmationDialog(
            icon = Icons.Outlined.Cancel,
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            title = "Remove frame?",
            message = if (reservation.items.size == 1) {
                "Remove ${item.frameName} from this reservation? This will also cancel the reservation."
            } else {
                "Remove ${item.frameName} from this reservation?"
            },
            confirmLabel = "Remove frame",
            dismissLabel = "Keep frame",
            onConfirm = {
                pendingRemoveItem = null
                onRemoveItem(item.id)
            },
            onDismissRequest = { pendingRemoveItem = null },
        )
    }

    val showBottomBar = reservation.canAddItems || reservation.isCancellable
    var bottomActionBarHeightPx by remember { mutableIntStateOf(0) }
    val bottomActionBarHeight = with(LocalDensity.current) { bottomActionBarHeightPx.toDp() }

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Always visible, mirroring the status-guidance banner on the appointment, request,
            // and order detail screens - unlike the old HoldNotice, which only appeared when
            // expiresAt was set and always painted itself "confirmed" green regardless of
            // whether the reservation was actually held.
            ReservationStatusGuidance(reservation)

            ReservationSummaryCard(reservation)

            ReservationAppointmentCard(
                reservation = reservation,
                onViewAppointment = onViewAppointment,
            )

            if (reservation.items.isNotEmpty()) {
                Text(
                    text = if (reservation.items.size == 1) "Reserved frame" else "Reserved frames",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp),
                )

                reservation.items.forEach { item ->
                    ReservedFrameCard(
                        item = item,
                        onViewFrame = { onViewFrame(item.frameId) },
                        showRemove = reservation.canRemoveItems,
                        isRemoving = state.removingItemId == item.id,
                        onRemove = { pendingRemoveItem = item },
                    )
                }
            }

            if (!reservation.canAddItems && reservation.isHeld) {
                Text(
                    text = "The clinic has already set these frames aside. Ask at your visit to make changes.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.removeItemError?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onDismissRemoveItemError) { Text("Dismiss") }
                }
            }

            state.deleteError?.let { error ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = onDismissDeleteError) { Text("Dismiss") }
                }
            }

            Spacer(
                Modifier
                    .testTag("reservation-bottom-spacer")
                    .height(if (showBottomBar) bottomActionBarHeight + 16.dp else 24.dp),
            )
        }

        if (showBottomBar) {
            // Keep the action surface lifted from the content while measuring its real height so
            // the scroll column can reserve exactly enough space for this variable action set.
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomActionBarHeightPx = it.height },
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reservation-action-bar"),
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (reservation.canAddItems) {
                            AppointmentPrimaryButton(
                                text = "Add frame",
                                onClick = onAddFrame,
                                icon = Icons.Outlined.Inventory2,
                            )
                        }
                        if (reservation.isCancellable) {
                            AppointmentOutlinedButton(
                                text = "Cancel reservation",
                                onClick = { showDeleteDialog = true },
                                enabled = !state.isDeleting,
                                loading = state.isDeleting,
                                icon = Icons.Outlined.Cancel,
                                isDestructive = true,
                            )
                        }
                    }
                }

                // Keep the gesture area on the same surface without drawing the action sheet's
                // outline or shadow directly against the system gesture handle.
                Spacer(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun ReservationSummaryCard(reservation: FrameReservation) {
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
                Text(
                    text = "Reservation #${reservation.id}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                StatusPill(reservation.isHeld)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DetailFactRow("Requested", formatReservationDate(reservation.createdAt))
                    reservation.expiresAt?.let { expiresAt ->
                        DetailFactRow(
                            if (reservation.isHeld) "Set aside until" else "Expected by",
                            formatReservationDateTime(expiresAt),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationStatusGuidance(reservation: FrameReservation) {
    val isHeld = reservation.isHeld
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = reservationStatusColor(isHeld).copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isHeld) Icons.Outlined.EventAvailable else Icons.Outlined.Schedule,
                contentDescription = null,
                tint = reservationStatusTextColor(isHeld),
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = reservationExplanation(isHeld, reservation.expiresAt),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ReservationAppointmentCard(
    reservation: FrameReservation,
    onViewAppointment: (Int) -> Unit,
) {
    val appointment = reservation.appointment
    val hasAppointment = appointment.id > 0

    DetailCard(
        onClick = if (hasAppointment) ({ onViewAppointment(appointment.id) }) else null,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Outlined.CalendarMonth)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "For your visit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = appointment.appointmentNumber
                        ?: if (hasAppointment) "Appointment #${appointment.id}" else "Appointment unavailable",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                if (appointment.scheduledAt.isNotBlank()) {
                    Text(
                        text = formatReservationSchedule(appointment.scheduledAt, appointment.durationMinutes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (hasAppointment) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReservedFrameCard(
    item: FrameReservationItem,
    onViewFrame: () -> Unit,
    showRemove: Boolean = false,
    isRemoving: Boolean = false,
    onRemove: () -> Unit = {},
) {
    DetailCard(
        modifier = Modifier.testTag("reservation-frame-card-${item.id}"),
        contentPadding = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrameThumbnail(item)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (item.frameBrand.isNotBlank()) {
                    Text(
                        text = item.frameBrand.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = item.frameName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = formatReservationPrice(item.price),
                        style = MaterialTheme.typography.bodyMedium,
                        color = EyecareColors.current.accentText,
                        fontWeight = FontWeight.SemiBold,
                    )
                    item.compareAtPrice?.let { original ->
                        Text(
                            text = formatReservationPrice(original),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                        )
                    }
                }
            }

            if (showRemove) {
                IconButton(onClick = onRemove, enabled = !isRemoving) {
                    if (isRemoving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            Icons.Outlined.Cancel,
                            contentDescription = "Remove ${item.frameName}",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            } else {
                IconButton(onClick = onViewFrame) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = "View ${item.frameName} details",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameThumbnail(item: FrameReservationItem) {
    val images = item.images
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(80.dp),
    ) {
        if (images.isEmpty()) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Outlined.FaceRetouchingNatural,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = EyecareColors.current.accentText,
                )
            }
        } else {
            AsyncImage(
                model = buildImageUrl(images.first()),
                contentDescription = item.frameName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.padding(8.dp),
            )
        }
    }
}

@Composable
private fun StatusPill(isHeld: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = reservationStatusColor(isHeld).copy(alpha = 0.12f)) {
        Text(
            text = reservationChipLabel(isHeld),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = reservationStatusTextColor(isHeld),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EyecareColors.current.accentText,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: androidx.compose.ui.unit.Dp = 16.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    val body: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            content = body,
        )
    } else {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            content = body,
        )
    }
}

@Composable
private fun DetailFactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "$label: $value" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
