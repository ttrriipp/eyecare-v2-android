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

enum class OrderListFilter(
    val apiValue: String,
    val label: String,
) {
    CURRENT("current", "Current"),
    HISTORY("history", "History"),
}

sealed interface OrderListUiState {
    val selectedFilter: OrderListFilter

    data class Loading(
        override val selectedFilter: OrderListFilter = OrderListFilter.CURRENT,
    ) : OrderListUiState

    data class Success(
        val items: List<OpticalOrder>,
        override val selectedFilter: OrderListFilter = OrderListFilter.CURRENT,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
    ) : OrderListUiState

    data class Empty(
        override val selectedFilter: OrderListFilter = OrderListFilter.CURRENT,
    ) : OrderListUiState

    data class Error(
        val message: String,
        override val selectedFilter: OrderListFilter = OrderListFilter.CURRENT,
    ) : OrderListUiState
}

@HiltViewModel
class OpticalOrderListViewModel @Inject constructor(
    private val repository: OpticalOrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading())
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private var selectedFilter = OrderListFilter.CURRENT
    private var currentPage = 1
    private var loadSequence = 0

    init { load() }

    fun selectFilter(filter: OrderListFilter) {
        if (filter == selectedFilter) return
        selectedFilter = filter
        currentPage = 1
        load()
    }

    fun refresh() {
        // A refresh while orders are already showing (pull-to-refresh, or returning to the
        // screen) shouldn't wipe the list back to a bare spinner — only a genuine first load
        // or an error retry resets to Loading.
        val current = _uiState.value
        if (current is OrderListUiState.Success) {
            refreshInternal(current)
        } else {
            currentPage = 1
            load()
        }
    }

    private fun refreshInternal(current: OrderListUiState.Success) {
        val seq = ++loadSequence
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.getOpticalOrders(filter = selectedFilter.apiValue, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    _uiState.value = if (result.data.isEmpty()) {
                        OrderListUiState.Empty(selectedFilter)
                    } else {
                        OrderListUiState.Success(
                            items = result.data,
                            selectedFilter = selectedFilter,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    // Keep the existing items visible; a failed background refresh shouldn't
                    // discard data the patient can already see.
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
        if (state !is OrderListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        loadMoreInternal(currentPage + 1)
    }

    private fun load() {
        currentPage = 1
        val seq = ++loadSequence
        _uiState.value = OrderListUiState.Loading(selectedFilter)
        viewModelScope.launch {
            repository.getOpticalOrders(filter = selectedFilter.apiValue, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = OrderListUiState.Empty(selectedFilter)
                    else _uiState.value = OrderListUiState.Success(
                        items = result.data,
                        selectedFilter = selectedFilter,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = OrderListUiState.Error(
                        message = "We couldn't load your orders. Check your connection and try again.",
                        selectedFilter = selectedFilter,
                    )
                },
            )
        }
    }

    private fun loadMoreInternal(page: Int) {
        val current = _uiState.value as? OrderListUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getOpticalOrders(filter = current.selectedFilter.apiValue, page = page).fold(
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
