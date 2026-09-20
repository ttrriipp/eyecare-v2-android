package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.OrderRequestFilter
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestListUiState {
    val selectedFilter: OrderRequestFilter

    data class Loading(
        override val selectedFilter: OrderRequestFilter = OrderRequestFilter.CURRENT,
    ) : RequestListUiState

    data class Success(
        val items: List<AccessoryOrderRequest>,
        override val selectedFilter: OrderRequestFilter = OrderRequestFilter.CURRENT,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
    ) : RequestListUiState

    data class Empty(
        override val selectedFilter: OrderRequestFilter = OrderRequestFilter.CURRENT,
    ) : RequestListUiState

    data class Error(
        val message: String,
        override val selectedFilter: OrderRequestFilter = OrderRequestFilter.CURRENT,
    ) : RequestListUiState
}

@HiltViewModel
class AccessoryOrderRequestListViewModel @Inject constructor(
    private val repository: AccessoryOrderRequestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<RequestListUiState>(RequestListUiState.Loading())
    val uiState: StateFlow<RequestListUiState> = _uiState.asStateFlow()

    private var selectedFilter = OrderRequestFilter.CURRENT
    private var currentPage = 1
    private var loadSequence = 0

    init { load() }

    fun selectFilter(filter: OrderRequestFilter) {
        if (filter == selectedFilter) return
        selectedFilter = filter
        currentPage = 1
        load()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is RequestListUiState.Success) {
            refreshInternal(current)
        } else {
            currentPage = 1
            load()
        }
    }

    private fun refreshInternal(current: RequestListUiState.Success) {
        val seq = ++loadSequence
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.getRequests(selectedFilter, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    _uiState.value = if (result.data.isEmpty()) {
                        RequestListUiState.Empty(selectedFilter)
                    } else {
                        RequestListUiState.Success(
                            items = result.data,
                            selectedFilter = selectedFilter,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = current.copy(isRefreshing = false)
                },
            )
        }
    }

    fun retry() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is RequestListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        loadMoreInternal(currentPage + 1)
    }

    private fun load() {
        currentPage = 1
        val seq = ++loadSequence
        _uiState.value = RequestListUiState.Loading(selectedFilter)
        viewModelScope.launch {
            repository.getRequests(selectedFilter, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    _uiState.value = if (result.data.isEmpty()) {
                        RequestListUiState.Empty(selectedFilter)
                    } else {
                        RequestListUiState.Success(
                            items = result.data,
                            selectedFilter = selectedFilter,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = RequestListUiState.Error(
                        message = "We couldn't load your requests. Check your connection and try again.",
                        selectedFilter = selectedFilter,
                    )
                },
            )
        }
    }

    private fun loadMoreInternal(page: Int) {
        val current = _uiState.value as? RequestListUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getRequests(selectedFilter, page = page).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    val all = (current.items + result.data).distinctBy { it.id }
                    _uiState.value = current.copy(
                        items = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        loadMoreError = it.message ?: "Failed to load more",
                    )
                },
            )
        }
    }
}