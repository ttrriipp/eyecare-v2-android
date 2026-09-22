package com.eyecare.app.presentation.accessories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.eyecare.app.presentation.accessories.components.AccessoryCard
import com.eyecare.app.presentation.accessories.components.AccessoryCatalogControls
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent

@Composable
fun AccessoryCatalogScreen(
    uiState: AccessoryCatalogUiState,
    searchQuery: String,
    currentSort: String?,
    minimumRating: Int?,
    rated: String?,
    onSearchChange: (String) -> Unit,
    onSortChange: (String?) -> Unit,
    onMinimumRatingChange: (Int?) -> Unit,
    onRatedChange: (String?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToAccessory: (Int) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToRequests: () -> Unit,
    canOrder: Boolean = true,
    onNavigateToLinkAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isRefreshing = (uiState as? AccessoryCatalogUiState.Success)?.isRefreshing == true
    val hasLocalFilters = currentSort != null || minimumRating != null || rated != null

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Accessories",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.weight(1f),
                )
                if (canOrder) {
                    IconButton(onClick = onNavigateToRequests) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Order requests",
                        )
                    }
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart")
                    }
                }
            }

            if (!canOrder) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = "Browsing only",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Link your clinic account to order accessories.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(
                            onClick = onNavigateToLinkAccount,
                            contentPadding = PaddingValues(horizontal = 8.dp),
                        ) {
                            Text("Link")
                        }
                    }
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Search accessories",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchChange("") }) {
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
                    .semantics { contentDescription = "Search accessories" },
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when (val state = uiState) {
                    is AccessoryCatalogUiState.Loading -> LoadingContent()
                    is AccessoryCatalogUiState.Error -> ErrorContent(
                        message = state.message,
                        onRetry = onRetry,
                    )
                    is AccessoryCatalogUiState.Empty -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            AccessoryCatalogControls(
                                currentSort = currentSort,
                                onSortChange = onSortChange,
                                minimumRating = minimumRating,
                                onMinimumRatingChange = onMinimumRatingChange,
                                rated = rated,
                                onRatedChange = onRatedChange,
                                onClearFilters = {
                                    onSortChange(null)
                                    onMinimumRatingChange(null)
                                    onRatedChange(null)
                                },
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            AccessoryEmptyState(
                                search = searchQuery,
                                hasLocalFilters = hasLocalFilters,
                                onClearSearch = { onSearchChange("") },
                                onClearFilters = {
                                    onSortChange(null)
                                    onMinimumRatingChange(null)
                                    onRatedChange(null)
                                },
                            )
                        }
                    }
                    is AccessoryCatalogUiState.Success -> {
                        val gridState = rememberLazyGridState()
                        val shouldLoadMore by remember {
                            derivedStateOf {
                                val layoutInfo = gridState.layoutInfo
                                val total = layoutInfo.totalItemsCount
                                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                                total > 0 && lastVisible >= total - 3
                            }
                        }
                        LaunchedEffect(
                            shouldLoadMore,
                            state.hasMorePages,
                            state.isLoadingMore,
                            state.loadMoreError,
                        ) {
                            if (shouldLoadMore && state.hasMorePages &&
                                !state.isLoadingMore && state.loadMoreError == null
                            ) {
                                onLoadMore()
                            }
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            state = gridState,
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = 96.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item(span = { GridItemSpan(2) }) {
                                AccessoryCatalogControls(
                                    currentSort = currentSort,
                                    onSortChange = onSortChange,
                                    minimumRating = minimumRating,
                                    onMinimumRatingChange = onMinimumRatingChange,
                                    rated = rated,
                                    onRatedChange = onRatedChange,
                                    onClearFilters = {
                                        onSortChange(null)
                                        onMinimumRatingChange(null)
                                        onRatedChange(null)
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                )
                            }

                            items(state.items, key = { it.id }) { accessory ->
                                AccessoryCard(
                                    accessory = accessory,
                                    onClick = { onNavigateToAccessory(accessory.id) },
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
                                            modifier = Modifier.size(24.dp),
                                        )
                                    }
                                }
                            } else if (state.loadMoreError != null) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        TextButton(onClick = onLoadMore) { Text("Retry") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccessoryEmptyState(
    search: String,
    hasLocalFilters: Boolean,
    onClearSearch: () -> Unit,
    onClearFilters: () -> Unit,
) {
    when {
        search.isNotBlank() -> EmptyContent(
            message = "No accessories match “$search”.",
            actionLabel = "Clear search",
            onAction = onClearSearch,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
        hasLocalFilters -> EmptyContent(
            message = "No accessories match these filters.",
            actionLabel = "Clear filters",
            onAction = onClearFilters,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
        else -> EmptyContent(
            message = "No accessories are available right now.",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(horizontal = 16.dp),
        )
    }
}
