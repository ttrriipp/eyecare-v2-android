package com.eyecare.app.presentation.accessories

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryOrderRequestListScreen(
    uiState: RequestListUiState,
    onSelectFilter: (OrderRequestFilter) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToRequest: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRefreshing = (uiState as? RequestListUiState.Success)?.isRefreshing == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order requests") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter tabs
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    OrderRequestFilter.entries.forEach { filter ->
                        SegmentedButton(
                            selected = uiState.selectedFilter == filter,
                            onClick = { onSelectFilter(filter) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = filter.ordinal,
                                count = OrderRequestFilter.entries.size,
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

                Box(modifier = Modifier.weight(1f)) {
                    when (val state = uiState) {
                        is RequestListUiState.Loading -> LoadingContent()
                        is RequestListUiState.Empty -> EmptyContent(
                            message = when (state.selectedFilter) {
                                OrderRequestFilter.CURRENT -> "No pending order requests. Submit an accessory order request to get started."
                                OrderRequestFilter.HISTORY -> "No order request history yet. Accepted, rejected, and cancelled requests will appear here."
                            },
                        )
                        is RequestListUiState.Error -> ErrorContent(
                            message = state.message,
                            onRetry = onRetry,
                        )
                        is RequestListUiState.Success -> {
                            val listState = rememberLazyListState()
                            val shouldLoadMore by remember {
                                derivedStateOf {
                                    val layoutInfo = listState.layoutInfo
                                    val total = layoutInfo.totalItemsCount
                                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                    total > 0 && lastVisible >= total - 3
                                }
                            }
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
                                items(state.items, key = { it.id }) { request ->
                                    OrderRequestCard(
                                        request = request,
                                        onClick = { onNavigateToRequest(request.id) },
                                    )
                                }
                                if (state.isLoadingMore) {
                                    item {
                                        Box(
                                            Modifier.fillMaxWidth().padding(16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                                if (state.loadMoreError != null) {
                                    item {
                                        Box(
                                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
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
}

@Composable
private fun OrderRequestCard(
    request: AccessoryOrderRequest,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        request.requestNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        requestStatusLabel(request.status),
                        style = MaterialTheme.typography.bodySmall,
                        color = requestStatusColor(request.status),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${request.items.size} item${if (request.items.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    pesoFormat.format(request.subtotalAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EyecareColors.current.accentText,
                )
            }
        }
    }
}

@Composable
private fun requestStatusColor(status: OrderRequestStatus) = when (status) {
    OrderRequestStatus.PENDING -> EyecareColors.current.statusPending
    OrderRequestStatus.ACCEPTED -> EyecareColors.current.statusConfirmed
    OrderRequestStatus.REJECTED -> MaterialTheme.colorScheme.error
    OrderRequestStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    OrderRequestStatus.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun requestStatusLabel(status: OrderRequestStatus) = when (status) {
    OrderRequestStatus.PENDING -> "Awaiting review"
    OrderRequestStatus.ACCEPTED -> "Accepted"
    OrderRequestStatus.REJECTED -> "Declined"
    OrderRequestStatus.CANCELLED -> "Cancelled"
    OrderRequestStatus.UNKNOWN -> "Status unavailable"
}