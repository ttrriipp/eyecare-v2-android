package com.eyecare.app.presentation.accessories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccessoryDetailScreen(
    uiState: AccessoryDetailUiState,
    onVariantSelect: (Int) -> Unit,
    onAddToCart: (Int) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessory Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (val state = uiState) {
                is AccessoryDetailUiState.Loading -> LoadingContent()
                is AccessoryDetailUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = if (!state.isNotFound) onRetry else null,
                )
                is AccessoryDetailUiState.Success -> {
                    val accessory = state.accessory
                    val selectedVariant = state.selectedVariant
                    val firstImage = accessory.images.firstOrNull()
                        ?: selectedVariant?.images?.firstOrNull()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        if (firstImage != null) {
                            AsyncImage(
                                model = buildImageUrl(firstImage),
                                contentDescription = accessory.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.5f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        "Photo coming soon",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        Text(
                            accessory.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )

                        if (accessory.averageRating != null && accessory.ratingCount > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "${accessory.averageRating} (${accessory.ratingCount} reviews)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        if (accessory.variants.size > 1) {
                            Text(
                                "Options",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                accessory.variants.forEach { variant ->
                                    val isSelected = variant.id == state.selectedVariantId
                                    val isOrderable = variant.availability.isOrderable
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { if (isOrderable) onVariantSelect(variant.id) },
                                        label = { Text(variant.name) },
                                        enabled = isOrderable,
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        ),
                                    )
                                }
                            }
                        }

                        if (selectedVariant != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        pesoFormat.format(selectedVariant.price),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    if (selectedVariant.compareAtPrice != null) {
                                        Text(
                                            pesoFormat.format(selectedVariant.compareAtPrice),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                AvailabilityBadge(availability = selectedVariant.availability)
                            }
                        }

                        if (accessory.description != null) {
                            Text(
                                "About",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                accessory.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        if (selectedVariant != null && selectedVariant.attributes.isNotEmpty()) {
                            Text(
                                "Specifications",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            selectedVariant.attributes.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        key.replace("_", " ").replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        value,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { onAddToCart(state.selectedVariantId) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = state.canAddToCart,
                        ) {
                            Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                            Text("Add to cart")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvailabilityBadge(availability: AccessoryAvailability) {
    val (label, color) = when (availability) {
        AccessoryAvailability.AVAILABLE -> "In stock" to MaterialTheme.colorScheme.tertiary
        AccessoryAvailability.LOW_STOCK -> "Low stock" to MaterialTheme.colorScheme.secondary
        AccessoryAvailability.UNAVAILABLE -> "Unavailable" to MaterialTheme.colorScheme.error
        AccessoryAvailability.UNKNOWN -> "Status unknown" to MaterialTheme.colorScheme.onSurfaceVariant
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