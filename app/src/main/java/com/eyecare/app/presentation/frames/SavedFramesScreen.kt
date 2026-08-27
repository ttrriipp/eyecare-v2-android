package com.eyecare.app.presentation.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.SavedFrameDisclaimer
import com.eyecare.app.ui.theme.EyecareColors
import com.eyecare.app.presentation.eyewear.formatTimestamp
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedFramesScreen(
    uiState: SavedFramesUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onOpenFrame: (frameId: Int, variantId: Int) -> Unit,
    onClearError: () -> Unit,
) {
    RefreshOnResumeEffect(onRefresh = onRefresh)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Frames") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is SavedFramesUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SavedFramesUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = state.patientSafeMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = onRefresh) {
                            Text("Try again")
                        }
                    }
                }
                is SavedFramesUiState.Success -> {
                    if (state.items.isEmpty() && !state.isRefreshing) {
                        EmptySavedFrames()
                    } else {
                        SavedFramesContent(
                            state = state,
                            onRefresh = onRefresh,
                            onLoadMore = onLoadMore,
                            onRemoveFrame = onRemoveFrame,
                            onOpenFrame = onOpenFrame,
                            onClearError = onClearError,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedFramesContent(
    state: SavedFramesUiState.Success,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRemoveFrame: (Int) -> Unit,
    onOpenFrame: (frameId: Int, variantId: Int) -> Unit,
    onClearError: () -> Unit,
) {
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
            ),
        ) {
            item {
                SavedFrameDisclaimer()
            }

            items(
                items = state.items,
                key = { it.productVariantId },
            ) { savedFrame ->
                SavedFrameCard(
                    savedFrame = savedFrame,
                    isRemoving = savedFrame.productVariantId in state.removingVariantIds,
                    onRemove = { onRemoveFrame(savedFrame.productVariantId) },
                    onOpen = { onOpenFrame(savedFrame.variant.product.id, savedFrame.productVariantId) },
                )
            }

            if (state.isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            if (state.canLoadMore && !state.isLoadingMore) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Load more")
                    }
                }
            }

            if (state.inlineError != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = state.inlineError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(
                                onClick = if (state.canRetryLoadMore) onLoadMore else onClearError,
                            ) {
                                Text(if (state.canRetryLoadMore) "Retry" else "Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedFrameCard(
    savedFrame: SavedFrame,
    isRemoving: Boolean,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        onClick = onOpen,
        enabled = !isRemoving,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val imageUrl = savedFrame.variant.images.firstOrNull()?.let(::buildImageUrl)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(64.dp),
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = savedFrame.variant.name,
                        modifier = Modifier.padding(6.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit,
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = EyecareColors.current.accentText,
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = savedFrame.variant.product.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = savedFrame.variant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatPrice(savedFrame.variant.price),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                )
                Text(
                    text = "Saved ${formatTimestamp(savedFrame.savedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (savedFrame.availability != SavedFrameAvailability.AVAILABLE) {
                    UnavailableBadge()
                }
            }

            IconButton(
                onClick = onRemove,
                enabled = !isRemoving,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "Remove ${savedFrame.variant.name} from saved"
                },
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun UnavailableBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
    ) {
        Text(
            text = "Unavailable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun EmptySavedFrames() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SavedFrameDisclaimer(modifier = Modifier.padding(bottom = 24.dp))
        Icon(
            Icons.Outlined.BookmarkBorder,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No saved frames yet",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Browse frames and save your favorites to find them here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatPrice(price: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    return format.format(price)
}
