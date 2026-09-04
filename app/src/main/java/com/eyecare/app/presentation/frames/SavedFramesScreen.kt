package com.eyecare.app.presentation.frames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImagePainter
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.model.SavedFrameAvailability
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
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
    onNavigateToFrames: () -> Unit = {},
    onClearSuccessMessage: () -> Unit = {},
) {
    RefreshOnResumeEffect(onRefresh = onRefresh)

    var pendingRemovalVariantId by rememberSaveable { mutableStateOf<Int?>(null) }
    val pendingRemoval = (uiState as? SavedFramesUiState.Success)
        ?.items
        ?.firstOrNull { it.productVariantId == pendingRemovalVariantId }
    val snackbarHostState = remember { SnackbarHostState() }
    val successMessage = (uiState as? SavedFramesUiState.Success)?.successMessage

    LaunchedEffect(successMessage) {
        successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            onClearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.semantics {
                                contentDescription = "Loading saved frames"
                            },
                        ) {
                            CircularProgressIndicator()
                            Text(
                                text = "Loading saved frames",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onRefresh,
                            modifier = Modifier.heightIn(min = 52.dp),
                            shape = RoundedCornerShape(26.dp),
                        ) {
                            Text("Try again")
                        }
                    }
                }
                is SavedFramesUiState.Success -> {
                    if (state.items.isEmpty() && !state.isRefreshing) {
                        EmptySavedFrames(onNavigateToFrames = onNavigateToFrames)
                    } else {
                        SavedFramesContent(
                            state = state,
                            onRefresh = onRefresh,
                            onLoadMore = onLoadMore,
                            onRequestRemove = { pendingRemovalVariantId = it.productVariantId },
                            onRetryRemove = onRemoveFrame,
                            onOpenFrame = onOpenFrame,
                            onClearError = onClearError,
                        )
                    }
                }
            }
        }
    }

    pendingRemoval?.let { savedFrame ->
        AppConfirmationDialog(
            icon = Icons.Outlined.BookmarkBorder,
            title = "Remove saved frame?",
            message = "Remove " + savedFrame.variant.product.name +
                " (" + savedFrame.variant.name + ") from your saved frames?",
            confirmLabel = "Remove",
            dismissLabel = "Keep saved",
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
            onConfirm = {
                pendingRemovalVariantId = null
                onRemoveFrame(savedFrame.productVariantId)
            },
            onDismissRequest = { pendingRemovalVariantId = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SavedFramesContent(
    state: SavedFramesUiState.Success,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRequestRemove: (SavedFrame) -> Unit,
    onRetryRemove: (Int) -> Unit,
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
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp,
            ),
        ) {
            item(key = "preference-disclaimer") {
                SavedFrameDisclaimer()
            }

            if (state.inlineError != null) {
                item(key = "inline-error") {
                    SavedFramesErrorBanner(
                        message = state.inlineError,
                        action = state.inlineErrorAction,
                        onRefresh = onRefresh,
                        onLoadMore = onLoadMore,
                        onRetryRemove = onRetryRemove,
                        onDismiss = onClearError,
                    )
                }
            }

            items(
                items = state.items,
                key = { it.productVariantId },
            ) { savedFrame ->
                SavedFrameCard(
                    savedFrame = savedFrame,
                    isRemoving = savedFrame.productVariantId in state.removingVariantIds,
                    onRemove = { onRequestRemove(savedFrame) },
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
        }
    }
}

@Composable
private fun SavedFramesErrorBanner(
    message: String,
    action: SavedFramesInlineErrorAction?,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onRetryRemove: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val actionLabel: String
    val onAction: () -> Unit
    when (action) {
        SavedFramesInlineErrorAction.RetryRefresh -> {
            actionLabel = "Try again"
            onAction = onRefresh
        }
        SavedFramesInlineErrorAction.RetryLoadMore -> {
            actionLabel = "Try again"
            onAction = onLoadMore
        }
        is SavedFramesInlineErrorAction.RetryRemove -> {
            actionLabel = "Try again"
            onAction = { onRetryRemove(action.productVariantId) }
        }
        null -> {
            actionLabel = "Dismiss"
            onAction = onDismiss
        }
    }

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
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = onAction,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun SavedFrameThumbnail(savedFrame: SavedFrame) {
    val imageUrl = savedFrame.variant.images.firstOrNull()?.let(::buildImageUrl)
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val imageLoaded = imageState is AsyncImagePainter.State.Success
    val placeholderDescription = when {
        imageUrl == null -> "Frame image unavailable"
        imageState is AsyncImagePainter.State.Error -> "Frame image unavailable"
        else -> "Loading frame image"
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.size(64.dp),
    ) {
        if (!imageLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = placeholderDescription },
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp).size(20.dp),
                        tint = EyecareColors.current.accentText,
                    )
                }
            }
        }
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = savedFrame.variant.product.name + " " +
                    savedFrame.variant.name + " frame image",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .alpha(if (imageLoaded) 1f else 0f),
                contentScale = ContentScale.Fit,
                onState = { imageState = it },
            )
        }
    }
}

@Composable
private fun AvailabilityBadge(availability: SavedFrameAvailability) {
    val label: String
    val containerColor: androidx.compose.ui.graphics.Color
    val contentColor: androidx.compose.ui.graphics.Color
    when (availability) {
        SavedFrameAvailability.AVAILABLE -> return
        SavedFrameAvailability.UNAVAILABLE -> {
            label = "Unavailable"
            containerColor = MaterialTheme.colorScheme.errorContainer
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        }
        SavedFrameAvailability.UNKNOWN -> {
            label = "Availability unknown"
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = EyecareColors.current.accentText
        }
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SavedFrameCard(
    savedFrame: SavedFrame,
    isRemoving: Boolean,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    val locale = LocalLocale.current.platformLocale

    Card(
        onClick = onOpen,
        enabled = !isRemoving,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SavedFrameThumbnail(savedFrame)

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val product = savedFrame.variant.product
                if (product.brand.isNotBlank()) {
                    Text(
                        text = product.brand.uppercase(locale),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = savedFrame.variant.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatPrice(savedFrame.variant.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Saved " + formatTimestamp(savedFrame.savedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                AvailabilityBadge(savedFrame.availability)
            }

            IconButton(
                onClick = onRemove,
                enabled = !isRemoving,
                modifier = Modifier.size(48.dp).semantics {
                    contentDescription = "Remove " + savedFrame.variant.name + " from saved"
                },
            ) {
                if (isRemoving) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(20.dp)
                            .semantics {
                                contentDescription = "Removing " + savedFrame.variant.name
                            },
                        strokeWidth = 2.dp,
                    )
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
private fun EmptySavedFrames(onNavigateToFrames: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
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
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        SavedFrameDisclaimer()
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onNavigateToFrames,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            shape = RoundedCornerShape(26.dp),
        ) {
            Text("Browse frames")
        }
    }
}

private fun formatPrice(price: BigDecimal): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    return format.format(price)
}
