package com.eyecare.app.presentation.messaging

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.SenderType

@Composable
fun MessageSearchContent(
    searchState: SearchState,
    searchDraft: String,
    currentUserId: Int?,
    onDraftChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onClose: () -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    Column(modifier.fillMaxSize()) {
        SearchBar(
            query = searchDraft,
            onQueryChanged = onDraftChanged,
            onSubmit = onSubmit,
            onBack = onClose,
        )

        when {
            searchState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            searchState.error != null && searchState.results.isEmpty() -> {
                SearchErrorOrEmpty(
                    message = searchState.error,
                    isRetry = true,
                    onAction = onSubmit,
                )
            }
            searchState.query.isBlank() && searchState.results.isEmpty() -> {
                SearchErrorOrEmpty(
                    message = "Enter at least 3 characters to search.",
                    isRetry = false,
                    onAction = null,
                )
            }
            searchState.results.isEmpty() -> {
                SearchErrorOrEmpty(
                    message = "No results found.",
                    isRetry = false,
                    onAction = null,
                )
            }
            else -> {
                SearchResultList(
                    results = searchState.results,
                    currentUserId = currentUserId,
                    listState = listState,
                    hasMore = searchState.hasMore,
                    isLoadingMore = searchState.isLoadingMore,
                    loadMoreError = if (searchState.error != null && searchState.results.isNotEmpty()) searchState.error else null,
                    onLoadMore = onLoadMore,
                    onRetryLoadMore = onLoadMore,
                )
            }
        }
    }

    // Load-more trigger when scrolling near the end
    LoadMoreSearchTrigger(listState, searchState, onLoadMore)
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search")
        }
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search messages…") },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            trailingIcon = {
                IconButton(onClick = onSubmit) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SearchResultList(
    results: List<Message>,
    currentUserId: Int?,
    listState: LazyListState,
    hasMore: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: String?,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
    ) {
        items(results, key = { "search-${it.id}" }) { msg ->
            SearchResultRow(message = msg, currentUserId = currentUserId)
        }

        if (isLoadingMore) {
            item(key = "__search_loading_more__") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }

        if (loadMoreError != null) {
            item(key = "__search_load_more_error__") {
                Box(
                    Modifier.fillMaxWidth()
                        .clickable { onRetryLoadMore() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        loadMoreError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!hasMore && !isLoadingMore && loadMoreError == null) {
            item(key = "__search_end__") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "End of results",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(
    message: Message,
    currentUserId: Int?,
) {
    val isOwn = currentUserId?.let { it == message.senderId }
        ?: (message.senderType == SenderType.PATIENT)
    val senderLabel = if (isOwn) "You" else "Staff"

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                senderLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                message.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
            )
            if (message.attachments.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                message.attachments.forEach { attachment ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            attachment.originalName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                message.createdAt.take(16).replace("T", " "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End),
            )
        }
    }
}

@Composable
private fun SearchErrorOrEmpty(
    message: String,
    isRetry: Boolean,
    onAction: (() -> Unit)?,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (onAction != null) Modifier.clickable { onAction() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isRetry) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isRetry) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tap to retry",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LoadMoreSearchTrigger(
    listState: LazyListState,
    searchState: SearchState,
    onLoadMore: () -> Unit,
) {
    LaunchedEffect(listState, searchState.hasMore, searchState.isLoadingMore, searchState.error) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (totalItems > 0 && lastVisible >= totalItems - 3 &&
                    searchState.hasMore && !searchState.isLoadingMore &&
                    searchState.error == null
                ) {
                    onLoadMore()
                }
            }
    }
}
