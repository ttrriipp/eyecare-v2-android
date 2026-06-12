package com.eyecare.app.presentation.orders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.ui.theme.StatusCancelled

// Ordered progression for the timeline (cancelled handled separately)
private val TIMELINE_STEPS = listOf(
    OrderStatus.REQUESTED to "Requested",
    OrderStatus.UNDER_REVIEW to "Under Review",
    OrderStatus.CONFIRMED to "Confirmed",
    OrderStatus.PREPARING to "Preparing",
    OrderStatus.READY_FOR_PICKUP to "Ready",
    OrderStatus.COMPLETED to "Completed",
)

@Composable
fun StatusTimeline(currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    if (currentStatus == OrderStatus.CANCELLED) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text("Order Cancelled", color = StatusCancelled, style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val currentIndex = TIMELINE_STEPS.indexOfFirst { it.first == currentStatus }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        TIMELINE_STEPS.forEachIndexed { index, (_, label) ->
            val isDone = index <= currentIndex
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Dot
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDone) Icon(Icons.Default.Check, contentDescription = null,
                        tint = Color.White, modifier = Modifier.size(14.dp))
                }
                // Connector line (except last)
                if (index < TIMELINE_STEPS.lastIndex) {
                    Box(
                        Modifier
                            .height(2.dp)
                            .fillMaxWidth(0.9f)
                            .background(
                                if (index < currentIndex) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                    color = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
