package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.presentation.common.components.ErrorContent
import com.eyecare.app.presentation.messaging.components.AttachmentPreview
import com.eyecare.app.presentation.messaging.components.MessageBubble
import com.eyecare.app.ui.theme.EyecareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onMessagesMarkedRead: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ChatEffect.MessagesMarkedRead -> onMessagesMarkedRead()
            }
        }
    }

    // Lifecycle-aware polling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.setScreenVisible(true)
                Lifecycle.Event.ON_PAUSE -> viewModel.setScreenVisible(false)
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            viewModel.setScreenVisible(false)
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // File picker — images + documents
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getString(idx) else "file"
        } ?: "file"
        val fileSize = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            cursor.moveToFirst()
            if (idx >= 0) cursor.getLong(idx) else 0L
        } ?: 0L
        viewModel.setPendingAttachment(PendingAttachment(uri, mimeType, fileName, fileSize))
    }

    val successState = uiState as? ChatUiState.Success

    // Auto-scroll to newest message on initial load
    val messages = successState?.messages
    var hasScrolledToBottom by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(messages?.size) {
        val size = messages?.size ?: return@LaunchedEffect
        if (size > 0 && !hasScrolledToBottom) {
            listState.scrollToItem(size - 1)
            hasScrolledToBottom = true
        }
    }

    // Older-page prepend viewport anchoring
    val isLoadingOlder = successState?.isLoadingOlder == true
    OlderPageAnchorEffect(listState, messages?.size, isLoadingOlder)

    // Load-older trigger when reaching the top
    LoadOlderTrigger(listState, successState, viewModel::loadOlder)

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0),
            title = { Text("Messages") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (successState != null && successState.searchState == null) {
                    IconButton(onClick = { viewModel.openSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "Search messages")
                    }
                }
            },
        )

        Box(Modifier.weight(1f)) {
            when (val state = uiState) {
                is ChatUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ChatUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::retry)
                is ChatUiState.Success -> {
                    val search = state.searchState
                    if (search != null) {
                        MessageSearchContent(
                            searchState = search,
                            searchDraft = state.searchDraft,
                            currentUserId = state.currentUserId,
                            onDraftChanged = viewModel::onSearchQueryChanged,
                            onSubmit = viewModel::submitSearch,
                            onClose = viewModel::closeSearch,
                            onLoadMore = viewModel::loadMoreSearchResults,
                        )
                    } else {
                        ChatContent(
                            state = state,
                            listState = listState,
                            onRetryLoadOlder = viewModel::retryLoadOlder,
                        )
                    }
                }
            }
        }

        // Pending attachment preview (hidden during search)
        if (successState?.searchState == null) {
            successState?.pendingAttachment?.let { attachment ->
                AttachmentPreview(
                    attachment = attachment,
                    error = successState.attachmentError,
                    onRemove = { viewModel.setPendingAttachment(null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            // Send error message
            successState?.sendError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                )
            }

            // Input bar
            val isSending = successState?.isSending == true
            val inputText = successState?.inputText ?: ""
            val canUpload = successState?.conversation?.let {
                it.accessLevel == ConversationAccessLevel.LINKED_PATIENT && it.capabilities.canUploadAttachments
            } == true

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // "+" attachment button — only shown when upload is allowed
                if (canUpload) {
                    Surface(
                        onClick = {
                            filePicker.launch(arrayOf("image/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        enabled = !isSending,
                        modifier = Modifier.size(44.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = EyecareColors.current.accentText)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.onDraftChanged(it) },
                    placeholder = { Text("Type a message…") },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    ),
                    maxLines = 4,
                    modifier = Modifier.weight(1f),
                    enabled = !isSending,
                )
                Spacer(Modifier.width(8.dp))

                val hasPendingAttachment = successState?.pendingAttachment != null && successState.attachmentError == null
                val canSend = (inputText.isNotBlank() || hasPendingAttachment) && !isSending

                Surface(
                    onClick = {
                        when {
                            hasPendingAttachment -> viewModel.sendPendingAttachment()
                            else -> viewModel.sendMessage()
                        }
                    },
                    shape = CircleShape,
                    color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    enabled = canSend,
                    modifier = Modifier.size(48.dp),
                ) {
                    val sendContentColor = if (canSend) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Box(contentAlignment = Alignment.Center) {
                        if (isSending) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = sendContentColor)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = sendContentColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatContent(
    state: ChatUiState.Success,
    listState: LazyListState,
    onRetryLoadOlder: () -> Unit,
) {
    if (state.messages.isEmpty() && !state.isLoadingOlder && state.olderPageError == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No messages yet. Say hello!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        ) {
            // Top loading/error/retry states
            if (state.isLoadingOlder) {
                item(key = "__loading_older__") {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    }
                }
            }
            if (state.olderPageError != null) {
                item(key = "__older_error__") {
                    Box(
                        Modifier.fillMaxWidth()
                            .clickable { onRetryLoadOlder() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            state.olderPageError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            items(state.messages, key = { it.id }) { msg ->
                val isOwn = state.currentUserId?.let { it == msg.senderId }
                    ?: (msg.senderType == SenderType.PATIENT)
                MessageBubble(
                    message = msg,
                    isOwn = isOwn,
                    conversationAccessLevel = state.conversation.accessLevel,
                )
            }
        }
    }
}

@Composable
private fun OlderPageAnchorEffect(
    listState: LazyListState,
    messageCount: Int?,
    isLoadingOlder: Boolean,
) {
    var anchorIndex by remember { mutableIntStateOf(0) }
    var anchorOffset by remember { mutableIntStateOf(0) }
    var anchorCount by remember { mutableIntStateOf(0) }
    var wasLoadingOlder by remember { mutableStateOf(false) }

    // Capture anchor before prepend starts
    if (isLoadingOlder && !wasLoadingOlder) {
        anchorIndex = listState.firstVisibleItemIndex
        anchorOffset = listState.firstVisibleItemScrollOffset
        anchorCount = messageCount ?: 0
    }
    wasLoadingOlder = isLoadingOlder

    // Restore anchor after prepend completes
    LaunchedEffect(messageCount, isLoadingOlder) {
        if (!isLoadingOlder && anchorCount > 0 && messageCount != null && messageCount > anchorCount) {
            val inserted = messageCount - anchorCount
            listState.scrollToItem(anchorIndex + inserted, anchorOffset)
            anchorCount = 0
        }
    }
}

@Composable
private fun LoadOlderTrigger(
    listState: LazyListState,
    state: ChatUiState.Success?,
    loadOlder: () -> Unit,
) {
    LaunchedEffect(listState, state?.hasMore, state?.isLoadingOlder, state?.olderPageError) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                val current = state
                if (index == 0 && current != null && current.hasMore && !current.isLoadingOlder && current.olderPageError == null) {
                    loadOlder()
                }
            }
    }
}
