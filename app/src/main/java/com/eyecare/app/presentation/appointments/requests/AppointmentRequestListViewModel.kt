package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestListState {
    data object Loading : RequestListState
    data class Data(
        val requests: List<AppointmentRequest> = emptyList(),
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = false,
        val currentPage: Int = 1,
        val error: String? = null,
        val appendError: String? = null,
        val isRefreshing: Boolean = false,
    ) : RequestListState
    data class Error(val message: String) : RequestListState
}

@HiltViewModel
class AppointmentRequestListViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestListState>(RequestListState.Loading)
    val state: StateFlow<RequestListState> = _state.asStateFlow()

    private val seenIds = mutableSetOf<Int>()
    private var hasBeenResumed = false

    init {
        loadInitial()
    }

    /**
     * Refresh when an existing ViewModel is shown again after navigation. The first resume is
     * covered by the initial load, so it does not start a duplicate request.
     */
    fun onScreenResumed() {
        if (hasBeenResumed) refresh()
        hasBeenResumed = true
    }

    fun loadInitial() {
        viewModelScope.launch {
            _state.value = RequestListState.Loading
            repository.getRequests(page = 1)
                .onSuccess { paginated ->
                    seenIds.clear()
                    seenIds.addAll(paginated.data.map { it.id })
                    _state.value = RequestListState.Data(
                        requests = paginated.data,
                        hasMore = paginated.hasMorePages,
                        currentPage = 1,
                    )
                }
                .onFailure { error ->
                    _state.value = RequestListState.Error(
                        patientSafeAppointmentRequestError(
                            error = error,
                            fallback = "We couldn't load your requests. Please try again.",
                        ),
                    )
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current !is RequestListState.Data || current.isLoadingMore || !current.hasMore) return

        _state.value = current.copy(isLoadingMore = true, appendError = null)
        viewModelScope.launch {
            repository.getRequests(page = current.currentPage + 1)
                .onSuccess { paginated ->
                    val filtered = paginated.data.filter { it.id !in seenIds }
                    seenIds.addAll(filtered.map { it.id })
                    _state.value = RequestListState.Data(
                        requests = current.requests + filtered,
                        hasMore = paginated.hasMorePages,
                        currentPage = current.currentPage + 1,
                    )
                }
                .onFailure { error ->
                    _state.value = current.copy(
                        isLoadingMore = false,
                        appendError = patientSafeAppointmentRequestError(
                            error = error,
                            fallback = "We couldn't load more requests. Please try again.",
                        ),
                    )
                }
        }
    }

    fun refresh() {
        // A refresh while requests are already showing (pull-to-refresh, or resuming the
        // screen) shouldn't wipe the list back to a bare loading row — only a genuine first
        // load resets to Loading.
        val current = _state.value
        if (current is RequestListState.Data) {
            refreshInternal(current)
        } else {
            seenIds.clear()
            loadInitial()
        }
    }

    private fun refreshInternal(current: RequestListState.Data) {
        _state.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.getRequests(page = 1)
                .onSuccess { paginated ->
                    seenIds.clear()
                    seenIds.addAll(paginated.data.map { it.id })
                    _state.value = RequestListState.Data(
                        requests = paginated.data,
                        hasMore = paginated.hasMorePages,
                        currentPage = 1,
                    )
                }
                .onFailure {
                    // Keep the existing requests visible; a failed background refresh
                    // shouldn't discard data the patient can already see.
                    _state.value = current.copy(isRefreshing = false)
                }
        }
    }
}
