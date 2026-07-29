package com.eyecare.app.presentation.frames

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.model.isArReady
import com.eyecare.app.presentation.common.buildImageUrl
import com.eyecare.app.presentation.common.components.ErrorContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FrameDetailScreen(
    frameId: Int,
    onBack: () -> Unit,
    onNavigateToAr: (frameId: Int, variantId: Int) -> Unit,
    onNavigateToReserve: (frameId: Int, variantId: Int) -> Unit,
) {
    val viewModel = hiltViewModel<FrameDetailViewModel, FrameDetailViewModel.Factory> { it.create(frameId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
        )

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
                    ) {
                        val images = selected.images.ifEmpty { frame.images }
                        val pagerState = rememberPagerState { images.size.coerceAtLeast(1) }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(if (images.isEmpty()) 1.65f else 1f)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (images.isEmpty()) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.FaceRetouchingNatural,
                                            contentDescription = null,
                                            modifier = Modifier.padding(16.dp).size(32.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "Photo coming soon",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            } else {
                                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                                    AsyncImage(
                                        model = buildImageUrl(images[page]),
                                        contentDescription = frame.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
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

                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                frame.brand.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                frame.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                            )

                            if (frame.variants.size > 1) {
                                Spacer(Modifier.height(16.dp))
                                Text("Options", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(8.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    frame.variants.forEach { variant ->
                                        val isSelected = variant.id == selected.id
                                        Surface(
                                            onClick = { viewModel.selectVariant(variant) },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outlineVariant,
                                                    shape = RoundedCornerShape(12.dp),
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                        ) {
                                            Text(
                                                variant.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))
                            Text(
                                "₱${String.format("%.2f", selected.price)}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                            )

                            selected.compareAtPrice?.let { original ->
                                Text(
                                    "₱${String.format("%.2f", original)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            Spacer(Modifier.height(16.dp))
                            DetailFactRow("Option", selected.name)
                            selected.sku.takeIf(String::isNotBlank)?.let { DetailFactRow("SKU", it) }
                            frame.category.takeIf(String::isNotBlank)?.let { DetailFactRow("Category", it) }

                            frame.description?.takeIf(String::isNotBlank)?.let { desc ->
                                Spacer(Modifier.height(16.dp))
                                Text("About", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Text(desc, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (selected.isArReady) {
                            OutlinedButton(
                                onClick = { onNavigateToAr(frame.id, selected.id) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Outlined.FaceRetouchingNatural, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Try AR")
                            }
                        }
                        Button(
                            onClick = { onNavigateToReserve(frame.id, selected.id) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Text("Reserve")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailFactRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
