package com.eyecare.app.presentation.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.isTypedArReady
import com.eyecare.app.presentation.common.RefreshOnResumeEffect
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.frames.components.FrameCatalogControls
import com.eyecare.app.presentation.frames.components.FrameCard
import com.eyecare.app.presentation.frames.components.FrameLoadingGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameListScreen(
    onNavigateToDetail: (Int) -> Unit,
    onNavigateToTryOn: (frameId: Int, variantId: Int) -> Unit,
    viewModel: FrameListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val message = (uiState as? FrameListUiState.Success)?.message

    RefreshOnResumeEffect(onRefresh = viewModel::refresh)

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            Text(
                text = "Frames",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            val filters = (uiState as? FrameListUiState.Success)?.filters ?: FrameListFilters()
            var query by rememberSaveable { mutableStateOf(filters.search) }

            LaunchedEffect(filters.search) {
                if (query != filters.search) query = filters.search
            }

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    viewModel.search(it)
                },
                placeholder = {
                    Text(
                        "Search frames",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(
                            onClick = {
                                query = ""
                                viewModel.clearSearch()
                            },
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { contentDescription = "Search frames" },
            )

            PullToRefreshBox(
                isRefreshing = uiState is FrameListUiState.Loading ||
                    (uiState as? FrameListUiState.Success)?.isRefreshing == true,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val state = uiState) {
                    is FrameListUiState.Loading -> FrameLoadingGrid(Modifier.fillMaxSize())
                    is FrameListUiState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = viewModel::refresh,
                    )
                    is FrameListUiState.Success -> {
                        val brands = state.frames
                            .map { it.brand.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
                        val categories = state.frames
                            .map { it.category.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()
                        val frames = if (state.isRefreshing) state.frames else state.visibleFrames

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                FrameCatalogControls(
                                    filters = state.filters,
                                    brands = brands,
                                    categories = categories,
                                    onSelectBrand = viewModel::selectBrand,
                                    onSelectCategory = viewModel::selectCategory,
                                    onSetArOnly = viewModel::setArOnly,
                                    onClearFilters = viewModel::clearCatalogFilters,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                )
                            }

                            if (frames.isEmpty() && !state.isRefreshing) {
                                item(span = { GridItemSpan(2) }) {
                                    CatalogEmptyState(
                                        search = state.filters.search,
                                        hasLocalFilters = state.filters.hasLocalFilters,
                                        onClearSearch = {
                                            query = ""
                                            viewModel.clearSearch()
                                        },
                                        onClearFilters = viewModel::clearCatalogFilters,
                                    )
                                }
                            }

                            items(frames, key = { it.id }) { frame ->
                                FrameCard(
                                    frame = frame,
                                    onClick = { onNavigateToDetail(frame.id) },
                                    onTryOn = frame.variants
                                        .firstOrNull { it.isTypedArReady }
                                        ?.let { variant ->
                                            { onNavigateToTryOn(frame.id, variant.id) }
                                        },
                                )
                            }

                            if (state.isLoadingMore) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .semantics { contentDescription = "Loading more frames" }
                                                .padding(end = 8.dp),
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            } else if (state.hasMorePages) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TextButton(
                                            onClick = viewModel::loadMore,
                                            modifier = Modifier.heightIn(min = 48.dp),
                                        ) {
                                            Text("Load more frames")
                                        }
                                    }
                                }
                            }
                        }
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
private fun CatalogEmptyState(
    search: String,
    hasLocalFilters: Boolean,
    onClearSearch: () -> Unit,
    onClearFilters: () -> Unit,
) {
    when {
        search.isNotBlank() -> EmptyContent(
            message = "No frames match “$search”.",
            actionLabel = "Clear search",
            onAction = onClearSearch,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
        hasLocalFilters -> EmptyContent(
            message = "No frames match these filters.",
            actionLabel = "Clear filters",
            onAction = onClearFilters,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
        else -> EmptyContent(
            message = "No frames are available right now.",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
    }
}
