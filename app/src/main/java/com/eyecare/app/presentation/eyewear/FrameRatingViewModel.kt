package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.repository.FrameRatingError
import com.eyecare.app.domain.model.FrameRating
import com.eyecare.app.domain.repository.JobOrderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FrameRatingUiState {
    data object Idle : FrameRatingUiState
    data object Submitting : FrameRatingUiState
    data class Success(val rating: FrameRating) : FrameRatingUiState
    data class Error(val message: String, val fieldErrors: Map<String, List<String>>? = null) : FrameRatingUiState
}

@HiltViewModel(assistedFactory = FrameRatingViewModel.Factory::class)
class FrameRatingViewModel @AssistedInject constructor(
    private val repository: JobOrderRepository,
    @Assisted("jobOrderItemId") private val jobOrderItemId: Int,
    @Assisted("productVariantId") private val productVariantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("jobOrderItemId") jobOrderItemId: Int,
            @Assisted("productVariantId") productVariantId: Int,
        ): FrameRatingViewModel
    }

    private val _uiState = MutableStateFlow<FrameRatingUiState>(FrameRatingUiState.Idle)
    val uiState: StateFlow<FrameRatingUiState> = _uiState.asStateFlow()

    fun submitRating(rating: Int, comment: String?) {
        if (rating < 1 || rating > 5) {
            _uiState.value = FrameRatingUiState.Error("Rating must be between 1 and 5")
            return
        }
        _uiState.value = FrameRatingUiState.Submitting
        viewModelScope.launch {
            repository.submitRating(
                jobOrderItemId = jobOrderItemId,
                productVariantId = productVariantId,
                rating = rating,
                comment = comment?.takeIf { it.isNotBlank() },
            ).fold(
                onSuccess = { frameRating ->
                    _uiState.value = FrameRatingUiState.Success(frameRating)
                },
                onFailure = { error ->
                    if (error is FrameRatingError) {
                        _uiState.value = FrameRatingUiState.Error(
                            message = error.message,
                            fieldErrors = error.fieldErrors,
                        )
                    } else {
                        _uiState.value = FrameRatingUiState.Error(
                            message = error.message ?: "Failed to submit rating",
                        )
                    }
                },
            )
        }
    }

    fun reset() {
        _uiState.value = FrameRatingUiState.Idle
    }
}
