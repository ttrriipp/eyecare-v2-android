package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 5_000L

data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val fileSize: Long,
)

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val conversation: Conversation,
        val messages: List<Message>,
        val currentUserId: Int? = null,
        val isSending: Boolean = false,
        val pendingAttachment: PendingAttachment? = null,
        val attachmentError: String? = null,
        val sendError: String? = null,
        val nextCursor: String? = null,
        val hasMore: Boolean = false,
        val isLoadingOlder: Boolean = false,
        val olderPageError: String? = null,
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var timelineState = MessageTimeline.State()
    private var isScreenVisible = false
    private var pollingJob: Job? = null
    private var isLoadingOlderGuard = false

    init { load() }

    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (visible && pollingJob == null) {
            startPolling()
        } else if (!visible) {
            pollingJob?.cancel()
            pollingJob = null
        }
    }

    private fun startPolling() {
        pollingJob = viewModelScope.launch {
            while (isScreenVisible) {
                delay(POLL_INTERVAL_MS)
                poll()
            }
        }
    }

    private suspend fun poll() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        chatRepository.getMessages().fold(
            onSuccess = { page ->
                val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                val polledMessages = page.messages
                if (MessageTimeline.hasNewMessages(timelineState, polledMessages)) {
                    timelineState = MessageTimeline.merge(timelineState, polledMessages)
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                    )
                }
            },
            onFailure = {
                // Poll failures preserve usable messages; no full-screen error
            },
        )
    }

    fun retry() = load()

    fun loadOlder() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        if (!current.hasMore || current.isLoadingOlder || isLoadingOlderGuard) return
        val cursor = current.nextCursor ?: return

        isLoadingOlderGuard = true
        _uiState.value = current.copy(isLoadingOlder = true, olderPageError = null)

        viewModelScope.launch {
            chatRepository.getMessages(cursor).fold(
                onSuccess = { page ->
                    timelineState = MessageTimeline.merge(timelineState, page.messages)
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                        nextCursor = page.nextCursor,
                        hasMore = page.hasMore,
                        isLoadingOlder = false,
                        olderPageError = null,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        isLoadingOlder = false,
                        olderPageError = "Failed to load older messages. Tap to retry.",
                    )
                },
            )
            isLoadingOlderGuard = false
        }
    }

    fun retryLoadOlder() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        if (current.olderPageError != null) {
            _uiState.value = current.copy(olderPageError = null)
            loadOlder()
        }
    }

    fun sendMessage(body: String) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(isSending = true, sendError = null)
        viewModelScope.launch {
            chatRepository.sendMessage(trimmed).fold(
                onSuccess = { msg ->
                    timelineState = MessageTimeline.merge(timelineState, listOf(msg))
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                        isSending = false,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        isSending = false,
                        sendError = it.message ?: "Failed to send message",
                    )
                },
            )
        }
    }

    fun setPendingAttachment(attachment: PendingAttachment?) {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val error = attachment?.let {
            AttachmentValidator.validate(it.mimeType, it.fileSize).exceptionOrNull()?.message
        }
        _uiState.value = current.copy(pendingAttachment = attachment, attachmentError = error)
    }

    fun sendPendingAttachment() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val attachment = current.pendingAttachment ?: return
        if (current.attachmentError != null) return
        _uiState.value = current.copy(isSending = true, pendingAttachment = null)
        viewModelScope.launch {
            chatRepository.sendFileMessage(attachment.uri, attachment.mimeType, attachment.fileName).fold(
                onSuccess = { msg ->
                    timelineState = MessageTimeline.merge(timelineState, listOf(msg))
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                        isSending = false,
                        pendingAttachment = null,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(isSending = false, pendingAttachment = attachment)
                },
            )
        }
    }

    fun clearSendError() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(sendError = null)
    }

    private fun load() {
        viewModelScope.launch {
            val accountDeferred = async { authRepository.getMe() }
            chatRepository.getConversation().fold(
                onSuccess = { conversation ->
                    chatRepository.getMessages().fold(
                        onSuccess = { page ->
                            timelineState = MessageTimeline.fromMessages(page.messages)
                            val currentUserId = accountDeferred.await().getOrNull()?.id
                            _uiState.value = ChatUiState.Success(
                                conversation = conversation,
                                messages = timelineState.chronological,
                                currentUserId = currentUserId,
                                nextCursor = page.nextCursor,
                                hasMore = page.hasMore,
                            )
                        },
                        onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages") },
                    )
                },
                onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load conversation") },
            )
        }
    }
}
