package com.eyecare.app.presentation.messaging.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.Order

private enum class SheetPage { MAIN, APPOINTMENTS, ORDERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentSheet(
    sheetState: SheetState,
    appointments: List<Appointment>,
    orders: List<Order>,
    onAttachFile: () -> Unit,
    onLinkAppointment: (Appointment) -> Unit,
    onLinkOrder: (Order) -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(SheetPage.MAIN) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            when (page) {
                SheetPage.MAIN -> {
                    Text(
                        "Add to message",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    SheetOption(Icons.Default.AttachFile, "Attach file") {
                        onDismiss()
                        onAttachFile()
                    }
                    SheetOption(Icons.Default.CalendarMonth, "Link appointment") { page = SheetPage.APPOINTMENTS }
                    SheetOption(Icons.Default.ShoppingBag, "Link order") { page = SheetPage.ORDERS }
                }
                SheetPage.APPOINTMENTS -> {
                    Text(
                        "Select appointment",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    if (appointments.isEmpty()) {
                        Text(
                            "No appointments found",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn {
                            items(appointments) { appt ->
                                PickerRow(
                                    primary = appt.visitReason.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                    secondary = "${appt.scheduledAt.take(10)} · ${appt.status.name.lowercase()}",
                                ) {
                                    onLinkAppointment(appt)
                                    onDismiss()
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                SheetPage.ORDERS -> {
                    Text(
                        "Select order",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                    if (orders.isEmpty()) {
                        Text(
                            "No orders found",
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        LazyColumn {
                            items(orders) { order ->
                                PickerRow(
                                    primary = "Order #${order.orderNumber}",
                                    secondary = order.status.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                                ) {
                                    onLinkOrder(order)
                                    onDismiss()
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun SheetOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PickerRow(primary: String, secondary: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(primary, style = MaterialTheme.typography.bodyMedium)
        Text(secondary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
