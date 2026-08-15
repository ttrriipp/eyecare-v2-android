package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

private const val POLL_INTERVAL_MS = 5_000L

data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val fileSize: Long,
)

data class SearchState(
    val query: String,
    val results: List<Message>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val error: String?,
    val generation: Long,
)

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val conversation: Conversation,
        val messages: List<Message>,
        val currentUserId: Int? = null,
        val inputText: String = "",
        val isSending: Boolean = false,
        val pendingAttachment: PendingAttachment? = null,
        val attachmentError: String? = null,
        val sendError: String? = null,
        val nextCursor: String? = null,
        val hasMore: Boolean = false,
        val isLoadingOlder: Boolean = false,
        val olderPageError: String? = null,
        val searchState: SearchState? = null,
        val searchDraft: String = "",
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

sealed interface ChatEffect {
    data object MessagesMarkedRead : ChatEffect
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var timelineState = MessageTimeline.State()
    private var isScreenVisible = false
    private var pollingJob: Job? = null
    private var isLoadingOlderGuard = false
    private var isMarkReadInFlight = false
    private var pendingMarkRead = false
    private var searchGeneration = 0L
    private var isLoadingMoreSearchGuard = false

    init { load() }

    fun setScreenVisible(visible: Boolean) {
        isScreenVisible = visible
        if (visible && pollingJob == null) {
            startPolling()
        } else if (!visible) {
            pollingJob?.cancel()
            pollingJob = null
        }
        if (visible) {
            tryMarkRead()
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
                val hadNewStaff = hasNewStaffMessages(polledMessages)
                if (MessageTimeline.hasNewMessages(timelineState, polledMessages)) {
                    timelineState = MessageTimeline.merge(timelineState, polledMessages)
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                    )
                    if (hadNewStaff && isScreenVisible) {
                        tryMarkRead()
                    }
                }
            },
            onFailure = {
                // Poll failures preserve usable messages; no full-screen error
            },
        )
    }

    private fun hasNewStaffMessages(incoming: List<Message>): Boolean {
        val currentUserId = (_uiState.value as? ChatUiState.Success)?.currentUserId
        return incoming.any { msg ->
            msg.senderType == SenderType.STAFF ||
                (currentUserId != null && msg.senderId != currentUserId)
        }
    }

    private fun tryMarkRead() {
        val state = _uiState.value as? ChatUiState.Success ?: return
        val hasStaffMessages = state.messages.any { msg ->
            msg.senderType == SenderType.STAFF ||
                (state.currentUserId != null && msg.senderId != state.currentUserId)
        }
        if (!hasStaffMessages) return
        if (isMarkReadInFlight) {
            pendingMarkRead = true
            return
        }
        isMarkReadInFlight = true
        pendingMarkRead = false
        viewModelScope.launch {
            chatRepository.markMessagesRead().fold(
                onSuccess = {
                    isMarkReadInFlight = false
                    _effects.send(ChatEffect.MessagesMarkedRead)
                    if (pendingMarkRead) {
                        pendingMarkRead = false
                        tryMarkRead()
                    }
                },
                onFailure = {
                    isMarkReadInFlight = false
                    pendingMarkRead = true
                },
            )
        }
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

    fun onDraftChanged(text: String) {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(inputText = text)
    }

    fun sendMessage() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val trimmed = current.inputText.trim()
        if (trimmed.isBlank()) return
        if (current.isSending) return
        _uiState.value = current.copy(isSending = true, sendError = null)
        viewModelScope.launch {
            chatRepository.sendMessage(trimmed).fold(
                onSuccess = { msg ->
                    timelineState = MessageTimeline.merge(timelineState, listOf(msg))
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                        isSending = false,
                        inputText = "",
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        isSending = false,
                        sendError = mapSendError(it),
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
        if (current.isSending) return
        val submittedDraft = current.inputText
        val submittedBody = submittedDraft.trim().ifBlank { "Attachment" }
        _uiState.value = current.copy(isSending = true, sendError = null, pendingAttachment = null)
        viewModelScope.launch {
            chatRepository.sendFileMessage(
                submittedBody,
                attachment.uri,
                attachment.mimeType,
                attachment.fileName,
            ).fold(
                onSuccess = { msg ->
                    timelineState = MessageTimeline.merge(timelineState, listOf(msg))
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        messages = timelineState.chronological,
                        isSending = false,
                        pendingAttachment = null,
                        inputText = if (latest.inputText == submittedDraft) "" else latest.inputText,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        isSending = false,
                        pendingAttachment = attachment,
                        sendError = mapSendError(it),
                    )
                },
            )
        }
    }

    fun clearSendError() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(sendError = null)
    }

    fun openSearch() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        if (current.searchState != null) return
        _uiState.value = current.copy(searchDraft = "", searchState = null)
    }

    fun closeSearch() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(searchState = null, searchDraft = "")
    }

    fun onSearchQueryChanged(query: String) {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(searchDraft = query)
    }

    fun submitSearch() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val trimmed = current.searchDraft.trim()
        if (trimmed.length < 3 || trimmed.length > 500) {
            _uiState.value = current.copy(
                searchState = SearchState(
                    query = trimmed,
                    results = emptyList(),
                    nextCursor = null,
                    hasMore = false,
                    isLoading = false,
                    isLoadingMore = false,
                    error = if (trimmed.length < 3) "Search requires at least 3 characters."
                    else "Search is limited to 500 characters.",
                    generation = searchGeneration,
                ),
            )
            return
        }
        val gen = ++searchGeneration
        isLoadingMoreSearchGuard = false
        _uiState.value = current.copy(
            searchState = SearchState(
                query = trimmed,
                results = emptyList(),
                nextCursor = null,
                hasMore = false,
                isLoading = true,
                isLoadingMore = false,
                error = null,
                generation = gen,
            ),
        )
        viewModelScope.launch {
            chatRepository.searchMessages(trimmed).fold(
                onSuccess = { page ->
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    val ss = latest.searchState ?: return@fold
                    if (ss.generation != gen) return@fold
                    _uiState.value = latest.copy(
                        searchState = ss.copy(
                            results = page.messages,
                            nextCursor = page.nextCursor,
                            hasMore = page.hasMore,
                            isLoading = false,
                            error = null,
                        ),
                    )
                },
                onFailure = { throwable ->
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    val ss = latest.searchState ?: return@fold
                    if (ss.generation != gen) return@fold
                    _uiState.value = latest.copy(
                        searchState = ss.copy(
                            isLoading = false,
                            error = "Search failed. Please try again.",
                        ),
                    )
                },
            )
        }
    }

    fun loadMoreSearchResults() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val ss = current.searchState ?: return
        if (!ss.hasMore || ss.isLoadingMore || ss.isLoading || isLoadingMoreSearchGuard) return
        val cursor = ss.nextCursor ?: return

        isLoadingMoreSearchGuard = true
        val gen = ss.generation
        _uiState.value = current.copy(
            searchState = ss.copy(isLoadingMore = true, error = null),
        )

        viewModelScope.launch {
            chatRepository.searchMessages(ss.query, cursor).fold(
                onSuccess = { page ->
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    val currentSS = latest.searchState ?: return@fold
                    if (currentSS.generation != gen) return@fold
                    _uiState.value = latest.copy(
                        searchState = currentSS.copy(
                            results = currentSS.results + page.messages,
                            nextCursor = page.nextCursor,
                            hasMore = page.hasMore,
                            isLoadingMore = false,
                            error = null,
                        ),
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    val currentSS = latest.searchState ?: return@fold
                    if (currentSS.generation != gen) return@fold
                    _uiState.value = latest.copy(
                        searchState = currentSS.copy(
                            isLoadingMore = false,
                            error = "Failed to load more results. Tap to retry.",
                        ),
                    )
                },
            )
            isLoadingMoreSearchGuard = false
        }
    }

    private fun mapSendError(throwable: Throwable): String {
        val apiError = throwable as? ApiDomainError
        return when {
            apiError?.httpStatus == 429 ->
                "You're sending messages too quickly. Please wait and try again."
            apiError?.httpStatus == 422 ->
                "Message could not be sent. Please check and try again."
            throwable is IOException ->
                "Unable to send. Check your connection and try again."
            else ->
                "Unable to send. Check your connection and try again."
        }
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
                            tryMarkRead()
                        },
                        onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages") },
                    )
                },
                onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load conversation") },
            )
        }
    }
}
