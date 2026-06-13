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

sealed interface FeedbackHistoryUiState {
    data object Loading : FeedbackHistoryUiState
    data class Success(val items: List<Feedback>) : FeedbackHistoryUiState
    data object Empty : FeedbackHistoryUiState
    data class Error(val message: String) : FeedbackHistoryUiState
}

@HiltViewModel(assistedFactory = FeedbackViewModel.Factory::class)
class FeedbackViewModel @AssistedInject constructor(
    private val repository: FeedbackRepository,
    @Assisted("appointmentId") val appointmentId: Int?,
    @Assisted("orderId") val orderId: Int?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("appointmentId") appointmentId: Int?,
            @Assisted("orderId") orderId: Int?,
        ): FeedbackViewModel
    }

    private val _uiState = MutableStateFlow<FeedbackUiState>(FeedbackUiState.Idle)
    val uiState: StateFlow<FeedbackUiState> = _uiState.asStateFlow()

    private val _history = MutableStateFlow<FeedbackHistoryUiState>(FeedbackHistoryUiState.Loading)
    val history: StateFlow<FeedbackHistoryUiState> = _history.asStateFlow()

    init { loadHistory() }

    fun retryHistory() = loadHistory()

    fun submit(rating: Int, comment: String?) {
        if (rating < 1) {
            _uiState.value = FeedbackUiState.ValidationError("Please select a rating")
            return
        }
        _uiState.value = FeedbackUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.submitFeedback(appointmentId, orderId, rating, comment?.takeIf { it.isNotBlank() }).fold(
                onSuccess = { FeedbackUiState.Submitted(it) },
                onFailure = { FeedbackUiState.Error(it.message ?: "Submission failed") },
            )
        }
    }

    private fun loadHistory() {
        viewModelScope.launch {
            _history.value = repository.getFeedbackHistory().fold(
                onSuccess = { if (it.isEmpty()) FeedbackHistoryUiState.Empty else FeedbackHistoryUiState.Success(it) },
                onFailure = { FeedbackHistoryUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
