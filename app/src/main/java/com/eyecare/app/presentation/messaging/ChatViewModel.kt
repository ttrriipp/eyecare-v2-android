package com.eyecare.app.presentation.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val conversation: Conversation,
        val messages: List<Message>,
        val isSending: Boolean = false,
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    var currentUserId: Int = -1
        private set

    init {
        viewModelScope.launch {
            authRepository.getUser().onSuccess { currentUserId = it.id }
        }
        load()
    }

    fun sendMessage(body: String) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(isSending = true)
        viewModelScope.launch {
            chatRepository.sendMessage(current.conversation.id, trimmed).fold(
                onSuccess = { msg ->
                    _uiState.value = current.copy(
                        messages = current.messages + msg,
                        isSending = false,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(isSending = false)
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            chatRepository.getOrCreateConversation().fold(
                onSuccess = { conversation ->
                    chatRepository.getMessages(conversation.id).fold(
                        onSuccess = { messages ->
                            _uiState.value = ChatUiState.Success(conversation, messages)
                        },
                        onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages") },
                    )
                },
                onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load conversation") },
            )
        }
    }
}
