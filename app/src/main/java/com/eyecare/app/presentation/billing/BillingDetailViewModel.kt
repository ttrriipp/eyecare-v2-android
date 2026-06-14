package com.eyecare.app.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.repository.BillingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface BillingDetailUiState {
    data object Loading : BillingDetailUiState
    data class Success(val billing: Billing) : BillingDetailUiState
    data class Error(val message: String) : BillingDetailUiState
}

@HiltViewModel(assistedFactory = BillingDetailViewModel.Factory::class)
class BillingDetailViewModel @AssistedInject constructor(
    private val billingRepository: BillingRepository,
    @Assisted private val billingId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(billingId: Int): BillingDetailViewModel
    }

    private val _uiState = MutableStateFlow<BillingDetailUiState>(BillingDetailUiState.Loading)
    val uiState: StateFlow<BillingDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = billingRepository.getBilling(billingId).fold(
                onSuccess = { BillingDetailUiState.Success(it) },
                onFailure = { BillingDetailUiState.Error(it.message ?: "Failed to load billing") },
            )
        }
    }
}
