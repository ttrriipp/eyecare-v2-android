package com.eyecare.app.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.repository.ChatRepository
import com.eyecare.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUnreadState(
    val messageUnreadCount: Int = 0,
    val notificationUnreadCount: Int = 0,
)

@HiltViewModel
class MainUnreadViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUnreadState())
    val state: StateFlow<MainUnreadState> = _state.asStateFlow()

    private var isMessageRefreshInFlight = false
    private var isNotificationRefreshInFlight = false

    fun refresh() {
        refreshMessages()
        refreshNotifications()
    }

    fun onMessagesMarkedRead() {
        _state.value = _state.value.copy(messageUnreadCount = 0)
        refreshMessages()
    }

    fun onNotificationRead() {
        val current = _state.value.notificationUnreadCount
        _state.value = _state.value.copy(notificationUnreadCount = (current - 1).coerceAtLeast(0))
        refreshNotifications()
    }

    fun onAllNotificationsRead() {
        _state.value = _state.value.copy(notificationUnreadCount = 0)
        refreshNotifications()
    }

    fun reconcileNotificationUnreadCount(count: Int) {
        _state.value = _state.value.copy(notificationUnreadCount = count.coerceAtLeast(0))
    }

    private fun refreshMessages() {
        if (isMessageRefreshInFlight) return
        isMessageRefreshInFlight = true
        viewModelScope.launch {
            chatRepository.getConversation().fold(
                onSuccess = { conversation ->
                    _state.value = _state.value.copy(
                        messageUnreadCount = conversation.unreadCount.coerceAtLeast(0),
                    )
                },
                onFailure = {
                    // Preserve last known value on failure
                },
            )
            isMessageRefreshInFlight = false
        }
    }

    private fun refreshNotifications() {
        if (isNotificationRefreshInFlight) return
        isNotificationRefreshInFlight = true
        viewModelScope.launch {
            notificationRepository.getUnreadCount().fold(
                onSuccess = { count ->
                    _state.value = _state.value.copy(
                        notificationUnreadCount = count.coerceAtLeast(0),
                    )
                },
                onFailure = {
                    // Preserve last known value on failure
                },
            )
            isNotificationRefreshInFlight = false
        }
    }
}
