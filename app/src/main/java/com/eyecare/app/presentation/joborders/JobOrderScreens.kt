package com.eyecare.app.presentation.joborders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.JobOrder
import com.eyecare.app.domain.model.JobOrderStatus
import com.eyecare.app.presentation.common.components.ErrorContent

@Composable
fun JobOrderListScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (Int) -> Unit,
    viewModel: JobOrderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.listState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Job Orders") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            JobOrderListUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            JobOrderListUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No job orders", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            is JobOrderListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh, modifier = Modifier.padding(padding))
            is JobOrderListUiState.Success -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.jobOrders, key = { it.id }) { order ->
                    JobOrderCard(order = order, onClick = { onNavigateToDetail(order.id) })
                }
                if (state.hasMorePages) {
                    item {
                        LaunchedEffect(Unit) { viewModel.loadMore() }
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobOrderCard(order: JobOrder, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(order.jobOrderNumber ?: "Job Order #${order.id}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                StatusChip(order.status)
            }
            order.totalAmount?.let { Text("₱${String.format("%.2f", it)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun JobOrderDetailScreen(
    jobOrderId: Int,
    onBack: () -> Unit,
    viewModel: JobOrderViewModel = hiltViewModel(),
) {
    val uiState by viewModel.detailState.collectAsStateWithLifecycle()

    LaunchedEffect(jobOrderId) { viewModel.loadDetail(jobOrderId) }

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = { Text("Job Order Detail") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
            )
        },
    ) { padding ->
        when (val state = uiState) {
            JobOrderDetailUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is JobOrderDetailUiState.Error -> ErrorContent(message = state.message, onRetry = { viewModel.loadDetail(jobOrderId) }, modifier = Modifier.padding(padding))
            is JobOrderDetailUiState.Success -> JobOrderDetailContent(order = state.jobOrder, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun JobOrderDetailContent(order: JobOrder, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(order.jobOrderNumber ?: "Job Order #${order.id}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            StatusChip(order.status)
        }
        order.totalAmount?.let { Text("Total: ₱${String.format("%.2f", it)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        order.notes?.let { Text("Notes: $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }

        TimelineRow("Started", order.startedAt)
        TimelineRow("Ready", order.readyAt)
        TimelineRow("Dispensed", order.dispensedAt)
        TimelineRow("Cancelled", order.cancelledAt)

        if (order.items.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    HorizontalDivider()
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(item.description, style = MaterialTheme.typography.bodyMedium)
                                Text("Qty: ${item.quantity} × ₱${String.format("%.2f", item.unitPrice)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("₱${String.format("%.2f", item.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineRow(label: String, value: String?) {
    if (value != null) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value.take(16), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun StatusChip(status: JobOrderStatus) {
    val (label, color) = when (status) {
        JobOrderStatus.QUEUED -> "Queued" to MaterialTheme.colorScheme.surfaceVariant
        JobOrderStatus.IN_PROGRESS -> "In Progress" to MaterialTheme.colorScheme.primaryContainer
        JobOrderStatus.READY_FOR_DISPENSING -> "Ready" to MaterialTheme.colorScheme.tertiaryContainer
        JobOrderStatus.DISPENSED -> "Dispensed" to MaterialTheme.colorScheme.tertiaryContainer
        JobOrderStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.errorContainer
    }
    Surface(shape = RoundedCornerShape(50), color = color) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}
