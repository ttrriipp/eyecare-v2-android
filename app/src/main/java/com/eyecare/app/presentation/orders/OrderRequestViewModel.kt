package com.eyecare.app.presentation.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.remote.dto.OrderDtos
import com.eyecare.app.domain.model.Order
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.model.isMobileOrderable
import com.eyecare.app.domain.repository.OrderRepository
import com.eyecare.app.domain.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface OrderRequestUiState {
    data object Loading : OrderRequestUiState
    data class Ready(
        val product: Product,
        val selectedVariant: ProductVariant,
        val quantity: Int = 1,
        val isSubmitting: Boolean = false,
        val error: String? = null,
    ) : OrderRequestUiState
    data class Submitted(val order: Order) : OrderRequestUiState
    data class Error(val message: String, val canRetry: Boolean = true) : OrderRequestUiState
}

@HiltViewModel(assistedFactory = OrderRequestViewModel.Factory::class)
class OrderRequestViewModel @AssistedInject constructor(
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository,
    @Assisted("productId") private val productId: Int,
    @Assisted("variantId") private val variantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("productId") productId: Int,
            @Assisted("variantId") variantId: Int,
        ): OrderRequestViewModel
    }

    private val _uiState = MutableStateFlow<OrderRequestUiState>(OrderRequestUiState.Loading)
    val uiState: StateFlow<OrderRequestUiState> = _uiState.asStateFlow()

    init { load() }

    fun setQuantity(qty: Int) = updateReady { copy(quantity = qty.coerceIn(1, 4)) }

    fun submit() {
        val state = _uiState.value as? OrderRequestUiState.Ready ?: return
        if (!state.product.isMobileOrderable) {
            _uiState.value = OrderRequestUiState.Error(
                message = "This product is available to browse only and cannot be ordered in the app.",
                canRetry = false,
            )
            return
        }
        viewModelScope.launch {
            updateReady { copy(isSubmitting = true, error = null) }
            orderRepository.createOrder(
                items = listOf(
                    OrderDtos.OrderItemRequest(
                        productVariantId = state.selectedVariant.id,
                        quantity = state.quantity,
                    )
                ),
            ).fold(
                onSuccess = { _uiState.value = OrderRequestUiState.Submitted(it) },
                onFailure = { updateReady { copy(isSubmitting = false, error = it.message) } },
            )
        }
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            productRepository.getProduct(productId).fold(
                onSuccess = { product ->
                    if (!product.isMobileOrderable) {
                        _uiState.value = OrderRequestUiState.Error(
                            message = "This product is available to browse only and cannot be ordered in the app.",
                            canRetry = false,
                        )
                        return@fold
                    }
                    val variant = product.variants.firstOrNull { it.id == variantId }
                        ?: product.variants.firstOrNull()
                        ?: return@fold run { _uiState.value = OrderRequestUiState.Error("Variant not found") }
                    _uiState.value = OrderRequestUiState.Ready(
                        product = product,
                        selectedVariant = variant,
                    )
                },
                onFailure = { _uiState.value = OrderRequestUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun updateReady(transform: OrderRequestUiState.Ready.() -> OrderRequestUiState.Ready) {
        val current = _uiState.value as? OrderRequestUiState.Ready ?: return
        _uiState.value = current.transform()
    }
}
