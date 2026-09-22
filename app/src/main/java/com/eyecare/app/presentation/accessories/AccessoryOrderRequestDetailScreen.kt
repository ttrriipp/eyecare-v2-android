package com.eyecare.app.presentation.accessories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ImageNotSupported
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryOrderRequestDetailScreen(
    uiState: RequestDetailUiState,
    showCancelDialog: Boolean,
    onShowCancelDialog: () -> Unit,
    onDismissCancelDialog: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToOrder: (Int) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (showCancelDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.Cancel,
            title = "Cancel request?",
            message = "Are you sure you want to cancel this order request? This cannot be undone.",
            confirmLabel = "Cancel request",
            dismissLabel = "Keep request",
            isDestructive = true,
            onConfirm = {
                onCancel()
                onDismissCancelDialog()
            },
            onDismissRequest = onDismissCancelDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Request details") },
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
        when (val state = uiState) {
            is RequestDetailUiState.Loading -> LoadingContent(modifier = Modifier.padding(padding))
            is RequestDetailUiState.Error -> ErrorContent(
                message = state.message,
                onRetry = if (!state.isNotFound) onRetry else null,
                modifier = Modifier.padding(padding),
            )
            is RequestDetailUiState.Success -> {
                val request = state.request
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Header
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    request.requestNumber,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                RequestStatusBadge(status = request.status)
                            }
                            Text(
                                pesoFormat.format(request.subtotalAmount),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    // Status guidance
                    RequestStatusGuidance(request = request)

                    // Items
                    if (request.items.isNotEmpty()) {
                        Text(
                            "Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        request.items.forEach { item ->
                            val productName = item.itemSnapshot.productName.ifBlank { item.description }
                            val variantName = item.itemSnapshot.variantName.trim()
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    RequestItemImage(
                                        imagePath = item.itemSnapshot.images.firstOrNull(),
                                        productName = productName,
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = productName,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (variantName.isNotBlank()) {
                                            Text(
                                                text = variantName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Text(
                                            "× ${item.quantity}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        pesoFormat.format(item.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    // Actions
                    if (state.canCancel) {
                        OutlinedButton(
                            onClick = onShowCancelDialog,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null)
                            Text("Cancel request")
                        }
                    }

                    if (state.isCancelling) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                            Text("Cancelling...")
                        }
                    }

                    // Accepted order navigation
                    request.order?.let { order ->
                        Button(
                            onClick = { onNavigateToOrder(order.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null)
                            Text("View order ${order.orderNumber}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestItemImage(
    imagePath: String?,
    productName: String,
) {
    val imageUrl = imagePath?.takeIf(String::isNotBlank)?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val shape = RoundedCornerShape(10.dp)

    Surface(
        modifier = Modifier.size(68.dp),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(contentAlignment = Alignment.Center) {
            val imageLoaded = imageState is AsyncImagePainter.State.Success
            if (!imageLoaded) {
                Icon(
                    imageVector = Icons.Outlined.ImageNotSupported,
                    contentDescription = "$productName image unavailable",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = productName,
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
    val (label, color) = when (status) {
        OrderRequestStatus.PENDING -> "Awaiting review" to EyecareColors.current.statusPending
        OrderRequestStatus.ACCEPTED -> "Accepted" to EyecareColors.current.statusConfirmed
        OrderRequestStatus.REJECTED -> "Declined" to MaterialTheme.colorScheme.error
        OrderRequestStatus.CANCELLED -> "Cancelled" to MaterialTheme.colorScheme.onSurfaceVariant
        OrderRequestStatus.UNKNOWN -> "Status unavailable" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun RequestStatusGuidance(request: AccessoryOrderRequest) {
    val (icon, message) = when (request.status) {
        OrderRequestStatus.PENDING -> Icons.Outlined.CheckCircle to
            "Your request is awaiting clinic review. The clinic will accept or decline it. Stock is not reserved."
        OrderRequestStatus.ACCEPTED -> Icons.Outlined.CheckCircle to
            "Your request has been accepted! You can now view the resulting order and proceed with payment."
        OrderRequestStatus.REJECTED -> Icons.Outlined.Cancel to
            (request.rejectionReason?.let { "Declined: $it" } ?: "Your request has been declined by the clinic.")
        OrderRequestStatus.CANCELLED -> Icons.Outlined.Cancel to
            "This request has been cancelled."
        OrderRequestStatus.UNKNOWN -> Icons.Outlined.Cancel to
            "The status of this request is currently unavailable."
    }
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
