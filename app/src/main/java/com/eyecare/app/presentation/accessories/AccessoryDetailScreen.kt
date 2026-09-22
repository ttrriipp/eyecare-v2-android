package com.eyecare.app.presentation.accessories

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.request.ImageRequest
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch

private val pesoFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

private enum class AddToCartStatus {
    IDLE,
    ADDING,
    ADDED,
    FAILED,
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AccessoryDetailScreen(
    uiState: AccessoryDetailUiState,
    onVariantSelect: (Int) -> Unit,
    onAddToCart: (Int, Int) -> Boolean,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onNavigateToCart: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {},
    canOrder: Boolean = true,
    onNavigateToLinkAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val successState = uiState as? AccessoryDetailUiState.Success
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var addStatus by remember { mutableStateOf(AddToCartStatus.IDLE) }
    var quantity by remember { mutableStateOf(1) }

    LaunchedEffect(successState?.selectedVariantId) {
        addStatus = AddToCartStatus.IDLE
        quantity = 1
    }

    fun submitAddToCart() {
        val state = successState ?: return
        if (!state.canAddToCart || addStatus == AddToCartStatus.ADDING || addStatus == AddToCartStatus.ADDED) return

        val requestedQuantity = quantity
        addStatus = AddToCartStatus.ADDING
        val added = runCatching { onAddToCart(state.selectedVariantId, requestedQuantity) }.getOrDefault(false)
        addStatus = if (added) AddToCartStatus.ADDED else AddToCartStatus.FAILED
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = if (added) {
                    "${state.accessory.name} × $requestedQuantity added to cart."
                } else {
                    "Cart limit reached or this item is already at its maximum quantity."
                },
                actionLabel = "View cart",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) onNavigateToCart()
            addStatus = AddToCartStatus.IDLE
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = successState?.accessory?.name ?: "Accessory details",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
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
        bottomBar = {
            successState?.let { state ->
                AccessoryDetailBottomBar(
                    canAddToCart = state.canAddToCart,
                    canOrder = canOrder,
                    status = addStatus,
                    quantity = quantity,
                    onQuantityChange = { quantity = it.coerceIn(1, 5) },
                    onAddToCart = ::submitAddToCart,
                    onNavigateToLinkAccount = onNavigateToLinkAccount,
                )
            }
        },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            // Keep the confirmation inside the scaffold's content flow so it
            // reserves space instead of covering the product details below.
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (val state = uiState) {
                    is AccessoryDetailUiState.Loading -> LoadingContent()
                    is AccessoryDetailUiState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = if (!state.isNotFound) onRetry else null,
                    )
                    is AccessoryDetailUiState.Success -> {
                        AccessoryDetailContent(
                            state = state,
                            onVariantSelect = onVariantSelect,
                            onNavigateToSupport = onNavigateToSupport,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryDetailContent(
    state: AccessoryDetailUiState.Success,
    onVariantSelect: (Int) -> Unit,
    onNavigateToSupport: () -> Unit,
) {
    val accessory = state.accessory
    val selectedVariant = state.selectedVariant
    val images = remember(accessory.images, selectedVariant?.images) {
        (accessory.images + (selectedVariant?.images ?: emptyList())).distinct()
    }
    val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }

    LaunchedEffect(images, selectedVariant?.id) {
        pagerState.scrollToPage(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AccessoryHeroMedia(
            accessoryName = accessory.name,
            images = images,
            pagerState = pagerState,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val productContext = listOfNotNull(accessory.brand, accessory.category)
                    .filter { it.isNotBlank() }
                    .joinToString(" · ")
                if (productContext.isNotBlank()) {
                    Text(
                        text = productContext.uppercase(Locale.US),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = accessory.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() },
                )

                if (accessory.averageRating != null && accessory.ratingCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Star,
                            contentDescription = "Rated ${accessory.averageRating} out of 5 from ${accessory.ratingCount} reviews",
                            tint = EyecareColors.current.accentText,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            "${accessory.averageRating} (${accessory.ratingCount} reviews)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                if (selectedVariant != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = pesoFormat.format(selectedVariant.price),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = EyecareColors.current.accentText,
                            )
                            selectedVariant.compareAtPrice?.let { original ->
                                Text(
                                    text = "Was ${pesoFormat.format(original)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textDecoration = TextDecoration.LineThrough,
                                )
                            }
                        }
                        AvailabilityBadge(availability = selectedVariant.availability)
                    }
                }
            }
        }

        if (accessory.variants.size > 1) {
            AccessoryOptionsCard(
                variants = accessory.variants,
                selectedVariantId = state.selectedVariantId,
                onVariantSelect = onVariantSelect,
            )
        }

        if (accessory.description != null || (selectedVariant != null && selectedVariant.attributes.isNotEmpty())) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    accessory.description?.takeIf { it.isNotBlank() }?.let { description ->
                        Text(
                            text = "About",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (selectedVariant != null && selectedVariant.attributes.isNotEmpty()) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text(
                            text = "Specifications",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.semantics { heading() },
                        )
                        selectedVariant.attributes.forEach { (key, value) ->
                            AccessorySpecificationRow(key = key, value = value)
                        }
                    }
                }
            }
        }

        AccessoryCareGuidance(onNavigateToSupport = onNavigateToSupport)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun AccessoryOptionsCard(
    variants: List<AccessoryVariant>,
    selectedVariantId: Int,
    onVariantSelect: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Options",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics { heading() },
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                variants.forEach { variant ->
                    val isSelected = variant.id == selectedVariantId
                    val isOrderable = variant.availability.isOrderable
                    FilterChip(
                        selected = isSelected,
                        onClick = { if (isOrderable) onVariantSelect(variant.id) },
                        label = {
                            Text(
                                text = variant.name + if (isOrderable) "" else " · unavailable",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        enabled = isOrderable,
                        modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = EyecareColors.current.accentText,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessorySpecificationRow(
    key: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = key.toAccessorySpecLabel(),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun AccessoryCareGuidance(onNavigateToSupport: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = EyecareColors.current.accentText,
                )
                Text(
                    text = "Use & safety",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = EyecareColors.current.accentText,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(
                text = "Use this product only as directed by your eye-care professional. If you are unsure whether it is right for you, ask the clinic before ordering.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            TextButton(
                onClick = onNavigateToSupport,
                modifier = Modifier.heightIn(min = 48.dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Text("Message the clinic", color = EyecareColors.current.accentText)
            }
        }
    }
}

@Composable
private fun AccessoryDetailBottomBar(
    canAddToCart: Boolean,
    canOrder: Boolean,
    status: AddToCartStatus,
    quantity: Int,
    onQuantityChange: (Int) -> Unit,
    onAddToCart: () -> Unit,
    onNavigateToLinkAccount: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (canOrder && canAddToCart) {
                val quantityControlsEnabled =
                    status != AddToCartStatus.ADDING && status != AddToCartStatus.ADDED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Quantity",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Up to 5 per variant",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            IconButton(
                                onClick = { onQuantityChange(quantity - 1) },
                                enabled = quantity > 1 && quantityControlsEnabled,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Remove,
                                    contentDescription = "Decrease quantity",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            Text(
                                text = quantity.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(28.dp),
                            )
                            IconButton(
                                onClick = { onQuantityChange(quantity + 1) },
                                enabled = quantity < 5 && quantityControlsEnabled,
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Increase quantity",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = if (canOrder) onAddToCart else onNavigateToLinkAccount,
                enabled = if (canOrder) {
                    canAddToCart && status != AddToCartStatus.ADDING && status != AddToCartStatus.ADDED
                } else {
                    true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                shape = RoundedCornerShape(26.dp),
            ) {
                if (!canOrder) {
                    Icon(Icons.Outlined.Lock, contentDescription = null)
                } else {
                    when (status) {
                        AddToCartStatus.ADDING -> CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        AddToCartStatus.ADDED -> Icon(Icons.Outlined.Check, contentDescription = null)
                        else -> Icon(Icons.Outlined.AddShoppingCart, contentDescription = null)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (!canOrder) {
                        "Link account to order"
                    } else {
                        when (status) {
                            AddToCartStatus.ADDING -> "Adding…"
                            AddToCartStatus.ADDED -> "Added to cart"
                            AddToCartStatus.FAILED -> "Try again"
                            AddToCartStatus.IDLE -> if (canAddToCart) "Add to cart" else "Unavailable"
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun AccessoryHeroMedia(
    accessoryName: String,
    images: List<String>,
    pagerState: PagerState,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(if (images.isEmpty()) 1.5f else 1.2f)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (images.isEmpty()) {
            Text(
                text = "Photo coming soon",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                AccessoryDetailImage(
                    imagePath = images[page],
                    contentDescription = "$accessoryName, image ${page + 1} of ${images.size}",
                    accessoryName = accessoryName,
                )
            }
            if (images.size > 1) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.58f),
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AccessoryDetailImage(
    imagePath: String,
    contentDescription: String,
    accessoryName: String,
) {
    val context = LocalContext.current
    val imageUrl = remember(imagePath) { buildImageUrl(imagePath) }
    var retryKey by remember(imageUrl) { mutableStateOf(0) }
    var imageState by remember(imageUrl) {
        mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
    }
    val request = remember(imageUrl, retryKey) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .memoryCacheKey("$imageUrl#accessory-detail-$retryKey")
            .diskCacheKey("$imageUrl#accessory-detail-$retryKey")
            .build()
    }
    val imageLoaded = imageState is AsyncImagePainter.State.Success
    val imageFailed = imageState is AsyncImagePainter.State.Error

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageLoaded) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (imageFailed) "Photo unavailable" else "Loading photo…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (imageFailed) {
                    TextButton(
                        onClick = {
                            imageState = AsyncImagePainter.State.Empty
                            retryKey += 1
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Try again")
                    }
                }
            }
        }
        AsyncImage(
            model = request,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (imageLoaded) 1f else 0f),
            onState = { imageState = it },
        )
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
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun String.toAccessorySpecLabel(): String = when (lowercase(Locale.US)) {
    "volume_ml" -> "Volume (mL)"
    "package_size" -> "Package size"
    else -> replace("_", " ").replaceFirstChar { it.uppercase(Locale.US) }
}
