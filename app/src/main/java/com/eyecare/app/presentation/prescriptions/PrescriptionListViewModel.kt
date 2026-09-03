package com.eyecare.app.presentation.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PrescriptionListUiState {
    data object Loading : PrescriptionListUiState
    data class Success(
        val prescriptions: List<Prescription>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
    ) : PrescriptionListUiState
    data object Empty : PrescriptionListUiState
    data class Error(val message: String) : PrescriptionListUiState
}

@HiltViewModel
class PrescriptionListViewModel @Inject constructor(
    private val repository: PrescriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrescriptionListUiState>(PrescriptionListUiState.Loading)
    val uiState: StateFlow<PrescriptionListUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init { load() }

    fun refresh() {
        val current = _uiState.value as? PrescriptionListUiState.Success
        if (current != null) {
            refreshInternal(current)
        } else {
            currentPage = 1
            load()
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is PrescriptionListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load() {
        _uiState.value = PrescriptionListUiState.Loading
        viewModelScope.launch {
            repository.getPrescriptions(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = PrescriptionListUiState.Empty
                    else _uiState.value = PrescriptionListUiState.Success(
                        prescriptions = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _uiState.value = PrescriptionListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun refreshInternal(current: PrescriptionListUiState.Success) {
        currentPage = 1
        _uiState.value = current.copy(isRefreshing = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getPrescriptions(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) {
                        _uiState.value = PrescriptionListUiState.Empty
                    } else {
                        _uiState.value = PrescriptionListUiState.Success(
                            prescriptions = result.data,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = {
                    _uiState.value = current.copy(isRefreshing = false)
                },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? PrescriptionListUiState.Success ?: return
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getPrescriptions(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.prescriptions + result.data
                    _uiState.value = current.copy(
                        prescriptions = all,
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
