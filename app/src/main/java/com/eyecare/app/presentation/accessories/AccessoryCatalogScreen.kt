package com.eyecare.app.presentation.accessories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.accessories.components.AccessoryCard
import com.eyecare.app.presentation.accessories.components.AccessoryCatalogControls
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessoryCatalogScreen(
    uiState: AccessoryCatalogUiState,
    searchQuery: String,
    currentSort: String?,
    onSearchChange: (String) -> Unit,
    onSortChange: (String?) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onNavigateToAccessory: (Int) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToRequests: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isRefreshing = (uiState as? AccessoryCatalogUiState.Success)?.isRefreshing == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessories", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onNavigateToCart) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Cart")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        modifier = modifier,
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val state = uiState) {
                is AccessoryCatalogUiState.Loading -> LoadingContent()
                is AccessoryCatalogUiState.Empty -> {
                    AccessoryCatalogControls(
                        searchQuery = searchQuery,
                        onSearchChange = onSearchChange,
                        currentSort = currentSort,
                        onSortChange = onSortChange,
                    )
                    EmptyContent(
                        message = "No accessories found. Try a different search or filter.",
                    )
                }
                is AccessoryCatalogUiState.Error -> ErrorContent(
                    message = state.message,
                    onRetry = onRetry,
                )
                is AccessoryCatalogUiState.Success -> {
                    val listState = rememberLazyListState()
                    val shouldLoadMore by remember {
                        derivedStateOf {
                            val layoutInfo = listState.layoutInfo
                            val total = layoutInfo.totalItemsCount
                            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                            total > 0 && lastVisible >= total - 3
                        }
                    }
                    LaunchedEffect(shouldLoadMore, state.hasMorePages, state.isLoadingMore, state.loadMoreError) {
                        if (shouldLoadMore && state.hasMorePages && !state.isLoadingMore && state.loadMoreError == null) {
                            onLoadMore()
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 8.dp,
                            bottom = 24.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "controls") {
                            AccessoryCatalogControls(
                                searchQuery = searchQuery,
                                onSearchChange = onSearchChange,
                                currentSort = currentSort,
                                onSortChange = onSortChange,
                            )
                        }
                        items(state.items, key = { it.id }) { accessory ->
                            AccessoryCard(
                                accessory = accessory,
                                onClick = { onNavigateToAccessory(accessory.id) },
                            )
                        }
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                        if (state.loadMoreError != null) {
                            item {
                                Box(
                                    Modifier
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