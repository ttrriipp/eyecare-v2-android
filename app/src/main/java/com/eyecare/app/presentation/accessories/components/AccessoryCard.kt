package com.eyecare.app.presentation.accessories.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.frames.components.RatingBadge
import com.eyecare.app.ui.theme.EyecareColors
import java.text.NumberFormat
import java.util.Locale

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

@Composable
fun AccessoryCard(
    accessory: Accessory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstVariant = accessory.variants.firstOrNull()
    val firstImage = accessory.images.firstOrNull()
        ?: firstVariant?.images?.firstOrNull()
    val imageUrl = firstImage?.let(::buildImageUrl)

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                var imageState by remember(imageUrl) {
                    mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
                }
                val imageLoaded = imageState is AsyncImagePainter.State.Success

                if (!imageLoaded) {
                    AccessoryImagePlaceholder(
                        hasError = imageState is AsyncImagePainter.State.Error,
                        hasImage = imageUrl != null,
                    )
                }
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = accessory.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (imageLoaded) 1f else 0f),
                        onState = { imageState = it },
                    )
                }
                AccessoryAvailabilityBadge(
                    availability = firstVariant?.availability ?: AccessoryAvailability.UNKNOWN,
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                accessory.brand?.takeIf { it.isNotBlank() }?.let { brand ->
                    Text(
                        text = brand.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = accessory.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                RatingBadge(
                    averageRating = accessory.averageRating,
                    ratingCount = accessory.ratingCount,
                )
                firstVariant?.let { variant ->
                    val price = pesoFormat.format(variant.price)
                    Text(
                        text = price,
                        style = MaterialTheme.typography.titleLarge,
                        color = EyecareColors.current.accentText,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    variant.compareAtPrice?.let { compareAtPrice ->
                        Text(
                            text = pesoFormat.format(compareAtPrice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryImagePlaceholder(
    hasError: Boolean,
    hasImage: Boolean,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ShoppingBag,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                    tint = EyecareColors.current.accentText,
                )
            }
            Text(
                text = when {
                    !hasImage -> "Photo coming soon"
                    hasError -> "Photo unavailable"
                    else -> "Loading photo…"
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccessoryAvailabilityBadge(
    availability: AccessoryAvailability,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (availability) {
        AccessoryAvailability.AVAILABLE -> "In stock" to MaterialTheme.colorScheme.tertiary
        AccessoryAvailability.LOW_STOCK -> "Low stock" to MaterialTheme.colorScheme.secondary
        AccessoryAvailability.UNAVAILABLE -> "Unavailable" to MaterialTheme.colorScheme.error
        AccessoryAvailability.UNKNOWN -> "Status unknown" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
