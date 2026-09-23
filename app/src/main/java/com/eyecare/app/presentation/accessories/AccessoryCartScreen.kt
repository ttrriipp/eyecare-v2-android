package com.eyecare.app.presentation.accessories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryCart
import com.eyecare.app.domain.model.AccessoryCartItem
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.ui.theme.EyecareColors
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryCartScreen(
    cart: AccessoryCart,
    onIncrement: (Int) -> Unit,
    onDecrement: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onRestore: (AccessoryCartItem) -> Unit,
    onClear: () -> Unit,
    onCheckout: () -> Unit,
    onBrowseAccessories: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showClearConfirmation by remember { mutableStateOf(false) }
    val hasUnavailableItems = cart.items.any { !it.availability.isOrderable }

    fun removeItem(item: AccessoryCartItem) {
        onRemove(item.productVariantId)
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = "${item.productName} removed",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onRestore(item)
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear cart?") },
            text = { Text("Remove all ${cart.itemCount} item${if (cart.itemCount == 1) "" else "s"} from your request?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Clear cart")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Keep items")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (cart.isEmpty) {
                            "Cart"
                        } else {
                            "Cart · ${cart.itemCount} ${if (cart.itemCount == 1) "item" else "items"}"
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (cart.items.isNotEmpty()) {
                        TextButton(
                            onClick = { showClearConfirmation = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = EyecareColors.current.accentText,
                            ),
                        ) {
                            Text("Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        if (cart.isEmpty) {
            EmptyContent(
                message = "Your cart is empty",
                actionLabel = "Browse accessories",
                onAction = onBrowseAccessories,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                ) {
                    items(cart.items, key = { it.productVariantId }) { item ->
                        CartItemRow(
                            item = item,
                            onIncrement = { onIncrement(item.productVariantId) },
                            onDecrement = { onDecrement(item.productVariantId) },
                            onRemove = { removeItem(item) },
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                "Estimated total",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                pesoFormat.format(cart.estimatedTotal),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EyecareColors.current.accentText,
                            )
                        }
                        Text(
                            "The clinic confirms the final total after review. No payment or stock hold yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (hasUnavailableItems) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                            ) {
                                Text(
                                    "Remove unavailable items to continue.",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                        Button(
                            onClick = onCheckout,
                            enabled = !hasUnavailableItems,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                        ) {
                            Text("Review order request")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: AccessoryCartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                CartItemImage(item)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        item.productName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.variantName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    AvailabilityBadge(item.availability)
                    Text(
                        "Up to 5 per variant",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "${item.quantity} × ${pesoFormat.format(item.unitPrice)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        pesoFormat.format(item.unitPrice * BigDecimal(item.quantity)),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = EyecareColors.current.accentText,
                    )
                }

                QuantityStepper(
                    item = item,
                    onIncrement = onIncrement,
                    onDecrement = onDecrement,
                )
                IconButton(onClick = onRemove, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Remove ${item.productName} from cart",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStepper(
    item: AccessoryCartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconButton(
                onClick = onDecrement,
                enabled = item.quantity > 1,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Outlined.Remove,
                    contentDescription = "Decrease quantity for ${item.productName}",
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                "${item.quantity}",
                modifier = Modifier.semantics {
                    contentDescription = "${item.productName}, quantity ${item.quantity}"
                    liveRegion = LiveRegionMode.Polite
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            IconButton(
                onClick = onIncrement,
                enabled = item.quantity < 5,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Increase quantity for ${item.productName}",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun CartItemImage(item: AccessoryCartItem) {
    val imageUrl = item.imagePath
        ?.takeIf(String::isNotBlank)
        ?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val imageLoaded = imageState is AsyncImagePainter.State.Success
    val imageFailed = imageState is AsyncImagePainter.State.Error

    Surface(
        modifier = Modifier.size(72.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageLoaded) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Icon(
                        Icons.Outlined.ShoppingBag,
                        contentDescription = null,
                        tint = EyecareColors.current.accentText,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        if (imageFailed || imageUrl == null) "Photo unavailable" else "Loading photo…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "${item.productName} image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (imageLoaded) 1f else 0f),
                    onState = { imageState = it },
                )
            }
        }
    }
}

@Composable
private fun AvailabilityBadge(availability: AccessoryAvailability) {
    val (label, fillColor, textColor) = when (availability) {
        AccessoryAvailability.AVAILABLE -> Triple(
            "In stock",
            EyecareColors.current.statusConfirmed,
            EyecareColors.current.statusConfirmedText,
        )
        AccessoryAvailability.LOW_STOCK -> Triple(
            "Low stock",
            EyecareColors.current.statusPending,
            EyecareColors.current.statusPendingText,
        )
        AccessoryAvailability.UNAVAILABLE -> Triple(
            "Unavailable",
            EyecareColors.current.statusCancelled,
            EyecareColors.current.statusCancelledText,
        )
        AccessoryAvailability.UNKNOWN -> Triple(
            "Status unknown",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = fillColor.copy(alpha = 0.12f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
        )
    }
}
