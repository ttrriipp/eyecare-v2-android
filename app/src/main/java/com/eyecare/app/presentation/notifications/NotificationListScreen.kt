package com.eyecare.app.presentation.notifications

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.AppNotification
import com.eyecare.app.domain.model.NotificationKind
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.EmptyContent
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.common.components.LoadingContent
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListScreen(
    uiState: NotificationListUiState,
    unreadCount: Int,
    onBack: () -> Unit,
    onNotificationTap: (AppNotification) -> Unit,
    onMarkAllRead: () -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDismissMessage: () -> Unit = {},
) {
    val successState = uiState as? NotificationListUiState.Success
    val snackbarHostState = remember { SnackbarHostState() }
    var showMarkAllDialog by remember { mutableStateOf(false) }

    LaunchedEffect(successState?.infoMessage) {
        val message = successState?.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onDismissMessage()
    }

    if (showMarkAllDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.DoneAll,
            title = "Mark all as read?",
            message = "Every notification in this list will be marked as read.",
            confirmLabel = "Mark all read",
            dismissLabel = "Cancel",
            onConfirm = {
                showMarkAllDialog = false
                onMarkAllRead()
            },
            onDismissRequest = { showMarkAllDialog = false },
        )
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Text(if (unreadCount > 0) "Notifications ($unreadCount unread)" else "Notifications")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (successState != null) {
                        val hasUnread = unreadCount > 0
                        val isMutating = successState.mutationInFlight.isNotEmpty() || successState.isMarkAllInFlight
                        if (hasUnread) {
                            TextButton(
                                onClick = { showMarkAllDialog = true },
                                enabled = !isMutating,
                            ) {
                                Text("Mark all read")
                            }
                        }
                    }
                },
            )

            val isRefreshing = uiState is NotificationListUiState.Loading || successState?.isRefreshing == true

            Box(Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    when (val state = uiState) {
                        is NotificationListUiState.Loading -> LoadingContent()
                        is NotificationListUiState.Error -> ErrorContent(
                            message = state.patientSafeMessage,
                            onRetry = onRetry,
                        )
                        is NotificationListUiState.Success -> {
                            if (state.notifications.isEmpty()) {
                                EmptyContent(message = "You're all caught up.")
                            } else {
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { data ->
            Snackbar(snackbarData = data)
        }
    }
}

@Composable
private fun NotificationList(
    state: NotificationListUiState.Success,
    onNotificationTap: (AppNotification) -> Unit,
    onLoadMore: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState, state.canLoadMore, state.isLoadingMore, state.notifications.size) {
        snapshotFlow {
            listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.collect { lastVisibleIndex ->
            val loadThreshold = (state.notifications.size - 3).coerceAtLeast(0)
            if (
                state.canLoadMore &&
                !state.isLoadingMore &&
                lastVisibleIndex >= loadThreshold
            ) {
                onLoadMore()
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                        .padding(vertical = 8.dp),
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
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Load more",
                        style = MaterialTheme.typography.bodySmall,
                        color = EyecareColors.current.accentText,
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

    Surface(
        onClick = onTap,
        enabled = !isMutating,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NotificationKindBadge(notification.kind)

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = notification.title,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isUnread) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .semantics { contentDescription = "Unread" },
                        ) {
                            val dotColor = MaterialTheme.colorScheme.primary
                            Canvas(Modifier.fillMaxSize()) {
                                drawCircle(color = dotColor)
                            }
                        }
                    }
                }
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
                CircularProgressIndicator(Modifier.size(16.dp).align(Alignment.CenterVertically))
            }
        }
    }
}

@Composable
private fun NotificationKindBadge(kind: NotificationKind) {
    val icon: ImageVector = when (kind) {
        NotificationKind.NEW_MESSAGE -> Icons.AutoMirrored.Outlined.Chat
        NotificationKind.UNKNOWN -> Icons.Outlined.Notifications
    }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.size(36.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EyecareColors.current.accentText,
                modifier = Modifier.size(20.dp),
            )
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
            else -> instant.atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    } catch (_: Exception) {
        createdAt
    }
}
