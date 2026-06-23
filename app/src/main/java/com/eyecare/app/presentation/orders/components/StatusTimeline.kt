package com.eyecare.app.presentation.orders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Pending
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.ui.theme.StatusCancelled

private data class TimelineStep(
    val status: OrderStatus,
    val label: String,
    val icon: ImageVector,
)

private val STEPS = listOf(
    TimelineStep(OrderStatus.REQUESTED, "Requested", Icons.Outlined.ShoppingCart),
    TimelineStep(OrderStatus.CONFIRMED, "Confirmed", Icons.Outlined.AssignmentTurnedIn),
    TimelineStep(OrderStatus.PROCESSING, "Processing", Icons.Outlined.Pending),
    TimelineStep(OrderStatus.READY_FOR_PICKUP, "Ready", Icons.Outlined.Store),
    TimelineStep(OrderStatus.COMPLETED, "Completed", Icons.Outlined.CheckCircle),
)

@Composable
fun StatusTimeline(currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    if (currentStatus == OrderStatus.CANCELLED) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Order Cancelled", color = StatusCancelled, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val currentIndex = STEPS.indexOfFirst { it.status == currentStatus }.coerceAtLeast(0)
    val active = MaterialTheme.colorScheme.primary
    val inactive = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Dots + connectors
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            STEPS.forEachIndexed { index, step ->
                val done = index <= currentIndex

                // Dot
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(if (done) active else inactive, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = step.icon,
                        contentDescription = step.label,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }

                // Connector line (not after last)
                if (index < STEPS.lastIndex) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(if (index < currentIndex) active else inactive),
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Labels
        Row(Modifier.fillMaxWidth()) {
            STEPS.forEachIndexed { index, step ->
                val done = index <= currentIndex
                Text(
                    step.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (done) active else inactive,
                )
            }
        }
    }
}
