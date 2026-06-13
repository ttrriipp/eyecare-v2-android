package com.eyecare.app.presentation.messaging

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.Conversation
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Pending attachment the user has picked but not yet sent. */
data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String,
    val fileSize: Long,
)

/** Pending context link the user has chosen but not yet sent. */
sealed interface PendingContext {
    data class AppointmentContext(val appointment: Appointment) : PendingContext
    data class OrderContext(val order: Order) : PendingContext
}

sealed interface ChatUiState {
    data object Loading : ChatUiState
    data class Success(
        val conversation: Conversation,
        val messages: List<Message>,
        val isSending: Boolean = false,
        val pendingAttachment: PendingAttachment? = null,
        val pendingContext: PendingContext? = null,
        val attachmentError: String? = null,
        // For pickers in the bottom sheet
        val appointments: List<Appointment> = emptyList(),
        val orders: List<Order> = emptyList(),
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val appointmentRepository: AppointmentRepository,
    private val orderRepository: OrderRepository,
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
                    _uiState.value = current.copy(messages = current.messages + msg, isSending = false)
                },
                onFailure = { _uiState.value = current.copy(isSending = false) },
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
            chatRepository.sendFileMessage(
                current.conversation.id, attachment.uri, attachment.mimeType, attachment.fileName
            ).fold(
                onSuccess = { msg ->
                    _uiState.value = current.copy(messages = current.messages + msg, isSending = false, pendingAttachment = null)
                },
                onFailure = { _uiState.value = current.copy(isSending = false, pendingAttachment = attachment) },
            )
        }
    }

    fun sendContextMessage() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        val ctx = current.pendingContext ?: return
        val body = when (ctx) {
            is PendingContext.AppointmentContext -> {
                val a = ctx.appointment
                "📅 Appointment: ${a.visitReason.replace('_', ' ')} — ${a.scheduledAt.take(10)} [${a.status.name.lowercase()}]"
            }
            is PendingContext.OrderContext -> {
                val o = ctx.order
                "📦 Order #${o.orderNumber} — ${o.status.name.replace('_', ' ').lowercase()}"
            }
        }
        val appointmentId = (ctx as? PendingContext.AppointmentContext)?.appointment?.id
        val orderId = (ctx as? PendingContext.OrderContext)?.order?.id
        _uiState.value = current.copy(isSending = true, pendingContext = null)
        viewModelScope.launch {
            chatRepository.sendContextMessage(current.conversation.id, body, appointmentId, orderId).fold(
                onSuccess = { msg ->
                    _uiState.value = current.copy(messages = current.messages + msg, isSending = false, pendingContext = null)
                },
                onFailure = { _uiState.value = current.copy(isSending = false, pendingContext = ctx) },
            )
        }
    }

    fun loadPickerData() {
        val current = _uiState.value as? ChatUiState.Success ?: return
        viewModelScope.launch {
            val appointments = appointmentRepository.getAppointments().getOrDefault(emptyList())
            val orders = orderRepository.getOrders().getOrDefault(emptyList())
            _uiState.value = current.copy(appointments = appointments, orders = orders)
        }
    }

    private fun load() {
        viewModelScope.launch {
            chatRepository.getOrCreateConversation().fold(
                onSuccess = { conversation ->
                    chatRepository.getMessages(conversation.id).fold(
                        onSuccess = { messages -> _uiState.value = ChatUiState.Success(conversation, messages) },
                        onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load messages") },
                    )
                },
                onFailure = { _uiState.value = ChatUiState.Error(it.message ?: "Failed to load conversation") },
            )
        }
    }
}
