package com.eyecare.app.presentation.reservations

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
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
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.isCancellable
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameReservationDetailScreen(
    onBack: () -> Unit,
    onViewAppointment: (Int) -> Unit,
    onViewFrame: (Int) -> Unit,
    viewModel: FrameReservationDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onCancel = viewModel::cancelReservation,
                onDismissCancelError = viewModel::dismissCancelError,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
fun ReservationDetailContent(
    state: ReservationDetailUiState.Success,
    onViewAppointment: (Int) -> Unit,
    onViewFrame: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismissCancelError: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val reservation = state.reservation
    var showCancelDialog by remember { mutableStateOf(false) }

    // Fires once, only on a genuine REQUESTED/PREPARED -> CANCELLED transition during this
    // session — not when the screen simply loads an already-cancelled reservation.
    val previousStatus = remember { mutableStateOf(reservation.status) }
    var showCancelledBanner by remember { mutableStateOf(false) }
    LaunchedEffect(reservation.status) {
        if (previousStatus.value != ReservationStatus.CANCELLED && reservation.status == ReservationStatus.CANCELLED) {
            showCancelledBanner = true
            delay(3000)
            showCancelledBanner = false
        }
        previousStatus.value = reservation.status
    }

    if (showCancelDialog) {
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
                showCancelDialog = false
                onCancel()
            },
            onDismissRequest = { showCancelDialog = false },
        )
    }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = showCancelledBanner, enter = fadeIn(), exit = fadeOut()) {
            CancelledConfirmationBanner()
        }

        ReservationSummaryCard(reservation)

        reservation.expiresAt?.let { expiresAt ->
            HoldNotice(expiresAt = expiresAt, status = reservation.status)
        }

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
                ReservedFrameCard(item = item, onViewFrame = { onViewFrame(item.frameId) })
            }
        }

        state.cancelError?.let { error ->
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
                TextButton(onClick = onDismissCancelError) { Text("Dismiss") }
            }
        }

        if (reservation.isCancellable) {
            OutlinedButton(
                onClick = { showCancelDialog = true },
                enabled = !state.isCancelling,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
            ) {
                if (state.isCancelling) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                } else {
                    Icon(Icons.Outlined.Cancel, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel reservation")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ReservationSummaryCard(reservation: FrameReservation) {
    DetailCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(Icons.Outlined.Inventory2)
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Reservation #${reservation.id}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Requested ${formatReservationDate(reservation.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(reservation.isHeld)
        }

        Text(
            text = reservationExplanation(reservation.isHeld, reservation.expiresAt),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CancelledConfirmationBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = "Reservation cancelled",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun HoldNotice(expiresAt: String, status: ReservationStatus) {
    val active = status == ReservationStatus.PREPARED
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EyecareColors.current.statusPending.copy(alpha = 0.10f),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Schedule,
                contentDescription = null,
                tint = EyecareColors.current.statusPending,
                modifier = Modifier.size(20.dp),
            )
            Column {
                Text(
                    text = if (active) "Held until ${formatReservationDateTime(expiresAt)}" else "Hold ended ${formatReservationDateTime(expiresAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                if (active) {
                    Text(
                        text = "Frames go back to the display after this time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
) {
    DetailCard(contentPadding = 0.dp) {
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
    val label = reservationChipLabel(isHeld)
    val color = if (isHeld) EyecareColors.current.statusConfirmed else EyecareColors.current.statusPending
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = color,
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
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            colors = colors,
            border = border,
            content = body,
        )
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
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
