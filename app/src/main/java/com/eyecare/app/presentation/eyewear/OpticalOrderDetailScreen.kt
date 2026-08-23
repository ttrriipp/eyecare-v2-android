package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.common.FeatureFlags
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalOrderDetailScreen(
    onBack: () -> Unit,
    onNavigateToMessages: () -> Unit = {},
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
    viewModel: OpticalOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var ratingItemId by remember { mutableStateOf<Int?>(null) }

    val successState = uiState as? OpticalOrderDetailUiState.Success
    val ratingTargetItem = ratingItemId?.let { id -> successState?.order?.items?.firstOrNull { it.id == id } }
    if (ratingTargetItem != null) {
        key(ratingTargetItem.id) {
            val ratingViewModel = hiltViewModel<FrameRatingViewModel, FrameRatingViewModel.Factory> {
                it.create(ratingTargetItem.id)
            }
            val ratingState by ratingViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(ratingState) {
                val success = ratingState as? FrameRatingUiState.Success ?: return@LaunchedEffect
                viewModel.updateItemRating(ratingTargetItem.id, success.result)
                ratingItemId = null
            }
            FrameRatingDialog(
                currentRating = ratingTargetItem.rating?.rating,
                currentComment = ratingTargetItem.rating?.comment,
                isSubmitting = ratingState is FrameRatingUiState.Submitting,
                errorMessage = (ratingState as? FrameRatingUiState.Error)?.message,
                onSubmit = ratingViewModel::submitRating,
                onDismiss = { ratingItemId = null },
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Eyewear Order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            is OpticalOrderDetailUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            is OpticalOrderDetailUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
            ) { ErrorContent(message = state.message, onRetry = viewModel::retry) }

            is OpticalOrderDetailUiState.Success -> OrderDetailContent(
                order = state.order,
                onRateItem = { itemId -> ratingItemId = itemId },
                onNavigateToMessages = onNavigateToMessages,
                ratingsEnabled = ratingsEnabled,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OpticalOrder,
    onRateItem: (Int) -> Unit,
    onNavigateToMessages: () -> Unit,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
    modifier: Modifier = Modifier,
) {
    val balanceDue = order.paymentSummary?.balanceDue?.takeIf { it > BigDecimal.ZERO }
    val showBottomBar = balanceDue != null

    Box(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (showBottomBar) 104.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OrderStatusGuidance(order.status)

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
                            orderCardTitle(order),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        // Shared with OpticalOrderListScreen.kt's OrderCard so the same order never
                        // renders in contradictory colors between the list and its own detail screen.
                        val statusColor = orderStatusColor(order.status)
                        Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
                            Text(
                                orderStatusLabel(order.status),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = orderStatusTextColor(order.status),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Only the reference number plus the current stage - the tracker
                            // below already shows the full prep/ready/released progression, so
                            // listing every past timestamp here would just repeat it as text.
                            DetailInfoRow("Reference", order.orderNumber)
                            val (stageLabel, stageValue) = orderDateLabelFull(order)
                            DetailInfoRow(stageLabel, stageValue)
                        }
                    }
                }
            }

            // Tracker
            OrderTracker(order.status)

            // Eyewear details
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eyewear details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    if (order.items.isEmpty()) {
                        Text(
                            "No items on this order",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    "${item.quantity} x ${formatPeso(item.unitPrice)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatPeso(item.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        if (ratingsEnabled && item.isRateable) {
                            TextButton(onClick = { onRateItem(item.id) }) {
                                Text(if (item.rating != null) "Update rating" else "Rate this item")
                            }
                        }
                        if (ratingsEnabled && item.rating != null) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "Rating: ${item.rating.rating}/5${item.rating.comment?.let { " - $it" } ?: ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }

            // Payment summary
            val paymentSummary = order.paymentSummary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (paymentSummary != null) {
                        DetailInfoRow(
                            "Status",
                            paymentStatusLabel(paymentSummary.status),
                            valueColor = paymentStatusTextColor(paymentSummary.status),
                        )
                        DetailInfoRow("Total", formatPeso(paymentSummary.totalAmount))
                        DetailInfoRow("Paid", formatPeso(paymentSummary.amountPaid))
                        DetailInfoRow(
                            "Balance due",
                            formatPeso(paymentSummary.balanceDue),
                            valueColor = if (balanceDue != null) {
                                MaterialTheme.colorScheme.error
                            } else {
                                paymentStatusTextColor(paymentSummary.status)
                            },
                            valueWeight = if (balanceDue != null) FontWeight.Bold else FontWeight.SemiBold,
                        )
                        paymentSummary.paymentDueDate?.let { DetailInfoRow("Due date", it) }
                        if (paymentSummary.isOverdue) {
                            Text("Overdue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        // Explicit "unavailable" rather than omitting the whole section, so a patient
                        // checking whether they owe money can't mistake missing data for nothing owed.
                        Text(
                            "Payment info unavailable",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(if (showBottomBar) 0.dp else 16.dp))
        }

        if (showBottomBar) {
            // Rounded top corners plus a hairline border read as an intentional sheet lifted off
            // the canvas, matching the pattern used on the appointment and request detail screens,
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
                ) {
                    AppointmentPrimaryButton(
                        text = "Message the clinic about your balance",
                        onClick = onNavigateToMessages,
                    )
                }
            }
        }
    }
}

private data class OrderStatusGuidanceCopy(val title: String, val message: String, val icon: ImageVector)

@Composable
private fun OrderStatusGuidance(status: OpticalOrderStatus) {
    val copy = when (status) {
        OpticalOrderStatus.QUEUED -> OrderStatusGuidanceCopy(
            title = "Order received",
            message = "Your eyewear has been added to the queue for preparation.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.IN_PROGRESS -> OrderStatusGuidanceCopy(
            title = "Being prepared",
            message = "Your eyewear is currently being prepared by our lab.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.READY_FOR_DISPENSING -> OrderStatusGuidanceCopy(
            title = "Ready for pickup",
            message = "Your eyewear is ready. Visit the clinic to pick it up.",
            icon = Icons.Outlined.CheckCircle,
        )
        OpticalOrderStatus.DISPENSED -> OrderStatusGuidanceCopy(
            title = "Released to you",
            message = "This order has been completed and released.",
            icon = Icons.Outlined.CheckCircle,
        )
        OpticalOrderStatus.CANCELLED -> OrderStatusGuidanceCopy(
            title = "Order cancelled",
            message = "This order is no longer active.",
            icon = Icons.Outlined.Info,
        )
        OpticalOrderStatus.UNKNOWN -> OrderStatusGuidanceCopy(
            title = "Status unavailable",
            message = "We couldn't confirm the latest status. Pull down to refresh.",
            icon = Icons.Outlined.Info,
        )
    }
    val isActive = status == OpticalOrderStatus.QUEUED ||
        status == OpticalOrderStatus.IN_PROGRESS ||
        status == OpticalOrderStatus.READY_FOR_DISPENSING

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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
            }
        }
    }
}

@Composable
private fun OrderTracker(status: OpticalOrderStatus) {
    val tracker = computeOrderTracker(status)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tracker.steps.forEachIndexed { index, (step, completed) ->
                // The active step gets a solid fill (matching this app's primary/onPrimary
                // "selected" convention) so it reads as distinct from a step that's merely
                // done - without this, "Prep" and "Ready" render identically once ready.
                val isActive = step == tracker.activeStep
                val (fillColor, textColor, weight) = when {
                    isActive -> Triple(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.onPrimary,
                        FontWeight.Bold,
                    )
                    completed -> Triple(
                        EyecareColors.current.accentText.copy(alpha = 0.12f),
                        EyecareColors.current.accentText,
                        FontWeight.SemiBold,
                    )
                    else -> Triple(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        FontWeight.Normal,
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(shape = RoundedCornerShape(50), color = fillColor) {
                        Text(
                            when (step) {
                                TrackerStep.PREPARATION -> "Prep"
                                TrackerStep.READY -> "Ready"
                                TrackerStep.RELEASED -> "Released"
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor,
                            fontWeight = weight,
                        )
                    }
                }
                if (index < tracker.steps.lastIndex) {
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
    if (tracker.terminalMessage != null) {
        Text(
            tracker.terminalMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun DetailInfoRow(
    label: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    valueWeight: FontWeight = FontWeight.SemiBold,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = valueWeight)
    }
}
