package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.repository.OpticalOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface OpticalOrderDetailUiState {
    data object Loading : OpticalOrderDetailUiState
    data class Success(val order: OpticalOrder) : OpticalOrderDetailUiState
    data class Error(val message: String) : OpticalOrderDetailUiState
}

@HiltViewModel
class OpticalOrderDetailViewModel @Inject constructor(
    private val repository: OpticalOrderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow<OpticalOrderDetailUiState>(OpticalOrderDetailUiState.Loading)
    val uiState: StateFlow<OpticalOrderDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() { load() }

    fun updateItemRating(itemId: Int, ratingResult: RatingResult) {
        val current = _uiState.value as? OpticalOrderDetailUiState.Success ?: return
        val updatedItems = current.order.items.map { item ->
            if (item.id == itemId) item.copy(
                rating = com.eyecare.app.domain.model.RatingSummary(
                    rating = ratingResult.rating,
                    comment = ratingResult.comment,
                    createdAt = null,
                )
            ) else item
        }
        _uiState.value = OpticalOrderDetailUiState.Success(current.order.copy(items = updatedItems))
    }

    private fun load() {
        _uiState.value = OpticalOrderDetailUiState.Loading
        viewModelScope.launch {
            repository.getOpticalOrder(orderId).fold(
                onSuccess = { _uiState.value = OpticalOrderDetailUiState.Success(it) },
                onFailure = { _uiState.value = OpticalOrderDetailUiState.Error(it.message ?: "Failed to load order") },
            )
        }
    }
}
