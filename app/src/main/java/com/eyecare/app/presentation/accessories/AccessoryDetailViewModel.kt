package com.eyecare.app.presentation.accessories

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryAvailability
import com.eyecare.app.domain.model.AccessoryVariant
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.repository.AccessoryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

sealed interface AccessoryDetailUiState {
    data object Loading : AccessoryDetailUiState
    data class Success(
        val accessory: Accessory,
        val selectedVariantId: Int,
    ) : AccessoryDetailUiState {
        val selectedVariant: AccessoryVariant?
            get() = accessory.variants.find { it.id == selectedVariantId }

        val displayPrice: BigDecimal
            get() = selectedVariant?.price ?: accessory.variants.firstOrNull()?.price ?: BigDecimal.ZERO

        val canAddToCart: Boolean
            get() = selectedVariant?.availability?.isOrderable == true
    }
    data class Error(
        val message: String,
        val isNotFound: Boolean = false,
    ) : AccessoryDetailUiState
}

@HiltViewModel
class AccessoryDetailViewModel @Inject constructor(
    private val repository: AccessoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val accessoryId: Int = savedStateHandle.get<Int>("accessoryId") ?: 0

    private val _uiState = MutableStateFlow<AccessoryDetailUiState>(AccessoryDetailUiState.Loading)
    val uiState: StateFlow<AccessoryDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun selectVariant(variantId: Int) {
        val current = _uiState.value
        if (current is AccessoryDetailUiState.Success) {
            _uiState.value = current.copy(selectedVariantId = variantId)
        }
    }

    fun retry() {
        load()
    }

    private fun load() {
        _uiState.value = AccessoryDetailUiState.Loading
        viewModelScope.launch {
            repository.getAccessory(accessoryId).fold(
                onSuccess = { accessory ->
                    val defaultVariant = accessory.variants.firstOrNull()?.id ?: 0
                    _uiState.value = AccessoryDetailUiState.Success(
                        accessory = accessory,
                        selectedVariantId = defaultVariant,
                    )
                },
                onFailure = { error ->
                    val isNotFound = error is ApiDomainError && error.httpStatus == 404
                    _uiState.value = AccessoryDetailUiState.Error(
                        message = if (isNotFound) {
                            "This accessory could not be found."
                        } else {
                            "We couldn't load this accessory. Check your connection and try again."
                        },
                        isNotFound = isNotFound,
                    )
                },
            )
        }
    }
}