package com.eyecare.app.presentation.feedback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Feedback
import com.eyecare.app.domain.repository.FeedbackRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FeedbackUiState {
    data object Idle : FeedbackUiState
    data object Loading : FeedbackUiState
    data class Submitted(val feedback: Feedback) : FeedbackUiState
    data class ValidationError(val message: String) : FeedbackUiState
    data class Error(val message: String) : FeedbackUiState
}

@HiltViewModel(assistedFactory = FeedbackViewModel.Factory::class)
class FeedbackViewModel @AssistedInject constructor(
    private val repository: FeedbackRepository,
    @Assisted("appointmentId") val appointmentId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("appointmentId") appointmentId: Int,
        ): FeedbackViewModel
    }

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Idle)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    fun submit(rating: Int, comment: String?) {
        if (rating < 1) {
            _uiState.value = FeedbackUiState.ValidationError("Please select a rating")
            return
        }
        _uiState.value = FeedbackUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.submitFeedback(appointmentId, rating, comment?.takeIf { it.isNotBlank() }).fold(
                onSuccess = { FeedbackUiState.Submitted(it) },
                onFailure = { FeedbackUiState.Error(it.message ?: "Submission failed") },
            )
        }
    }
}
