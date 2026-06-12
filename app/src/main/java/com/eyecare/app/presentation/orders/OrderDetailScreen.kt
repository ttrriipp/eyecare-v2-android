package com.eyecare.app.presentation.orders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.OrderItem
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.presentation.orders.components.StatusTimeline

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    orderId: Int,
    onBack: () -> Unit,
    onViewBilling: (orderId: Int) -> Unit,
    onLeaveFeedback: (orderId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<OrderDetailViewModel, OrderDetailViewModel.Factory> { it.create(orderId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Order Detail") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )
        when (val state = uiState) {
            is OrderDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is OrderDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is OrderDetailUiState.Success -> {
                val order = state.order
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(order.orderNumber, style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold)
                        OrderStatusChip(order.status)
                    }
                    Text(order.createdAt.take(10), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                    // Status timeline
                    StatusTimeline(order.status)

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    // Order items
                    if (order.items.isNotEmpty()) {
                        Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        order.items.forEach { item ->
                            OrderItemRow(item)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    }

                    // Totals
                    Card(shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            TotalRow("Subtotal", "$${order.subtotal}")
                            TotalRow("Total", "$${order.totalAmount}", bold = true)
                        }
                    }

                    // Billing button for confirmed+ orders
                    val confirmedStatuses = setOf(OrderStatus.CONFIRMED, OrderStatus.PREPARING,
                        OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED)
                    if (order.status in confirmedStatuses) {
                        Button(onClick = { onViewBilling(order.id) }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp)) {
                            Text("View Billing")
                        }
                    }

                    // Feedback button for completed orders
                    if (order.status == OrderStatus.COMPLETED) {
                        OutlinedButton(onClick = { onLeaveFeedback(order.id) }, modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp)) {
                            Text("Leave Feedback")
                        }
                    }

                    Spacer(Modifier.height(96.dp))
                }
            }
        }
    }
}

@Composable
private fun OrderItemRow(item: OrderItem) {
    Card(shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(item.productName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("${item.variantName} · ${item.lensTypeName}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Qty: ${item.quantity}", style = MaterialTheme.typography.bodySmall)
                Text("$${item.subtotal}", style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            color = if (bold) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
    }
}
