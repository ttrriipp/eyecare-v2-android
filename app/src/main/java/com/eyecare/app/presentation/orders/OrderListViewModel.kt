package com.eyecare.app.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OrderListUiState {
    data object Loading : OrderListUiState
    data class Success(
        val orders: List<Order>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : OrderListUiState
    data object Empty : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val repository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init { load() }

    fun refresh() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val current = _uiState.value as? OrderListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            currentPage++
            repository.getOrders(page = currentPage).fold(
                onSuccess = { newOrders ->
                    val hasMore = repository.hasMorePages(currentPage)
                    _uiState.value = OrderListUiState.Success(
                        orders = current.orders + newOrders,
                        hasMorePages = hasMore,
                    )
                },
                onFailure = {
                    currentPage--
                    _uiState.value = current.copy(isLoadingMore = false)
                },
            )
        }
    }

    private fun load() {
        _uiState.value = OrderListUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getOrders(page = 1).fold(
                onSuccess = { orders ->
                    if (orders.isEmpty()) OrderListUiState.Empty
                    else OrderListUiState.Success(orders, hasMorePages = repository.hasMorePages(1))
                },
                onFailure = { OrderListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
