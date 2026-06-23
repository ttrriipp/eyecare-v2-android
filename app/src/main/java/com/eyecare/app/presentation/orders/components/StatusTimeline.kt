package com.eyecare.app.presentation.orders.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.eyecare.app.ui.theme.StatusConfirmed

private val TIMELINE_STEPS = listOf(
    OrderStatus.REQUESTED to "Requested",
    OrderStatus.CONFIRMED to "Confirmed",
    OrderStatus.PROCESSING to "Processing",
    OrderStatus.READY_FOR_PICKUP to "Ready",
    OrderStatus.COMPLETED to "Completed",
)

private val DOT_SIZE = 28.dp
private val LINE_HEIGHT = 3.dp

@Composable
fun StatusTimeline(currentStatus: OrderStatus, modifier: Modifier = Modifier) {
    if (currentStatus == OrderStatus.CANCELLED) {
        Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Order Cancelled",
                color = StatusCancelled,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    val currentIndex = TIMELINE_STEPS.indexOfFirst { it.first == currentStatus }

    Column(modifier = modifier.fillMaxWidth()) {
        // Dots + lines row
        Box(Modifier.fillMaxWidth()) {
            // Connector lines drawn first (behind dots)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .offset(y = -(DOT_SIZE / 2)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TIMELINE_STEPS.forEachIndexed { index, _ ->
                    // Spacer takes up the dot width
                    Box(Modifier.size(DOT_SIZE))
                    if (index < TIMELINE_STEPS.lastIndex) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(LINE_HEIGHT)
                                .background(
                                    if (index < currentIndex) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant,
                                ),
                        )
                    }
                }
            }

            // Dots row on top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TIMELINE_STEPS.forEachIndexed { index, _ ->
                    val isDone = index <= currentIndex
                    val isCurrent = index == currentIndex
                    Box(
                        modifier = Modifier
                            .size(DOT_SIZE)
                            .background(
                                when {
                                    isDone -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                                CircleShape,
                            )
                            .then(
                                if (isCurrent) Modifier.background(
                                    MaterialTheme.colorScheme.primary,
                                    CircleShape,
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isDone) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Labels row
        Row(Modifier.fillMaxWidth()) {
            TIMELINE_STEPS.forEachIndexed { index, (_, label) ->
                val isDone = index <= currentIndex
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = if (isDone) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
