package com.eyecare.app.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.remote.api.BillingApiService
import com.eyecare.app.data.remote.dto.BillingDtos
import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.model.BillingStatus
import com.eyecare.app.domain.model.Payment
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

sealed interface BillingDetailUiState {
    data object Loading : BillingDetailUiState
    data class Success(val billing: Billing) : BillingDetailUiState
    data class Error(val message: String) : BillingDetailUiState
}

@HiltViewModel(assistedFactory = BillingDetailViewModel.Factory::class)
class BillingDetailViewModel @AssistedInject constructor(
    private val api: BillingApiService,
    private val json: Json,
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
            _uiState.value = runCatching { api.getBilling(billingId).data.toDomain() }.fold(
                onSuccess = { BillingDetailUiState.Success(it) },
                onFailure = { BillingDetailUiState.Error(it.message ?: "Failed to load billing") },
            )
        }
    }

    private fun BillingDtos.BillingDto.toDomain() = Billing(
        id = id, orderId = orderId, status = BillingStatus.from(status),
        totalAmount = totalAmount, amountPaid = amountPaid, balanceDue = balanceDue,
        issuedAt = issuedAt, createdAt = createdAt,
        payments = payments.map { p ->
            Payment(p.id, p.amount, p.status, p.method, p.referenceNumber, p.paidAt)
        },
    )
}
