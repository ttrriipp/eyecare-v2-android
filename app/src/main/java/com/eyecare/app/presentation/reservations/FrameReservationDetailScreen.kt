package com.eyecare.app.presentation.reservations

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Button
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationItem
import com.eyecare.app.domain.model.ReservationStatus
import com.eyecare.app.domain.model.isCancellable
import com.eyecare.app.domain.model.totalValue
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
    modifier: Modifier = Modifier,
) {
    val reservation = state.reservation
    var showCancelDialog by remember { mutableStateOf(false) }

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
        ReservationSummaryCard(reservation)

        if (reservation.status != ReservationStatus.UNKNOWN) {
            ReservationProgressCard(reservation.status)
        }

        reservation.expiresAt?.let { expiresAt ->
            HoldNotice(expiresAt = expiresAt, status = reservation.status)
        }

        ReservationAppointmentCard(
            reservation = reservation,
            onViewAppointment = onViewAppointment,
        )

        Text(
            text = if (reservation.items.size == 1) "Reserved frame" else "Reserved frames",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp),
        )

        reservation.items.forEach { item ->
            ReservedFrameCard(item = item, onViewFrame = { onViewFrame(item.frameId) })
        }

        if (reservation.items.isNotEmpty()) {
            ReservationValueCard(reservation)
        }

        state.cancelError?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
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
            StatusPill(reservation.status)
        }

        Text(
            text = reservationStatusExplanation(reservation.status),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Requested -> Prepared -> Tried on. Terminal states replace the tracker with their own notice. */
@Composable
private fun ReservationProgressCard(status: ReservationStatus) {
    val terminalMessage = when (status) {
        ReservationStatus.CONVERTED -> "Converted into an eyewear order"
        ReservationStatus.RELEASED -> "Hold released — frames returned to the display"
        ReservationStatus.CANCELLED -> "Cancelled — no frames are being held"
        else -> null
    }

    DetailCard {
        if (terminalMessage != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (status == ReservationStatus.CONVERTED) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                    contentDescription = null,
                    tint = reservationStatusColor(status),
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = terminalMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            return@DetailCard
        }

        val steps = listOf(
            ReservationStatus.REQUESTED to "Requested",
            ReservationStatus.PREPARED to "Prepared",
            ReservationStatus.TRIED_ON to "Tried on",
        )
        val reachedIndex = steps.indexOfFirst { it.first == status }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            steps.forEachIndexed { index, (_, label) ->
                val reached = index <= reachedIndex
                val stepColor = if (reached) EyecareColors.current.accentText
                else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (reached) stepColor else stepColor.copy(alpha = 0.24f)),
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = stepColor,
                        fontWeight = if (index == reachedIndex) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
                if (index < steps.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .padding(horizontal = 8.dp)
                            .background(
                                if (index < reachedIndex) EyecareColors.current.accentText
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
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
        FrameImages(item)

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                if (item.frameBrand.isNotBlank()) {
                    Text(
                        text = item.frameBrand.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp,
                    )
                }
                Text(
                    text = item.frameName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = "Price",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatReservationPrice(item.price),
                        style = MaterialTheme.typography.titleLarge,
                        color = EyecareColors.current.accentText,
                        fontWeight = FontWeight.Bold,
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

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            DetailFactRow("Option", item.variantName)
            DetailFactRow("SKU", item.variantSku)
            if (item.frameCategory.isNotBlank()) {
                DetailFactRow("Category", item.frameCategory)
            }

            item.frameDescription?.takeIf(String::isNotBlank)?.let { description ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "About",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val cleanDescription = remember(description) {
                    HtmlCompat.fromHtml(description, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                }
                Text(
                    text = cleanDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            item.attributes?.filterValues(String::isNotBlank)?.takeIf { it.isNotEmpty() }?.let { specs ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Text(
                    text = "Specifications",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                specs.forEach { (key, value) -> DetailFactRow(key.toSpecLabel(), value) }
            }

            Button(
                onClick = onViewFrame,
                modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
                elevation = null,
            ) {
                Text("View frame details")
            }
        }
    }
}

@Composable
private fun FrameImages(item: FrameReservationItem) {
    val images = item.images
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        if (images.isEmpty()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        imageVector = Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        modifier = Modifier.padding(16.dp).size(28.dp),
                        tint = EyecareColors.current.accentText,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Photo coming soon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { images.size })
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                AsyncImage(
                    model = buildImageUrl(images[page]),
                    contentDescription = "${item.frameName} image ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                )
            }
            if (images.size > 1) {
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    repeat(images.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                    else Color.White.copy(alpha = 0.6f),
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationValueCard(reservation: FrameReservation) {
    DetailCard {
        DetailFactRow(
            label = if (reservation.items.size == 1) "1 frame reserved" else "${reservation.items.size} frames reserved",
            value = formatReservationPrice(reservation.totalValue),
        )
        Text(
            text = "Reserving holds stock only — nothing is charged. The clinic prepares an estimate after your fitting.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusPill(status: ReservationStatus) {
    val color = reservationStatusColor(status)
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.12f)) {
        Text(
            text = reservationStatusLabel(status),
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

private fun String.toSpecLabel(): String = replace('_', ' ')
    .replace('-', ' ')
    .trim()
    .split(" ")
    .filter(String::isNotBlank)
    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }
