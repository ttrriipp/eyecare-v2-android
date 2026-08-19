package com.eyecare.app.presentation.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppNotification
import com.eyecare.app.domain.model.MobileDestination
import com.eyecare.app.domain.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NO_DESTINATION_MESSAGE = "No further details for this notification."

sealed interface NotificationListUiState {
    data object Loading : NotificationListUiState

    data class Success(
        val notifications: List<AppNotification>,
        val isLoadingMore: Boolean,
        val canLoadMore: Boolean,
        val mutationInFlight: Set<String>,
        val inlineError: String?,
        val isRefreshing: Boolean = false,
        val isMarkAllInFlight: Boolean = false,
        val infoMessage: String? = null,
    ) : NotificationListUiState

    data class Error(val patientSafeMessage: String) : NotificationListUiState
}

sealed interface NotificationEffect {
    data class Navigate(val destination: MobileDestination) : NotificationEffect
    data object NotificationRead : NotificationEffect
    data object AllNotificationsRead : NotificationEffect
    data class UnreadCountReconciled(val count: Int) : NotificationEffect
}

@HiltViewModel
class NotificationListViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationListUiState>(NotificationListUiState.Loading)
    val uiState: StateFlow<NotificationListUiState> = _uiState.asStateFlow()

    private val _effects = Channel<NotificationEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var currentPage = 0
    private var lastPage = 0
    private var isLoadingMoreGuard = false
    private var isMarkAllInFlight = false

    init { loadInitial() }

    fun loadInitial() {
        _uiState.value = NotificationListUiState.Loading
        currentPage = 0
        lastPage = 0
        isLoadingMoreGuard = false
        isMarkAllInFlight = false
        viewModelScope.launch {
            notificationRepository.getNotifications(page = 1).fold(
                onSuccess = { page ->
                    currentPage = 1
                    lastPage = page.lastPage
                    _uiState.value = NotificationListUiState.Success(
                        notifications = page.notifications,
                        isLoadingMore = false,
                        canLoadMore = currentPage < lastPage,
                        mutationInFlight = emptySet(),
                        inlineError = null,
                    )
                },
                onFailure = {
                    _uiState.value = NotificationListUiState.Error(
                        mapError(it),
                    )
                },
            )
        }
    }

    fun loadMore() {
        val current = _uiState.value as? NotificationListUiState.Success ?: return
        if (!current.canLoadMore || current.isLoadingMore || isLoadingMoreGuard) return
        val nextPage = currentPage + 1
        if (nextPage > lastPage) return

        isLoadingMoreGuard = true
        _uiState.value = current.copy(isLoadingMore = true, inlineError = null)

        viewModelScope.launch {
            notificationRepository.getNotifications(page = nextPage).fold(
                onSuccess = { page ->
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    val seenIds = latest.notifications.map { it.id }.toMutableSet()
                    val deduped = page.notifications.filter { seenIds.add(it.id) }
                    currentPage = page.currentPage
                    lastPage = page.lastPage
                    _uiState.value = latest.copy(
                        notifications = latest.notifications + deduped,
                        isLoadingMore = false,
                        canLoadMore = currentPage < lastPage,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        isLoadingMore = false,
                        inlineError = "Failed to load more notifications. Tap to retry.",
                    )
                },
            )
            isLoadingMoreGuard = false
        }
    }

    fun refresh() {
        val current = _uiState.value as? NotificationListUiState.Success
        if (current != null) {
            _uiState.value = current.copy(isRefreshing = true, inlineError = null)
        }
        viewModelScope.launch {
            notificationRepository.getNotifications(page = 1).fold(
                onSuccess = { page ->
                    currentPage = 1
                    lastPage = page.lastPage
                    _uiState.value = NotificationListUiState.Success(
                        notifications = page.notifications,
                        isLoadingMore = false,
                        canLoadMore = currentPage < lastPage,
                        mutationInFlight = emptySet(),
                        inlineError = null,
                        isRefreshing = false,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? NotificationListUiState.Success
                    if (latest != null) {
                        _uiState.value = latest.copy(
                            isRefreshing = false,
                            inlineError = "Refresh failed. Pull to try again.",
                        )
                    }
                },
            )
        }
    }

    fun markOneRead(notification: AppNotification) {
        if (notification.readAt != null) return
        val current = _uiState.value as? NotificationListUiState.Success ?: return
        if (notification.id in current.mutationInFlight) return

        _uiState.value = current.copy(
            mutationInFlight = current.mutationInFlight + notification.id,
            notifications = current.notifications.map {
                if (it.id == notification.id) it.copy(readAt = "") else it
            },
        )

        viewModelScope.launch {
            notificationRepository.markOneRead(notification.id).fold(
                onSuccess = {
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    val hasDestination = notification.mobileAction != null &&
                        notification.mobileAction != MobileDestination.UNKNOWN
                    _uiState.value = latest.copy(
                        mutationInFlight = latest.mutationInFlight - notification.id,
                        inlineError = null,
                        infoMessage = if (hasDestination) null else NO_DESTINATION_MESSAGE,
                    )
                    _effects.send(NotificationEffect.NotificationRead)
                    if (hasDestination) {
                        _effects.send(NotificationEffect.Navigate(notification.mobileAction))
                    }
                },
                onFailure = {
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        mutationInFlight = latest.mutationInFlight - notification.id,
                        notifications = latest.notifications.map {
                            if (it.id == notification.id) it.copy(readAt = notification.readAt) else it
                        },
                        inlineError = "Failed to mark notification as read. Please try again.",
                    )
                    reconcileUnreadCount()
                },
            )
        }
    }

    fun markAllRead() {
        val current = _uiState.value as? NotificationListUiState.Success ?: return
        if (isMarkAllInFlight) return

        isMarkAllInFlight = true
        _uiState.value = current.copy(inlineError = null, isMarkAllInFlight = true)

        viewModelScope.launch {
            notificationRepository.markAllRead().fold(
                onSuccess = {
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        notifications = latest.notifications.map { it.copy(readAt = it.readAt ?: "") },
                        isMarkAllInFlight = false,
                    )
                    _effects.send(NotificationEffect.AllNotificationsRead)
                    isMarkAllInFlight = false
                },
                onFailure = {
                    val latest = _uiState.value as? NotificationListUiState.Success ?: return@fold
                    _uiState.value = latest.copy(
                        inlineError = "Failed to mark all as read. Please try again.",
                        isMarkAllInFlight = false,
                    )
                    reconcileUnreadCount()
                    isMarkAllInFlight = false
                },
            )
        }
    }

    fun onNotificationTap(notification: AppNotification) {
        if (notification.readAt == null) {
            markOneRead(notification)
            return
        }
        val hasDestination = notification.mobileAction != null &&
            notification.mobileAction != MobileDestination.UNKNOWN
        if (hasDestination) {
            viewModelScope.launch {
                _effects.send(NotificationEffect.Navigate(notification.mobileAction))
            }
        } else {
            val current = _uiState.value as? NotificationListUiState.Success ?: return
            _uiState.value = current.copy(infoMessage = NO_DESTINATION_MESSAGE)
        }
    }

    /** Clears the one-shot info message after the screen has shown it. */
    fun clearInfoMessage() {
        val current = _uiState.value as? NotificationListUiState.Success ?: return
        _uiState.value = current.copy(infoMessage = null)
    }

    private fun mapError(throwable: Throwable): String = "Unable to load notifications. Please try again."

    private suspend fun reconcileUnreadCount() {
        notificationRepository.getUnreadCount().fold(
            onSuccess = { count ->
                _effects.send(NotificationEffect.UnreadCountReconciled(count.coerceAtLeast(0)))
            },
            onFailure = {
                // The mutation error remains the safe feedback shown to the patient.
            },
        )
    }
}
