package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.messaging.PendingContext
import com.eyecare.app.presentation.eyewear.estimateCardTitle
import com.eyecare.app.presentation.eyewear.orderCardTitle

@Composable
fun ContextCard(
    context: PendingContext,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (context) {
                is PendingContext.Estimate -> Icons.Outlined.Receipt
                is PendingContext.Order -> Icons.Outlined.ShoppingBag
            },
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            when (context) {
                is PendingContext.Estimate -> {
                    val q = context.quotation
                    Text("Estimate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${estimateCardTitle(q)} · ${q.quotationNumber}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is PendingContext.Order -> {
                    val o = context.order
                    Text("Eyewear order", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "${orderCardTitle(o)} · ${o.orderNumber}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Remove context")
        }
    }
}
