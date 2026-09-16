package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentHistoryUiState {
    data object Loading : AppointmentHistoryUiState

    data class Content(
        val appointments: List<AppointmentV1>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : AppointmentHistoryUiState

    data object Empty : AppointmentHistoryUiState

    data class Error(val message: String) : AppointmentHistoryUiState
}

@HiltViewModel
class AppointmentHistoryViewModel @Inject constructor(
    private val appointmentRepository: AppointmentV1Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentHistoryUiState>(AppointmentHistoryUiState.Loading)
    val uiState: StateFlow<AppointmentHistoryUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var lastPage = 1
    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        currentPage = 1
        _uiState.value = AppointmentHistoryUiState.Loading
        loadJob = viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = 1).fold(
                onSuccess = { result ->
                    updateFromResult(result, isRefresh = false)
                },
                onFailure = { error ->
                    _uiState.value = AppointmentHistoryUiState.Error(
                        message = error.message ?: "Unable to load history. Please try again.",
                    )
                },
            )
        }
    }

    fun refresh() {
        currentPage = 1
        val current = _uiState.value
        if (current is AppointmentHistoryUiState.Content) {
            _uiState.value = current.copy(isRefreshing = true, refreshError = null)
        }
        viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = 1).fold(
                onSuccess = { result ->
                    updateFromResult(result, isRefresh = true)
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is AppointmentHistoryUiState.Content) {
                        _uiState.value = latest.copy(
                            isRefreshing = false,
                            refreshError = error.message ?: "Refresh failed.",
                        )
                    }
                },
            )
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current !is AppointmentHistoryUiState.Content || current.isLoadingMore || !current.hasMorePages) return

        val nextPage = currentPage + 1
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = nextPage).fold(
                onSuccess = { result ->
                    val latest = _uiState.value
                    if (latest is AppointmentHistoryUiState.Content) {
                        currentPage = result.currentPage
                        lastPage = result.lastPage
                        _uiState.value = latest.copy(
                            appointments = latest.appointments + result.data,
                            isLoadingMore = false,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is AppointmentHistoryUiState.Content) {
                        _uiState.value = latest.copy(
                            isLoadingMore = false,
                            loadMoreError = error.message ?: "Unable to load more.",
                        )
                    }
                },
            )
        }
    }

    fun retry() {
        load()
    }

    fun clearRefreshError() {
        val current = _uiState.value
        if (current is AppointmentHistoryUiState.Content) {
            _uiState.value = current.copy(refreshError = null)
        }
    }

    fun clearLoadMoreError() {
        val current = _uiState.value
        if (current is AppointmentHistoryUiState.Content) {
            _uiState.value = current.copy(loadMoreError = null)
        }
    }

    private fun updateFromResult(result: PaginatedResult<AppointmentV1>, isRefresh: Boolean) {
        currentPage = result.currentPage
        lastPage = result.lastPage
        if (result.data.isEmpty() && !isRefresh) {
            _uiState.value = AppointmentHistoryUiState.Empty
        } else {
            _uiState.value = AppointmentHistoryUiState.Content(
                appointments = result.data,
                hasMorePages = result.hasMorePages,
            )
        }
    }
}
