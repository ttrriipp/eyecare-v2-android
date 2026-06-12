package com.eyecare.app.presentation.ar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ProductVariant
import com.eyecare.app.domain.repository.ProductRepository
import com.eyecare.app.presentation.ar.model.ArFaceState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ArPermissionState {
    data object Required : ArPermissionState
    data object Granted : ArPermissionState
    data class Denied(val shouldShowRationale: Boolean) : ArPermissionState
}

@HiltViewModel(assistedFactory = ArViewModel.Factory::class)
class ArViewModel @AssistedInject constructor(
    private val productRepository: ProductRepository,
    @Assisted("productId") val productId: Int,
    @Assisted("variantId") val initialVariantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("productId") productId: Int,
            @Assisted("variantId") initialVariantId: Int,
        ): ArViewModel
    }

    private val _permissionState = MutableStateFlow<ArPermissionState>(ArPermissionState.Required)
    val permissionState: StateFlow<ArPermissionState> = _permissionState.asStateFlow()

    private val _faceState = MutableStateFlow<ArFaceState>(ArFaceState.Initialising)
    val faceState: StateFlow<ArFaceState> = _faceState.asStateFlow()

    private val _variants = MutableStateFlow<List<ProductVariant>>(emptyList())
    val variants: StateFlow<List<ProductVariant>> = _variants.asStateFlow()

    private val _selectedVariant = MutableStateFlow<ProductVariant?>(null)
    val selectedVariant: StateFlow<ProductVariant?> = _selectedVariant.asStateFlow()

    init { loadVariants() }

    fun onPermissionResult(granted: Boolean, shouldShowRationale: Boolean = false) {
        _permissionState.value = if (granted) ArPermissionState.Granted
        else ArPermissionState.Denied(shouldShowRationale)
    }

    fun onFaceResult(state: ArFaceState) {
        _faceState.value = state
    }

    fun selectVariant(variant: ProductVariant) {
        _selectedVariant.value = variant
    }

    private fun loadVariants() {
        viewModelScope.launch {
            productRepository.getProduct(productId).onSuccess { product ->
                _variants.value = product.variants
                _selectedVariant.value = product.variants.firstOrNull { it.id == initialVariantId }
                    ?: product.variants.firstOrNull()
            }
        }
    }
}
