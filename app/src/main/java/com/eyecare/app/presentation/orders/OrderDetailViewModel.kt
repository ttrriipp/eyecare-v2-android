package com.eyecare.app.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.repository.OrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrderDetailUiState {
    data object Loading : OrderDetailUiState
    data class Success(val order: Order, val isCancelling: Boolean = false, val cancelError: String? = null) : OrderDetailUiState
    data class Error(val message: String) : OrderDetailUiState
}

@HiltViewModel(assistedFactory = OrderDetailViewModel.Factory::class)
class OrderDetailViewModel @AssistedInject constructor(
    private val repository: OrderRepository,
    @Assisted private val orderId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(orderId: Int): OrderDetailViewModel
    }

    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    fun cancelOrder() {
        val current = _uiState.value
        if (current !is OrderDetailUiState.Success) return
        _uiState.value = current.copy(isCancelling = true, cancelError = null)
        viewModelScope.launch {
            repository.cancelOrder(orderId).fold(
                onSuccess = { load() },
                onFailure = {
                    _uiState.value = current.copy(
                        isCancelling = false,
                        cancelError = it.message ?: "Failed to cancel order",
                    )
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = repository.getOrder(orderId).fold(
                onSuccess = { OrderDetailUiState.Success(it) },
                onFailure = { OrderDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
