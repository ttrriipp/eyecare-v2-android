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
    data class Success(val orders: List<Order>) : OrderListUiState
    data object Empty : OrderListUiState
    data class Error(val message: String) : OrderListUiState
}

@HiltViewModel
class OrderListViewModel @Inject constructor(
    private val repository: OrderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderListUiState>(OrderListUiState.Loading)
    val uiState: StateFlow<OrderListUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        _uiState.value = OrderListUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getOrders().fold(
                onSuccess = { if (it.isEmpty()) OrderListUiState.Empty else OrderListUiState.Success(it) },
                onFailure = { OrderListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
