package com.eyecare.app.presentation.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eyecare.app.BuildConfig
import com.eyecare.app.domain.model.ProductVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Int,
    onBack: () -> Unit,
    onNavigateToAr: (productId: Int, variantId: Int) -> Unit,
    onNavigateToOrder: (productId: Int, variantId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<ProductDetailViewModel, ProductDetailViewModel.Factory> { it.create(productId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

        when (val state = uiState) {
            is ProductDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is ProductDetailUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
            is ProductDetailUiState.Success -> {
                val product = state.product
                val selected = state.selectedVariant

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    // Image pager
                    val images = product.images.sortedWith(compareByDescending { it.isPrimary })
                    val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        if (images.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No image", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                val ref = images[page].path
                                val url = if (ref.startsWith("http")) ref else "${BuildConfig.API_BASE_URL}storage/$ref"
                                AsyncImage(
                                    model = url,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            // Page indicator dots
                            if (images.size > 1) {
                                Row(
                                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    repeat(images.size) { i ->
                                        Box(
                                            modifier = Modifier
                                                .size(if (i == pagerState.currentPage) 8.dp else 6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (i == pagerState.currentPage) MaterialTheme.colorScheme.primary
                                                    else Color.White.copy(alpha = 0.6f)
                                                ),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                        // Brand + Name
                        Text(
                            product.brand.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            product.name,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$${selected.price}",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )

                        // Description
                        if (!product.description.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(product.description, style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Dimensions
                        if (!product.dimensions.isNullOrBlank()) {
                            Spacer(Modifier.height(8.dp))
                            Text("Dimensions: ${product.dimensions}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Variant selector
                        if (product.variants.size > 1) {
                            Spacer(Modifier.height(20.dp))
                            Text("Select Variant", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(product.variants) { variant ->
                                    VariantChip(
                                        variant = variant,
                                        isSelected = variant.id == selected.id,
                                        onClick = { viewModel.selectVariant(variant) },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(28.dp))

                        // Try AR button — only when variant is AR eligible
                        if (selected.arEligible) {
                            Button(
                                onClick = { onNavigateToAr(product.id, selected.id) },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(26.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            ) {
                                Icon(Icons.Outlined.FaceRetouchingNatural, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Try with AR", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Order button
                        OutlinedButton(
                            onClick = { onNavigateToOrder(product.id, selected.id) },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Outlined.ShoppingBag, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text("Order this frame", color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(96.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun VariantChip(variant: ProductVariant, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White,
        modifier = Modifier.border(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            RoundedCornerShape(12.dp),
        ),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(variant.name, style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
            Text("$${variant.price}", style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
