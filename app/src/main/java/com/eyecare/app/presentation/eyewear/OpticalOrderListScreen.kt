package com.eyecare.app.presentation.eyewear

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal

@Composable
fun OpticalOrderListContent(
    uiState: OrderListUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onSelectFilter: (OrderListFilter) -> Unit,
    onNavigateToOrder: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keep existing orders visible while a refresh is in progress.
    val isRefreshing = (uiState as? OrderListUiState.Success)?.isRefreshing == true

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OrderListFilterTabs(
                selectedFilter = uiState.selectedFilter,
                onFilterSelected = onSelectFilter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is OrderListUiState.Loading -> LoadingContent()
                    is OrderListUiState.Empty -> EmptyContent(
                        message = when (state.selectedFilter) {
                            OrderListFilter.CURRENT -> "No current orders. Clinic-confirmed orders will appear here."
                            OrderListFilter.HISTORY -> "No order history yet. Completed and cancelled orders will appear here."
                        },
                    )
                    is OrderListUiState.Error -> ErrorContent(message = state.message, onRetry = onRetry)
                    is OrderListUiState.Success -> {
                        val listState = rememberLazyListState()
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val layoutInfo = listState.layoutInfo
                                val total = layoutInfo.totalItemsCount
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                total > 0 && lastVisible >= total - 3
                            }
                        }
                        // Do not immediately re-fire after a failed request while the user is
                        // still near the bottom; the retry row provides the next attempt.
                        LaunchedEffect(shouldLoadMore, state.hasMorePages, state.isLoadingMore, state.loadMoreError) {
                            if (shouldLoadMore && state.hasMorePages && !state.isLoadingMore && state.loadMoreError == null) {
                                onLoadMore()
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 8.dp,
                                bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.items, key = { it.id }) { item ->
                                OrderCard(
                                    order = item,
                                    onClick = { onNavigateToOrder(item.id) },
                                )
                            }
                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                }
                            }
                            if (state.loadMoreError != null) {
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            state.loadMoreError,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.weight(1f),
                                        )
                                        TextButton(onClick = onLoadMore) { Text("Retry") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderListFilterTabs(
    selectedFilter: OrderListFilter,
    onFilterSelected: (OrderListFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        OrderListFilter.entries.forEach { filter ->
            SegmentedButton(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = filter.ordinal,
                    count = OrderListFilter.entries.size,
                ),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    activeContentColor = EyecareColors.current.accentText,
                    activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    inactiveContainerColor = MaterialTheme.colorScheme.surface,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                label = { Text(filter.label) },
            )
        }
    }
}

@Composable
private fun OrderCard(
    order: OpticalOrder,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        orderReferenceLabel(order.orderNumber),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        orderCardTitle(order),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View order details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val statusColor = orderStatusColor(order.status)
            val statusTextColor = orderStatusTextColor(order.status)
            Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.12f)) {
                Text(
                    orderStatusLabel(order.status),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = statusTextColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            val balanceDue = order.paymentSummary?.balanceDue?.takeIf { it > BigDecimal.ZERO }
            if (balanceDue != null) {
                Text(
                    "Balance due: ${formatPeso(balanceDue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                Text(
                    "Total: ${formatPeso(order.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                )
            }

            val (dateLabel, dateValue) = orderDateLabel(order)
            Text(
                "$dateLabel $dateValue",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun orderReferenceLabel(orderNumber: String): String {
    val reference = orderNumber.trim()
    return when {
        reference.isBlank() -> "Order"
        reference.startsWith("#") -> "Order $reference"
        else -> "Order #$reference"
    }
}
