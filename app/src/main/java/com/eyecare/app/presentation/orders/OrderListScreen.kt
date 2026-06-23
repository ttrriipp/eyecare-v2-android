package com.eyecare.app.presentation.orders

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.ui.theme.StatusCancelled
import com.eyecare.app.ui.theme.StatusConfirmed
import com.eyecare.app.ui.theme.StatusInfo
import com.eyecare.app.ui.theme.StatusPending

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: OrderListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("My Orders") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
    PullToRefreshBox(
        isRefreshing = uiState is OrderListUiState.Loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is OrderListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is OrderListUiState.Empty -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No orders yet.", style = MaterialTheme.typography.bodyMedium)
            }
            is OrderListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is OrderListUiState.Success -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.orders, key = { it.id }) { order ->
                    OrderCard(order = order, onClick = { onNavigateToDetail(order.id) })
                }
            }
        }
    }
} // end Column
}

@Composable
private fun OrderCard(order: Order, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(order.orderNumber, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                OrderStatusChip(order.status)
            }
            Text("₱${order.totalAmount}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text(order.createdAt.take(10), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun OrderStatusChip(status: OrderStatus) {
    val (label, color) = when (status) {
        OrderStatus.REQUESTED -> "Requested" to StatusPending
        OrderStatus.CONFIRMED -> "Confirmed" to StatusConfirmed
        OrderStatus.PROCESSING -> "Processing" to StatusInfo
        OrderStatus.READY_FOR_PICKUP -> "Ready" to StatusConfirmed
        OrderStatus.COMPLETED -> "Completed" to StatusConfirmed
        OrderStatus.CANCELLED -> "Cancelled" to StatusCancelled
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = color.copy(alpha = 0.15f)),
        border = SuggestionChipDefaults.suggestionChipBorder(enabled = true, borderColor = color),
    )
}



