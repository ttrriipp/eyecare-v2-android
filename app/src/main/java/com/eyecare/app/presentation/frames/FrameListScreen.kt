package com.eyecare.app.presentation.frames

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.frames.components.FrameCard
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrameListScreen(
    onNavigateToDetail: (Int) -> Unit,
    viewModel: FrameListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp),
    ) {
        Text(
            "Frames",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        val filters = (uiState as? FrameListUiState.Success)?.filters ?: FrameListFilters()
        var query by remember { mutableStateOf(filters.search) }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it; viewModel.search(it) },
            placeholder = {
                Text(
                    "Search frames",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
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
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        PullToRefreshBox(
            isRefreshing = uiState is FrameListUiState.Loading,
            onRefresh = viewModel::refresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            when (val state = uiState) {
                is FrameListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading\u2026", style = MaterialTheme.typography.bodyMedium)
                }
                is FrameListUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::refresh)
                is FrameListUiState.Success -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(span = { GridItemSpan(2) }) {
                        FrameSortBar(
                            currentSort = state.filters.sort,
                            onSelectSort = viewModel::selectSort,
                        )
                    }

                    if (state.frames.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                Modifier.fillMaxWidth().padding(vertical = 48.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "No frames found",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    items(state.frames, key = { it.id }) { frame ->
                        FrameCard(
                            frame = frame,
                            onClick = { onNavigateToDetail(frame.id) },
                        )
                    }

                    if (state.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("Loading more\u2026", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (state.hasMorePages && !state.isLoadingMore) {
                        item(span = { GridItemSpan(2) }) {
                            Box(
                                Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                androidx.compose.material3.TextButton(onClick = viewModel::loadMore) {
                                    Text("Load more")
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
private fun FrameSortBar(
    currentSort: FrameSortOption,
    onSelectSort: (FrameSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
        SingleChoiceSegmentedButtonRow {
            FrameSortOption.entries.forEach { option ->
                SegmentedButton(
                    selected = currentSort == option,
                    onClick = { onSelectSort(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = option.ordinal,
                        count = FrameSortOption.entries.size,
                    ),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        activeContentColor = EyecareColors.current.accentText,
                        activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        inactiveContainerColor = MaterialTheme.colorScheme.surface,
                        inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                    label = { Text(option.label) },
                )
            }
        }
    }
}
