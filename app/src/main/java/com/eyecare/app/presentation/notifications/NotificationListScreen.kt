package com.eyecare.app.presentation.notifications

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppNotification
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    uiState: NotificationListUiState,
    onBack: () -> Unit,
    onNotificationTap: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(bottom = 0.dp),
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Notifications") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (uiState is NotificationListUiState.Success) {
                    val hasUnread = uiState.notifications.any { it.readAt == null }
                    val isMutating = uiState.mutationInFlight.isNotEmpty()
                    if (hasUnread) {
                        TextButton(
                            onClick = onMarkAllRead,
                            enabled = !isMutating,
                        ) {
                            Text("Mark all read")
                        }
                    }
                }
            },
        )

        Box(Modifier.weight(1f)) {
            when (val state = uiState) {
                is NotificationListUiState.Loading -> LoadingContent()
                is NotificationListUiState.Error -> ErrorContent(
                    message = state.patientSafeMessage,
                    onRetry = onRetry,
                )
                is NotificationListUiState.Success -> {
                    if (state.notifications.isEmpty()) {
                        EmptyContent(message = "No notifications yet.")
                    } else {
                        PullToRefreshBox(
                            isRefreshing = false,
                            onRefresh = onRefresh,
                        ) {
                            NotificationList(
                                state = state,
                                onNotificationTap = onNotificationTap,
                                onLoadMore = onLoadMore,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationList(
    state: NotificationListUiState.Success,
    onNotificationTap: (AppNotification) -> Unit,
    onLoadMore: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = state.notifications,
            key = { it.id },
        ) { notification ->
            NotificationRow(
                notification = notification,
                isMutating = notification.id in state.mutationInFlight,
                onTap = { onNotificationTap(notification) },
            )
        }

        if (state.isLoadingMore) {
            item(key = "__loading_more__") {
                Box(
                    Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }

        if (state.inlineError != null && !state.isLoadingMore) {
            item(key = "__inline_error__") {
                Box(
                    Modifier.fillMaxWidth()
                        .clickable { onLoadMore() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.inlineError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (state.canLoadMore && !state.isLoadingMore && state.inlineError == null) {
            item(key = "__load_more_trigger__") {
                Box(
                    Modifier.fillMaxWidth()
                        .clickable { onLoadMore() }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Load more",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(
    notification: AppNotification,
    isMutating: Boolean,
    onTap: () -> Unit,
) {
    val isUnread = notification.readAt == null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isMutating, onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (isUnread) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .padding(0.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.Center),
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = androidx.compose.ui.graphics.Color(0xFF1976D2))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "Unread",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = formatRelativeTime(notification.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (isMutating) {
            Spacer(Modifier.width(8.dp))
            CircularProgressIndicator(Modifier.size(16.dp).align(Alignment.CenterVertically))
        }
    }
}

private fun formatRelativeTime(createdAt: String): String {
    return try {
        val instant = java.time.Instant.parse(createdAt)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        when {
            duration.toMinutes() < 1 -> "Just now"
            duration.toHours() < 1 -> "${duration.toMinutes()}m ago"
            duration.toDays() < 1 -> "${duration.toHours()}h ago"
            duration.toDays() < 7 -> "${duration.toDays()}d ago"
            else -> createdAt.substringBefore("T")
        }
    } catch (_: Exception) {
        createdAt
    }
}
