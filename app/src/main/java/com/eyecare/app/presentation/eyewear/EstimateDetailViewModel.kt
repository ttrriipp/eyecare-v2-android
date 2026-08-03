package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.repository.QuotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EstimateDetailUiState {
    data object Loading : EstimateDetailUiState
    data class Success(val quotation: Quotation) : EstimateDetailUiState
    data class Error(val message: String) : EstimateDetailUiState
}

@HiltViewModel
class EstimateDetailViewModel @Inject constructor(
    private val repository: QuotationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val quotationId: Int = checkNotNull(savedStateHandle["quotationId"])

    private val _uiState = MutableStateFlow<EstimateDetailUiState>(EstimateDetailUiState.Loading)
    val uiState: StateFlow<EstimateDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() { load() }

    private fun load() {
        _uiState.value = EstimateDetailUiState.Loading
        viewModelScope.launch {
            repository.getQuotation(quotationId).fold(
                onSuccess = { _uiState.value = EstimateDetailUiState.Success(it) },
                onFailure = { _uiState.value = EstimateDetailUiState.Error(it.message ?: "Failed to load estimate") },
            )
        }
    }
}
