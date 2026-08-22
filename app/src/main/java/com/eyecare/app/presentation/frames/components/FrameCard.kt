package com.eyecare.app.presentation.frames.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.isTypedArReady
import com.eyecare.app.presentation.common.FeatureFlags
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.ui.theme.EyecareColors
import java.util.Locale

@Composable
fun FrameCard(
    frame: Frame,
    onClick: () -> Unit,
    onTryOn: (() -> Unit)? = null,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
    modifier: Modifier = Modifier,
) {
    val hasTypedAr = frame.variants.any { it.isTypedArReady }
    val firstTypedArVariant = frame.variants.firstOrNull { it.isTypedArReady }
    val imageUrl = (frame.images.firstOrNull() ?: frame.variants.firstOrNull()?.images?.firstOrNull())
        ?.let { buildImageUrl(it) }
    val displayVariant = frame.variants.firstOrNull()

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
                    FrameImagePlaceholder(
                        hasError = imageState is AsyncImagePainter.State.Error,
                        hasImage = imageUrl != null,
                    )
                }
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = frame.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(if (imageLoaded) 1f else 0f),
                        onState = { imageState = it },
                    )
                }
                if (hasTypedAr) {
                    ArBadge(Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
            }

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Column {
                    Text(
                        frame.brand.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                    )
                    Text(
                        frame.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (ratingsEnabled) {
                    RatingBadge(
                        averageRating = frame.averageRating,
                        ratingCount = frame.ratingCount,
                    )
                }
                val formattedPrice = displayVariant?.formattedPrice()
                val formattedCompareAtPrice = displayVariant?.formattedCompareAtPrice()
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = formattedPrice ?: "Price unavailable",
                        style = if (formattedPrice != null) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.bodySmall
                        },
                        color = if (formattedPrice != null) {
                            EyecareColors.current.accentText
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = if (formattedPrice != null) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (formattedCompareAtPrice != null) {
                        Text(
                            text = "From $formattedCompareAtPrice",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.LineThrough,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onTryOn != null && firstTypedArVariant != null) {
                    OutlinedButton(
                        onClick = onTryOn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        shape = RoundedCornerShape(24.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp),
                    ) {
                        Icon(
                            Icons.Outlined.FaceRetouchingNatural,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Text("Try on")
                    }
                }
            }
        }
    }
}

internal fun FrameVariant.formattedPrice(): String = String.format(Locale.US, "₱%.2f", price)

internal fun FrameVariant.formattedCompareAtPrice(): String? =
    compareAtPrice?.let { String.format(Locale.US, "₱%.2f", it) }

@Composable
private fun FrameImagePlaceholder(
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
                    imageVector = Icons.Outlined.FaceRetouchingNatural,
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
private fun ArBadge(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.FaceRetouchingNatural,
                contentDescription = null,
                tint = EyecareColors.current.accentText,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "AR-ready",
                style = MaterialTheme.typography.labelMedium,
                color = EyecareColors.current.accentText,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
