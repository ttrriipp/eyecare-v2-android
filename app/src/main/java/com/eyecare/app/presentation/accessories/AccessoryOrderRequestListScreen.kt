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
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.AccessoryOrderRequestItem
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
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
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        request.requestNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RequestStatusBadge(request.status)
                Text(
                    if (request.items.size == 1) "1 item" else "${request.items.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (request.items.isEmpty()) {
                Text(
                    "Item details unavailable",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                request.items.take(2).forEach { item ->
                    RequestItemPreview(item)
                }
            }
            if (request.items.size > 2) {
                Text(
                    "And ${request.items.size - 2} more item${if (request.items.size - 2 == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Request total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    pesoFormat.format(request.subtotalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = EyecareColors.current.accentText,
                )
            }
        }
    }
}

@Composable
private fun RequestItemPreview(item: AccessoryOrderRequestItem) {
    val productName = item.itemSnapshot.productName.takeIf(String::isNotBlank) ?: item.description
    val variantName = item.itemSnapshot.variantName.takeIf(String::isNotBlank)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RequestItemThumbnail(item = item, productName = productName)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!variantName.isNullOrBlank() && variantName != productName) {
                Text(
                    variantName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                "Qty ${item.quantity}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RequestItemThumbnail(item: AccessoryOrderRequestItem, productName: String) {
    val imageUrl = item.itemSnapshot.images.firstOrNull()?.takeIf(String::isNotBlank)?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = Modifier.size(52.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (imageUrl == null || imageState !is AsyncImagePainter.State.Success) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (imageUrl != null) {
                val imageLoaded = imageState is AsyncImagePainter.State.Success
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "$productName image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(shape)
                        .alpha(if (imageLoaded) 1f else 0f),
                    onState = { imageState = it },
                )
            }
        }
    }
}

@Composable
private fun RequestStatusBadge(status: OrderRequestStatus) {
    val (label, fill, foreground) = when (status) {
        OrderRequestStatus.PENDING -> Triple("Awaiting review", EyecareColors.current.statusPending, EyecareColors.current.statusPendingText)
        OrderRequestStatus.ACCEPTED -> Triple("Accepted", EyecareColors.current.statusConfirmed, EyecareColors.current.statusConfirmedText)
        OrderRequestStatus.REJECTED -> Triple("Declined", EyecareColors.current.statusCancelled, EyecareColors.current.statusCancelledText)
        OrderRequestStatus.CANCELLED -> Triple("Cancelled", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        OrderRequestStatus.UNKNOWN -> Triple("Status unavailable", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Surface(shape = RoundedCornerShape(50), color = fill.copy(alpha = 0.14f)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
        )
    }
}
