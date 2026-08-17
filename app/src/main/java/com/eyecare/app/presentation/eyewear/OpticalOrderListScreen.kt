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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
    onNavigateToOrder: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Loading covers the first, data-less load; a Success screen's own isRefreshing covers a
    // pull-to-refresh or resume-triggered refresh that keeps existing items visible underneath.
    val isRefreshing = uiState is OrderListUiState.Loading ||
        (uiState as? OrderListUiState.Success)?.isRefreshing == true

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is OrderListUiState.Loading -> LoadingContent()
            is OrderListUiState.Empty -> EmptyContent(message = "No eyewear orders yet")
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
                // Gated on loadMoreError == null so a failed attempt doesn't immediately
                // re-fire while the user is still scrolled near the bottom — only a genuine
                // scroll-position change or an explicit Retry tap (below) triggers another try.
                LaunchedEffect(shouldLoadMore, state.hasMorePages, state.isLoadingMore, state.loadMoreError) {
                    if (shouldLoadMore && state.hasMorePages && !state.isLoadingMore && state.loadMoreError == null) {
                        onLoadMore()
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    state = listState,
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
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
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                    if (state.loadMoreError != null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
            Text(
                orderCardTitle(order),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Status and payment pills grouped together: the two most urgent facts on the card.
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                val paymentSummary = order.paymentSummary
                if (paymentSummary != null) {
                    val payColor = paymentStatusColor(paymentSummary.status)
                    val payTextColor = paymentStatusTextColor(paymentSummary.status)
                    Surface(shape = RoundedCornerShape(50), color = payColor.copy(alpha = 0.12f)) {
                        Text(
                            paymentStatusLabel(paymentSummary.status),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = payTextColor,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    // Explicit "unavailable" rather than silently showing nothing, so a patient
                    // scanning for money owed can't mistake missing data for a paid-in-full order.
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            "Payment info unavailable",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            order.paymentSummary?.takeIf { it.balanceDue > BigDecimal.ZERO }?.let { ps ->
                Text(
                    "Balance due: ${formatPeso(ps.balanceDue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
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
                color = EyecareColors.current.accentText,
            )
        }
    }
}
