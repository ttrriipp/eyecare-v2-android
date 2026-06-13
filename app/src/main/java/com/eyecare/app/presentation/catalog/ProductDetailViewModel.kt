package com.eyecare.app.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Product
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.ProductRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ProductDetailUiState {
    data object Loading : ProductDetailUiState
    data class Success(
        val product: Product,
        val selectedVariant: ProductVariant,
    ) : ProductDetailUiState
    data class Error(val message: String) : ProductDetailUiState
}

@HiltViewModel(assistedFactory = ProductDetailViewModel.Factory::class)
class ProductDetailViewModel @AssistedInject constructor(
    private val repository: ProductRepository,
    @Assisted private val productId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(productId: Int): ProductDetailViewModel
    }

    private val _uiState = MutableStateFlow<ProductDetailUiState>(ProductDetailUiState.Loading)
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun selectVariant(variant: ProductVariant) {
        val current = _uiState.value as? ProductDetailUiState.Success ?: return
        _uiState.value = current.copy(selectedVariant = variant)
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = repository.getProduct(productId).fold(
                onSuccess = { product ->
                    ProductDetailUiState.Success(
                        product = product,
                        selectedVariant = product.variants.firstOrNull()
                            ?: return@fold ProductDetailUiState.Error("No variants available"),
                    )
                },
                onFailure = { ProductDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
