package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageContext
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.domain.repository.OpticalOrderRepository
import com.eyecare.app.domain.repository.QuotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

sealed interface PendingContext {
    data class Estimate(val quotation: Quotation) : PendingContext
    data class Order(val order: OpticalOrder) : PendingContext
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val conversation: Conversation,
        val messages: List<Message>,
        val currentUserId: Int? = null,
        val isSending: Boolean = false,
        val pendingAttachment: PendingAttachment? = null,
        val pendingContext: PendingContext? = null,
        val attachmentError: String? = null,
        val quotations: List<Quotation> = emptyList(),
        val orders: List<OpticalOrder> = emptyList(),
        val pickerError: String? = null,
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val quotationRepository: QuotationRepository,
    private val opticalOrderRepository: OpticalOrderRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var isScreenVisible = false
    private var pollingJob: kotlinx.coroutines.Job? = null

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
                val current = _uiState.value as? ChatUiState.Success ?: continue
                chatRepository.getMessages().onSuccess { messages ->
                    val latest = _uiState.value as? ChatUiState.Success ?: return@onSuccess
                    if (messages.size != latest.messages.size || messages.lastOrNull()?.id != latest.messages.lastOrNull()?.id) {
                        _uiState.value = latest.copy(messages = messages)
                    }
                }
            }
        }
    }

    fun retry() = load()

    fun sendMessage(body: String) {
        val trimmed = body.trim()
        if (trimmed.isBlank()) return
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(isSending = true)
        viewModelScope.launch {
            chatRepository.sendMessage(trimmed).fold(
                onSuccess = { msg ->
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(messages = latest.messages + msg, isSending = false)
                },
                onFailure = {
                    val latest = _uiState.value as? ChatUiState.Success ?: return@fold
                    _uiState.value = latest.copy(isSending = false)
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

    fun setPendingContext(context: PendingContext?) {
        val current = _uiState.value as? ChatUiState.Success ?: return
        _uiState.value = current.copy(pendingContext = context)
    }

    fun sendPendingAttachment() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val attachment = current.pendingAttachment ?: return
        if (current.attachmentError != null) return
        _uiState.value = current.copy(isSending = true, pendingAttachment = null)
        viewModelScope.launch {
            chatRepository.sendFileMessage(attachment.uri, attachment.mimeType, attachment.fileName).fold(
                onSuccess = {
                    chatRepository.getMessages().fold(
                        onSuccess = { messages ->
                            _uiState.value = current.copy(messages = messages, isSending = false, pendingAttachment = null)
                        },
                        onFailure = { _uiState.value = current.copy(isSending = false, pendingAttachment = null) },
                    )
                },
                onFailure = { _uiState.value = current.copy(isSending = false, pendingAttachment = attachment) },
            )
        }
    }

    fun sendContextMessage() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val ctx = current.pendingContext ?: return
        val (body, messageContext) = when (ctx) {
            is PendingContext.Estimate -> {
                val q = ctx.quotation
                "Estimate: ${q.quotationNumber}" to MessageContext.Quotation(q.id)
            }
            is PendingContext.Order -> {
                val o = ctx.order
                "Eyewear order: ${o.orderNumber}" to MessageContext.OpticalOrder(o.id)
            }
        }
        _uiState.value = current.copy(isSending = true, pendingContext = null)
        viewModelScope.launch {
            chatRepository.sendMessage(body, listOf(messageContext)).fold(
                onSuccess = { msg ->
                    _uiState.value = current.copy(messages = current.messages + msg, isSending = false, pendingContext = null)
                },
                onFailure = { _uiState.value = current.copy(isSending = false, pendingContext = ctx) },
            )
        }
    }

    fun loadPickerData() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        if (current.quotations.isNotEmpty() || current.orders.isNotEmpty()) return
        viewModelScope.launch {
            val quotations = quotationRepository.getQuotations(filter = "current").getOrNull()?.data ?: emptyList()
            val orders = opticalOrderRepository.getOpticalOrders(filter = "current").getOrNull()?.data ?: emptyList()
            _uiState.value = current.copy(quotations = quotations, orders = orders)
        }
    }

    private fun load() {
        viewModelScope.launch {
            // Loaded alongside the conversation, not derived from message.senderType — this backend
            // is known to leak raw Eloquent polymorphic class names for other polymorphic fields
            // instead of its own documented "patient"/"staff" aliases, so bubble ownership is
            // determined by comparing sender_id to the authenticated account's own id instead.
            val accountDeferred = async { authRepository.getMe() }
            chatRepository.getConversation().fold(
                onSuccess = { conversation ->
                    chatRepository.getMessages().fold(
                        onSuccess = { messages ->
                            val currentUserId = accountDeferred.await().getOrNull()?.id
                            _uiState.value = ChatUiState.Success(conversation, messages, currentUserId = currentUserId)
                        },
                        onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages") },
                    )
                },
                onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load conversation") },
            )
        }
    }
}
