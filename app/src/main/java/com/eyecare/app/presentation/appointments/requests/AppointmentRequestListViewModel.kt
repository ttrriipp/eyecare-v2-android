package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentBookingEligibility
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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
        val bookingEligibility: AppointmentBookingEligibility? = null,
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
    private var requestJob: Job? = null
    private var requestGeneration = 0L

    init {
        loadInitial()
    }

    /**
     * Refresh when an existing ViewModel is shown again after navigation. The lifecycle helper
     * skips the initial resume, so every callback here represents a return to the list.
     */
    fun onScreenResumed() {
        refresh()
    }

    fun loadInitial() {
        val generation = beginRequest()
        requestJob = viewModelScope.launch {
            _state.value = RequestListState.Loading
            repository.getRequests(page = 1)
                .onSuccess { paginated ->
                    if (generation == requestGeneration) {
                        seenIds.clear()
                        seenIds.addAll(paginated.data.map { it.id })
                        _state.value = RequestListState.Data(
                            requests = paginated.data,
                            hasMore = paginated.hasMorePages,
                            currentPage = 1,
                            bookingEligibility = paginated.bookingEligibility,
                        )
                    }
                }
                .onFailure { error ->
                    if (generation == requestGeneration) {
                        _state.value = RequestListState.Error(
                            patientSafeAppointmentRequestError(
                                error = error,
                                fallback = "We couldn't load your requests. Please try again.",
                            ),
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        if (current !is RequestListState.Data || current.isLoadingMore || !current.hasMore) return

        val generation = beginRequest()
        _state.value = current.copy(isLoadingMore = true, appendError = null)
        requestJob = viewModelScope.launch {
            repository.getRequests(page = current.currentPage + 1)
                .onSuccess { paginated ->
                    if (generation == requestGeneration) {
                        val filtered = paginated.data.filter { it.id !in seenIds }
                        seenIds.addAll(filtered.map { it.id })
                        _state.value = RequestListState.Data(
                            requests = current.requests + filtered,
                            hasMore = paginated.hasMorePages,
                            currentPage = current.currentPage + 1,
                            bookingEligibility = paginated.bookingEligibility ?: current.bookingEligibility,
                        )
                    }
                }
                .onFailure { error ->
                    if (generation == requestGeneration) {
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
        val generation = beginRequest()
        _state.value = current.copy(isRefreshing = true, error = null)
        requestJob = viewModelScope.launch {
            repository.getRequests(page = 1)
                .onSuccess { paginated ->
                    if (generation == requestGeneration) {
                        seenIds.clear()
                        seenIds.addAll(paginated.data.map { it.id })
                        _state.value = RequestListState.Data(
                            requests = paginated.data,
                            hasMore = paginated.hasMorePages,
                            currentPage = 1,
                            bookingEligibility = paginated.bookingEligibility,
                        )
                    }
                }
                .onFailure {
                    if (generation == requestGeneration) {
                        // Keep the existing requests visible; a failed background refresh
                        // shouldn't discard data the patient can already see.
                        _state.value = current.copy(
                            isRefreshing = false,
                            error = "We couldn't refresh your requests. Showing the latest list.",
                        )
                    }
                }
        }
    }

    private fun beginRequest(): Long {
        requestJob?.cancel()
        requestJob = null
        return ++requestGeneration
    }
}
