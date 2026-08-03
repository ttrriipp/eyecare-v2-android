package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderStatus
import com.eyecare.app.domain.model.PaymentStatus
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal

@Composable
fun OpticalOrderListContent(
    uiState: OrderListUiState,
    onSelectFilter: (OrderFilter) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToOrder: (Int) -> Unit,
    onNavigateToEstimate: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentFilter = when (uiState) {
        is OrderListUiState.Success -> uiState.filter
        is OrderListUiState.Empty -> uiState.filter
        else -> OrderFilter.CURRENT
    }

    Column(modifier.fillMaxSize()) {
        OrderFilterRow(currentFilter, onSelectFilter)

        when (val state = uiState) {
            is OrderListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is OrderListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Receipt, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.filter == OrderFilter.CURRENT) "No current eyewear orders"
                        else "No order history yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            is OrderListUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = onRetry,
            )
            is OrderListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    OrderCard(
                        order = item,
                        onClick = { onNavigateToOrder(item.id) },
                        onViewEstimate = item.sourceQuotation?.let { ref -> { onNavigateToEstimate(ref.id) } },
                    )
                }
                if (state.isLoadingMore) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
                if (state.loadMoreError != null) {
                    item {
                        Text(
                            state.loadMoreError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderFilterRow(
    currentFilter: OrderFilter,
    onSelectFilter: (OrderFilter) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OrderFilter.entries.forEach { filter ->
            val selected = currentFilter == filter
            Surface(
                onClick = { onSelectFilter(filter) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant),
            ) {
                Text(
                    filter.label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun OrderCard(
    order: OpticalOrder,
    onClick: () -> Unit,
    onViewEstimate: (() -> Unit)?,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                orderCardTitle(order),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
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
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Text(
                "Ref: ${order.orderNumber}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val (dateLabel, dateValue) = orderDateLabel(order)
            Text(
                "$dateLabel: $dateValue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                "Total: ${formatPeso(order.totalAmount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            // Payment status
            if (order.paymentSummary != null) {
                val ps = order.paymentSummary
                val payColor = when (ps.status) {
                    PaymentStatus.PAID -> MaterialTheme.colorScheme.tertiary
                    PaymentStatus.UNPAID, PaymentStatus.PARTIALLY_PAID -> MaterialTheme.colorScheme.error
                    PaymentStatus.VOIDED -> MaterialTheme.colorScheme.onSurfaceVariant
                    PaymentStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Surface(shape = RoundedCornerShape(50), color = payColor.copy(alpha = 0.12f)) {
                    Text(
                        paymentStatusLabel(ps.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = payColor,
                    )
                }
                if (ps.balanceDue > BigDecimal.ZERO) {
                    Text(
                        "Balance: ${formatPeso(ps.balanceDue)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            if (onViewEstimate != null) {
                TextButton(onClick = onViewEstimate) {
                    Text("View original estimate")
                }
            }
        }
    }
}
