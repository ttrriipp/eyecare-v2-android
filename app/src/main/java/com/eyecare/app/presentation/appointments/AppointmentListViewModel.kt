package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentListUiState {
    data object Loading : AppointmentListUiState
    data class Success(
        val appointments: List<AppointmentV1>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
    ) : AppointmentListUiState
    data object Empty : AppointmentListUiState
    data class Error(val message: String) : AppointmentListUiState
}

@HiltViewModel
class AppointmentListViewModel @Inject constructor(
    private val repository: AppointmentV1Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentListUiState>(AppointmentListUiState.Loading)
    val uiState: StateFlow<AppointmentListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var lastPage = 1

    init { load() }

    fun refresh() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is AppointmentListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load() {
        _uiState.value = AppointmentListUiState.Loading
        viewModelScope.launch {
            repository.getAppointments(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    lastPage = result.lastPage
                    if (result.data.isEmpty()) {
                        _uiState.value = AppointmentListUiState.Empty
                    } else {
                        _uiState.value = AppointmentListUiState.Success(
                            appointments = result.data.sortedByDescending {
                                runCatching { java.time.Instant.parse(it.scheduledAt) }
                                    .getOrElse { java.time.Instant.EPOCH }
                            },
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = { _uiState.value = AppointmentListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? AppointmentListUiState.Success ?: return
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getAppointments(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    lastPage = result.lastPage
                    val all = current.appointments + result.data
                    _uiState.value = current.copy(
                        appointments = all.sortedByDescending {
                            runCatching { java.time.Instant.parse(it.scheduledAt) }
                                .getOrElse { java.time.Instant.EPOCH }
                        },
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        loadMoreError = it.message ?: "Failed to load more",
                    )
                },
            )
        }
    }
}
