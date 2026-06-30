package com.eyecare.app.presentation.billing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Billing
import com.eyecare.app.domain.repository.BillingRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.InputStream

sealed interface BillingDetailUiState {
    data object Loading : BillingDetailUiState
    data class Success(val billing: Billing, val isDownloading: Boolean = false) : BillingDetailUiState
    data class Error(val message: String) : BillingDetailUiState
}

sealed interface BillingEvent {
    data class PdfReady(val inputStream: InputStream, val fileName: String) : BillingEvent
    data class DownloadError(val message: String) : BillingEvent
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

    private val _events = MutableSharedFlow<BillingEvent>()
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    init { load() }

    fun refresh() = load()

    fun downloadPdf() {
        val current = _uiState.value as? BillingDetailUiState.Success ?: return
        _uiState.value = current.copy(isDownloading = true)
        viewModelScope.launch {
            billingRepository.downloadPdf(billingId).fold(
                onSuccess = { stream ->
                    val fileName = "receipt-${current.billing.billingNumber}.pdf"
                    _events.emit(BillingEvent.PdfReady(stream, fileName))
                    _uiState.value = current.copy(isDownloading = false)
                },
                onFailure = {
                    _events.emit(BillingEvent.DownloadError(it.message ?: "Download failed"))
                    _uiState.value = current.copy(isDownloading = false)
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = billingRepository.getBilling(billingId).fold(
                onSuccess = { BillingDetailUiState.Success(it) },
                onFailure = { BillingDetailUiState.Error(it.message ?: "Failed to load billing") },
            )
        }
    }
}
