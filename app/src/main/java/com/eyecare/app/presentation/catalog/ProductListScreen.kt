package com.eyecare.app.presentation.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.Brand
import com.eyecare.app.domain.model.Category
import com.eyecare.app.presentation.catalog.components.ProductCard
import com.eyecare.app.presentation.common.components.ErrorContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: ProductListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        // Title
        Text(
            "Product Catalog",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        // Search bar
        val filters = (uiState as? ProductListUiState.Success)?.filters ?: ProductFilters()
        var query by remember { mutableStateOf(filters.search) }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; viewModel.search(it) },
            placeholder = { Text("Search frames, brands…", color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotBlank()) {
                    IconButton(onClick = { query = ""; viewModel.search("") }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(32.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        PullToRefreshBox(
            isRefreshing = uiState is ProductListUiState.Loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = uiState) {
                is ProductListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading…", style = MaterialTheme.typography.bodyMedium)
                }
                is ProductListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is ProductListUiState.Success -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Filter row
                    item(span = { GridItemSpan(2) }) {
                        FilterRow(
                            brands = state.brands,
                            categories = state.categories,
                            filters = state.filters,
                            onSelectBrand = viewModel::selectBrand,
                            onSelectCategory = viewModel::selectCategory,
                            onSelectSort = viewModel::selectSort,
                            onClearFilters = viewModel::clearFilters,
                        )
                    }

                    if (state.products.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("No products found", style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        items(state.products, key = { it.id }) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onNavigateToDetail(product.id) },
                            )
                        }

                        // Load more trigger
                        if (state.hasMorePages) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.isLoadingMore) {
                                        Text("Loading more…", style = MaterialTheme.typography.bodySmall)
                                    } else {
                                        TextButton(onClick = viewModel::loadMore) {
                                            Text("Load More")
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
}

@Composable
private fun FilterRow(
    brands: List<Brand>,
    categories: List<Category>,
    filters: ProductFilters,
    onSelectBrand: (Int?) -> Unit,
    onSelectCategory: (Int?) -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onClearFilters: () -> Unit,
) {
    val hasActiveFilters = filters.brandId != null || filters.categoryId != null ||
        filters.sort != SortOption.NAME

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Category chips
        if (categories.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = filters.categoryId == null,
                        onClick = { onSelectCategory(null) },
                        label = { Text("All") },
                        shape = RoundedCornerShape(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filters.categoryId == null,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = filters.categoryId == cat.id,
                        onClick = { onSelectCategory(if (filters.categoryId == cat.id) null else cat.id) },
                        label = { Text(cat.name) },
                        shape = RoundedCornerShape(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filters.categoryId == cat.id,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                }
            }
        }

        // Brand + Sort row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Brand dropdown
            if (brands.isNotEmpty()) {
                var brandExpanded by remember { mutableStateOf(false) }
                Box {
                    FilterChip(
                        selected = filters.brandId != null,
                        onClick = { brandExpanded = true },
                        label = {
                            Text(
                                filters.brandId?.let { id -> brands.find { it.id == id }?.name }
                                    ?: "Brand",
                            )
                        },
                        shape = RoundedCornerShape(32.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = filters.brandId != null,
                            borderColor = MaterialTheme.colorScheme.outline,
                            selectedBorderColor = Color.Transparent,
                        ),
                    )
                    DropdownMenu(expanded = brandExpanded, onDismissRequest = { brandExpanded = false }) {
                        DropdownMenuItem(
                            text = { Text("All Brands") },
                            onClick = { onSelectBrand(null); brandExpanded = false },
                        )
                        brands.forEach { brand ->
                            DropdownMenuItem(
                                text = { Text(brand.name) },
                                onClick = { onSelectBrand(brand.id); brandExpanded = false },
                            )
                        }
                    }
                }
            }

            // Sort dropdown
            var sortExpanded by remember { mutableStateOf(false) }
            Box {
                FilterChip(
                    selected = filters.sort != SortOption.NAME,
                    onClick = { sortExpanded = true },
                    label = { Text(filters.sort.label) },
                    shape = RoundedCornerShape(32.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = Color.White,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = filters.sort != SortOption.NAME,
                        borderColor = MaterialTheme.colorScheme.outline,
                        selectedBorderColor = Color.Transparent,
                    ),
                )
                DropdownMenu(expanded = sortExpanded, onDismissRequest = { sortExpanded = false }) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.label) },
                            onClick = { onSelectSort(option); sortExpanded = false },
                        )
                    }
                }
            }

            // Clear filters
            if (hasActiveFilters) {
                TextButton(onClick = onClearFilters) {
                    Text("Clear", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
