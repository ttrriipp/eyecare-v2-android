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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpticalOrderDetailScreen(
    onBack: () -> Unit,
    onRateItem: (Int) -> Unit,
    viewModel: OpticalOrderDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
                onRateItem = onRateItem,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun OrderDetailContent(
    order: OpticalOrder,
    onRateItem: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            orderCardTitle(order),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )

        val statusColor = when (order.status) {
            OpticalOrderStatus.QUEUED, OpticalOrderStatus.IN_PROGRESS -> EyecareColors.current.statusInfo
            OpticalOrderStatus.READY_FOR_DISPENSING -> MaterialTheme.colorScheme.tertiary
            OpticalOrderStatus.DISPENSED -> MaterialTheme.colorScheme.tertiary
            OpticalOrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
            OpticalOrderStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
            Text(
                orderStatusLabel(order.status),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Tracker
        OrderTracker(order.status)

        // Reference and dates
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailInfoRow("Reference", order.orderNumber)
                if (order.createdAt.isNotBlank()) DetailInfoRow("Created", formatTimestamp(order.createdAt))
                order.startedAt?.let { DetailInfoRow("Started", formatTimestamp(it)) }
                order.readyAt?.let { DetailInfoRow("Ready", formatTimestamp(it)) }
                order.dispensedAt?.let { DetailInfoRow("Released", formatTimestamp(it)) }
                order.cancelledAt?.let { DetailInfoRow("Cancelled", formatTimestamp(it)) }
            }
        }

        // Eyewear details
        if (order.items.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Eyewear details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.description, style = MaterialTheme.typography.bodyMedium)
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
                        if (item.isRateable) {
                            TextButton(onClick = { onRateItem(item.id) }) {
                                Text(if (item.rating != null) "Update rating" else "Rate this item")
                            }
                        }
                        if (item.rating != null) {
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
        }

        // Payment summary
        if (order.paymentSummary != null) {
            val ps = order.paymentSummary
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    DetailInfoRow("Status", paymentStatusLabel(ps.status))
                    DetailInfoRow("Total", formatPeso(ps.totalAmount))
                    DetailInfoRow("Paid", formatPeso(ps.amountPaid))
                    DetailInfoRow("Balance", formatPeso(ps.balanceDue))
                    ps.paymentDueDate?.let { DetailInfoRow("Due date", it) }
                    if (ps.isOverdue) {
                        Text("Overdue", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val color = if (completed) EyecareColors.current.accentText
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = color.copy(alpha = 0.12f),
                    ) {
                        Text(
                            when (step) {
                                TrackerStep.PREPARATION -> "Prep"
                                TrackerStep.READY -> "Ready"
                                TrackerStep.RELEASED -> "Released"
                                else -> ""
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = color,
                            fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal,
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
private fun DetailInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
