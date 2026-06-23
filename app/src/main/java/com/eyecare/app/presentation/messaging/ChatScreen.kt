package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.presentation.common.components.ErrorContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.presentation.messaging.components.AttachmentPreview
import com.eyecare.app.presentation.messaging.components.AttachmentSheet
import com.eyecare.app.presentation.messaging.components.ContextCard
import com.eyecare.app.presentation.messaging.components.MessageBubble
import com.eyecare.app.presentation.common.components.ErrorContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var showSheet by remember { mutableStateOf(false) }

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

    // Auto-scroll to latest message
    val messages = (uiState as? ChatUiState.Success)?.messages
    LaunchedEffect(messages?.size) {
        val size = messages?.size ?: return@LaunchedEffect
        if (size > 0) listState.animateScrollToItem(size - 1)
    }

    if (showSheet) {
        val state = uiState as? ChatUiState.Success
        AttachmentSheet(
            sheetState = sheetState,
            appointments = state?.appointments ?: emptyList(),
            orders = state?.orders ?: emptyList(),
            onAttachFile = { filePicker.launch(arrayOf("image/*", "application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
            onLinkAppointment = { viewModel.setPendingContext(PendingContext.AppointmentContext(it)) },
            onLinkOrder = { viewModel.setPendingContext(PendingContext.OrderContext(it)) },
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showSheet = false } },
        )
    }

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
        )

        Box(Modifier.weight(1f)) {
            when (val state = uiState) {
                is ChatUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ChatUiState.Error -> ErrorContent(message = state.message, onRetry = viewModel::retry)
                is ChatUiState.Success -> {
                    if (state.messages.isEmpty()) {
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
                            items(state.messages, key = { it.id }) { msg ->
                                MessageBubble(message = msg, isOwn = msg.senderId == viewModel.currentUserId)
                            }
                        }
                    }
                }
            }
        }

        // Pending attachment / context previews
        val successState = uiState as? ChatUiState.Success
        successState?.pendingAttachment?.let { attachment ->
            AttachmentPreview(
                attachment = attachment,
                error = successState.attachmentError,
                onRemove = { viewModel.setPendingAttachment(null) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
        successState?.pendingContext?.let { ctx ->
            ContextCard(
                context = ctx,
                onRemove = { viewModel.setPendingContext(null) },
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }

        // Input bar
        val isSending = successState?.isSending == true
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // "+" attachment button
            Surface(
                onClick = {
                    viewModel.loadPickerData()
                    showSheet = true
                },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Add attachment", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Type a message…") },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                maxLines = 4,
                modifier = Modifier.weight(1f),
                enabled = !isSending,
            )
            Spacer(Modifier.width(8.dp))
            val hasPendingAttachment = successState?.pendingAttachment != null && successState.attachmentError == null
            val hasPendingContext = successState?.pendingContext != null
            val canSend = (inputText.isNotBlank() || hasPendingAttachment || hasPendingContext) && !isSending
            Surface(
                onClick = {
                    when {
                        hasPendingAttachment -> viewModel.sendPendingAttachment()
                        hasPendingContext -> viewModel.sendContextMessage()
                        else -> {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    }
                },
                shape = CircleShape,
                color = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSending) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White)
                    else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}


