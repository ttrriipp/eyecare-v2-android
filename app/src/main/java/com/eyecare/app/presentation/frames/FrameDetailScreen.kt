package com.eyecare.app.presentation.frames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.text.HtmlCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.isTypedArReady
import com.eyecare.app.presentation.common.FeatureFlags
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.frames.components.RatingSummary
import com.eyecare.app.ui.theme.EyecareColors
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrameDetailScreen(
    frameId: Int,
    onBack: () -> Unit,
    onNavigateToAr: (frameId: Int, variantId: Int) -> Unit,
    onNavigateToReserve: (frameId: Int, variantId: Int) -> Unit,
    ratingsEnabled: Boolean = FeatureFlags.FRAME_RATINGS_ENABLED,
) {
    val viewModel = hiltViewModel<FrameDetailViewModel, FrameDetailViewModel.Factory> { it.create(frameId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val locale = LocalLocale.current.platformLocale
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (uiState as? FrameDetailUiState.Success)?.message
    var showVariantPicker by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    (uiState as? FrameDetailUiState.Success)?.frame?.let { frame ->
                        Text(
                            text = frame.name,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
            if ((uiState as? FrameDetailUiState.Success)?.isRefreshing == true) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            when (val state = uiState) {
            is FrameDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is FrameDetailUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
            is FrameDetailUiState.Success -> {
                val frame = state.frame
                val selected = state.selectedVariant

                Box(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(bottom = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        val images = selected.images.ifEmpty { frame.images }
                        val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }
                        LaunchedEffect(selected.id) {
                            pagerState.scrollToPage(0)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .aspectRatio(if (images.isEmpty()) 1.65f else 1.2f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            FrameHeroMedia(
                                frameName = frame.name,
                                images = images,
                                pagerState = pagerState,
                                onTryOn = if (selected.isTypedArReady) {
                                    { onNavigateToAr(frame.id, selected.id) }
                                } else {
                                    null
                                },
                            )
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Outlined.Inventory2,
                                                contentDescription = null,
                                                tint = EyecareColors.current.accentText,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = frame.brand.uppercase(locale),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            letterSpacing = 0.8.sp,
                                        )
                                        Text(
                                            text = frame.name,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        if (ratingsEnabled) {
                                            RatingSummary(
                                                averageRating = frame.averageRating,
                                                ratingCount = frame.ratingCount,
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom,
                                ) {
                                    Text(
                                        text = "Price",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = String.format(Locale.US, "₱%.2f", selected.price),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = EyecareColors.current.accentText,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        selected.compareAtPrice?.let { original ->
                                            Text(
                                                text = String.format(Locale.US, "₱%.2f", original),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textDecoration = TextDecoration.LineThrough,
                                            )
                                        }
                                    }
                                }

                                FrameCapabilityNotice(isArReady = selected.isTypedArReady)
                            }
                        }

                        if (frame.variants.size > 1) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.size(36.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Palette,
                                                    contentDescription = null,
                                                    tint = EyecareColors.current.accentText,
                                                    modifier = Modifier.size(20.dp),
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "Options",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                text = selected.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }

                                    if (frame.variants.size <= 4) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                        ) {
                                            frame.variants.forEach { variant ->
                                                val isSelected = variant.id == selected.id
                                                Surface(
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.surface,
                                                    border = BorderStroke(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.outlineVariant,
                                                    ),
                                                    modifier = Modifier
                                                        .defaultMinSize(minHeight = 48.dp)
                                                        .selectable(
                                                            selected = isSelected,
                                                            onClick = { viewModel.selectVariant(variant) },
                                                            role = Role.RadioButton,
                                                        ),
                                                ) {
                                                    Text(
                                                        text = variant.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = { showVariantPicker = true },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 48.dp),
                                            shape = RoundedCornerShape(24.dp),
                                        ) {
                                            Text("Choose from " + frame.variants.size + " options")
                                        }
                                    }
                                }
                            }
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "Details",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                selected.sku.takeIf(String::isNotBlank)?.let { DetailFactRow("SKU", it) }
                                frame.category.takeIf(String::isNotBlank)?.let { DetailFactRow("Category", it) }
                                DetailFactRow("Option", selected.name)

                                frame.description?.takeIf(String::isNotBlank)?.let { desc ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "About",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    val cleanDesc = remember(desc) {
                                        HtmlCompat.fromHtml(desc, HtmlCompat.FROM_HTML_MODE_COMPACT)
                                            .toString()
                                            .trim()
                                    }
                                    Text(
                                        text = cleanDesc,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                selected.attributes?.filterValues { it.isNotBlank() && it != "null" }?.takeIf { it.isNotEmpty() }?.let { specs ->
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                    Text(
                                        text = "Specifications",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    specs.forEach { (key, value) ->
                                        DetailFactRow(key.toSpecLabel(), value)
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 4.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                                .navigationBarsPadding(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (selected.isTypedArReady) {
                                    Button(
                                        onClick = { onNavigateToAr(frame.id, selected.id) },
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                    ) {
                                        Icon(Icons.Outlined.FaceRetouchingNatural, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Try on")
                                    }
                                }
                                if (selected.isTypedArReady) {
                                    OutlinedButton(
                                        onClick = { onNavigateToReserve(frame.id, selected.id) },
                                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    ) {
                                        Text("Reserve")
                                    }
                                } else {
                                    Button(
                                        onClick = { onNavigateToReserve(frame.id, selected.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .defaultMinSize(minHeight = 48.dp),
                                        shape = RoundedCornerShape(24.dp),
                                    ) {
                                        Text("Reserve")
                                    }
                                }
                            }
                        }
                    }
                }

                if (showVariantPicker && frame.variants.size > 4) {
                    VariantPickerSheet(
                        variants = frame.variants,
                        selectedVariant = selected,
                        onSelect = {
                            viewModel.selectVariant(it)
                            showVariantPicker = false
                        },
                        onDismiss = { showVariantPicker = false },
                    )
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 88.dp),
        )
    }
}

@Composable
private fun FrameHeroMedia(
    frameName: String,
    images: List<String>,
    pagerState: PagerState,
    onTryOn: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .aspectRatio(if (images.isEmpty()) 1.65f else 1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (images.isEmpty()) {
            FramePhotoPlaceholder(
                frameName = frameName,
                label = "Photo coming soon",
                showLoading = false,
                onRetry = null,
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                FrameDetailImage(
                    imageUrl = buildImageUrl(images[page]),
                    contentDescription = frameName +
                        ", image " + (page + 1) + " of " + images.size,
                    frameName = frameName,
                )
            }

            if (images.size > 1) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Black.copy(alpha = 0.58f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                ) {
                    Text(
                        text = (pagerState.currentPage + 1).toString() + " / " + images.size,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            onTryOn?.let { tryOn ->
                Button(
                    onClick = tryOn,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .heightIn(min = 48.dp),
                    shape = RoundedCornerShape(24.dp),
                ) {
                    Icon(
                        Icons.Outlined.FaceRetouchingNatural,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Try on")
                }
            }
        }
    }
}

@Composable
private fun FrameDetailImage(
    imageUrl: String,
    contentDescription: String,
    frameName: String,
) {
    val context = LocalContext.current
    var retryKey by remember(imageUrl) { mutableIntStateOf(0) }
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val request = remember(imageUrl, retryKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey(imageUrl + "#detail-" + retryKey)
            .diskCacheKey(imageUrl + "#detail-" + retryKey)
            .build()
    }
    val imageLoaded = imageState is AsyncImagePainter.State.Success
    val imageFailed = imageState is AsyncImagePainter.State.Error

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageLoaded) {
            FramePhotoPlaceholder(
                frameName = frameName,
                label = if (imageFailed) "Photo unavailable" else "Loading photo…",
                showLoading = !imageFailed,
                onRetry = if (imageFailed) {
                    {
                        imageState = AsyncImagePainter.State.Empty
                        retryKey += 1
                    }
                } else {
                    null
                },
            )
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .alpha(if (imageLoaded) 1f else 0f),
            onState = { imageState = it },
        )
    }
}

@Composable
private fun FramePhotoPlaceholder(
    frameName: String,
    label: String,
    showLoading: Boolean,
    onRetry: (() -> Unit)?,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(16.dp),
    ) {
        if (showLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = Icons.Outlined.FaceRetouchingNatural,
                    contentDescription = null,
                    modifier = Modifier.padding(14.dp).size(30.dp),
                    tint = EyecareColors.current.accentText,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = frameName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        onRetry?.let { retry ->
            TextButton(onClick = retry) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Try again")
            }
        }
    }
}

@Composable
private fun FrameCapabilityNotice(isArReady: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isArReady) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.FaceRetouchingNatural,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (isArReady) {
                    EyecareColors.current.accentText
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = if (isArReady) {
                    "AR-ready — see this option on your face before reserving."
                } else {
                    "Virtual try-on isn't available for this option. You can still reserve it."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (isArReady) {
                    EyecareColors.current.accentText
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantPickerSheet(
    variants: List<FrameVariant>,
    selectedVariant: FrameVariant,
    onSelect: (FrameVariant) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = "Choose an option",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Text(
                text = "Select a variant before trying on or reserving.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(variants, key = { it.id }) { variant ->
                    val isSelected = variant.id == selectedVariant.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = isSelected,
                                onClick = { onSelect(variant) },
                                role = Role.RadioButton,
                            )
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                        )
                        Column(
                            modifier = Modifier.padding(start = 12.dp),
                        ) {
                            Text(
                                text = variant.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                            Text(
                                text = String.format(Locale.US, "₱%.2f", variant.price) + " • " +
                                    if (variant.isTypedArReady) "AR-ready — Try on" else "Try-on unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                }
            }
        }
    }
}

private fun String.toSpecLabel(): String = replace('_', ' ')
    .replace('-', ' ')
    .trim()
    .split(" ")
    .filter(String::isNotBlank)
    .joinToString(" ") { word -> word.replaceFirstChar(Char::uppercaseChar) }

@Composable
private fun DetailFactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
