package com.eyecare.app.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eyecare.app.BuildConfig
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.OrderStatus
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.model.Product
import com.eyecare.app.presentation.appointments.StatusChip
import com.eyecare.app.ui.theme.StatusCancelled

private val DARK_BLUE = Color(0xFF1A2E5A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAppointments: () -> Unit = {},
    onNavigateToBooking: () -> Unit = {},
    onNavigateToOrderDetail: (Int) -> Unit = {},
    onNavigateToCatalog: () -> Unit = {},
    onNavigateToProductDetail: (Int) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = uiState is HomeUiState.Loading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is HomeUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is HomeUiState.Success -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // Greeting header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Good morning 👋", style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Your Vision Health", style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold)
                    }
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Vision Status Card
                Card(shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(20.dp), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Vision Status", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text("Optimal", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text("Next checkup due in 8 months", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(shape = CircleShape, color = Color.White, modifier = Modifier.size(64.dp),
                            border = BorderStroke(3.dp, MaterialTheme.colorScheme.primary)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("20/20", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Prescription expiry warning (conditional)
                state.expiringPrescription?.let { prescription ->
                    PrescriptionWarningCard(prescription, onBookExam = onNavigateToBooking)
                }

                // Next Appointment
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Next Appointment", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    TextButton(onClick = onNavigateToAppointments) { Text("View all") }
                }
                if (state.nextAppointment != null) {
                    NextAppointmentCard(state.nextAppointment)
                } else {
                    Card(shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("No upcoming appointments", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Active Order Tracker
                state.activeOrder?.let { order ->
                    Text("Active Order", style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    OrderTrackerCard(order, onClick = { onNavigateToOrderDetail(order.id) })
                }

                // New Arrivals Carousel
                if (state.newArrivals.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("New Arrivals", style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold)
                        TextButton(onClick = onNavigateToCatalog) { Text("See all") }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        items(state.newArrivals, key = { it.id }) { product ->
                            NewArrivalCard(product, onClick = { onNavigateToProductDetail(product.id) })
                        }
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

@Composable
private fun PrescriptionWarningCard(prescription: Prescription, onBookExam: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StatusCancelled.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, StatusCancelled.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Prescription Expiring Soon", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, color = StatusCancelled)
            val expiresAt = prescription.expiresAt?.take(10) ?: ""
            Text("Your prescription expires on $expiresAt. Book an exam to renew it.",
                style = MaterialTheme.typography.bodySmall, color = StatusCancelled)
            Button(
                onClick = onBookExam,
                colors = ButtonDefaults.buttonColors(containerColor = StatusCancelled),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text("Book Exam", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun NextAppointmentCard(appointment: Appointment) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DARK_BLUE),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(appointment.status)
            Text(
                appointment.visitReason.replace("_", " ").replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date:", style = MaterialTheme.typography.bodySmall,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(appointment.scheduledAt.take(16).replace("T", " • "),
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
private fun OrderTrackerCard(order: Order, onClick: () -> Unit) {
    val steps = listOf(OrderStatus.REQUESTED, OrderStatus.UNDER_REVIEW, OrderStatus.CONFIRMED,
        OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP, OrderStatus.COMPLETED)
    val currentStep = steps.indexOfFirst { it == order.status }.coerceAtLeast(0)
    val progress = (currentStep + 1).toFloat() / steps.size

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderNumber, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(order.status.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline,
            )
            Text("${"%.0f".format(progress * 100)}% complete",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NewArrivalCard(product: Product, onClick: () -> Unit) {
    val imageRef = product.images.firstOrNull { it.isPrimary }?.path ?: product.images.firstOrNull()?.path
    val imageUrl = imageRef?.let {
        if (it.startsWith("http")) it else "${BuildConfig.API_BASE_URL}storage/$it"
    }
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.width(140.dp),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (imageUrl != null) {
                    AsyncImage(model = imageUrl, contentDescription = product.name,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Column(Modifier.padding(8.dp)) {
                Text(product.brand.uppercase(), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.5.sp)
                Text(product.name, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text("$${product.price}", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
