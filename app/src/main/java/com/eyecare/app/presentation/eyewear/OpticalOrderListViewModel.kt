package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.repository.OpticalOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class OrderFilter(val apiValue: String, val label: String) {
    CURRENT("current", "Current"),
    HISTORY("history", "History"),
}

sealed interface OrderListUiState {
    data object Loading : OrderListUiState
    data class Success(
        val items: List<OpticalOrder>,
        val filter: OrderFilter,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
    ) : OrderListUiState
    data class Empty(val filter: OrderFilter) : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}

@HiltViewModel
class OpticalOrderListViewModel @Inject constructor(
    private val repository: OpticalOrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private var currentFilter = OrderFilter.CURRENT
    private var currentPage = 1
    private var loadSequence = 0

    init { load(OrderFilter.CURRENT) }

    fun selectFilter(filter: OrderFilter) {
        if (filter == currentFilter) return
        currentFilter = filter
        currentPage = 1
        load(filter)
    }

    fun refresh() {
        currentPage = 1
        load(currentFilter)
    }

    fun retry() {
        currentPage = 1
        load(currentFilter)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is OrderListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load(filter: OrderFilter) {
        val seq = ++loadSequence
        _uiState.value = OrderListUiState.Loading
        viewModelScope.launch {
            repository.getOpticalOrders(filter = filter.apiValue, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = OrderListUiState.Empty(filter)
                    else _uiState.value = OrderListUiState.Success(
                        items = result.data,
                        filter = filter,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = OrderListUiState.Error(it.message ?: "Failed to load orders")
                },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? OrderListUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getOpticalOrders(filter = currentFilter.apiValue, page = currentPage).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    val all = current.items + result.data
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
