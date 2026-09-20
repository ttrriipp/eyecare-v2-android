package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.OpticalOrder
import com.eyecare.app.domain.model.OpticalOrderItem
import com.eyecare.app.domain.model.PaymentProofUpload
import com.eyecare.app.domain.model.RatingResult
import com.eyecare.app.domain.repository.OpticalOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface OpticalOrderDetailUiState {
    data object Loading : OpticalOrderDetailUiState
    data class Success(
        val order: OpticalOrder,
        val uploadState: ProofUploadState = ProofUploadState.Idle,
    ) : OpticalOrderDetailUiState
    data class Error(val message: String) : OpticalOrderDetailUiState
}

sealed interface ProofUploadState {
    data object Idle : ProofUploadState
    data class Validating(val file: File) : ProofUploadState
    data class Uploading(val progress: Boolean = true) : ProofUploadState
    data class Success(val message: String) : ProofUploadState
    data class Error(val message: String) : ProofUploadState
}

@HiltViewModel
class OpticalOrderDetailViewModel @Inject constructor(
    private val repository: OpticalOrderRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val orderId: Int = checkNotNull(savedStateHandle["orderId"])

    private val _uiState = MutableStateFlow<OpticalOrderDetailUiState>(OpticalOrderDetailUiState.Loading)
    val uiState: StateFlow<OpticalOrderDetailUiState> = _uiState.asStateFlow()

    private var isUploading = false
    private var lastLoadedOrder: OpticalOrder? = null

    init { load() }

    fun retry() { load() }

    fun refresh() { load() }

    fun onResume() {
        // Refresh on resume to pick up staff decisions
        val current = _uiState.value
        if (current is OpticalOrderDetailUiState.Success) {
            load()
        }
    }

    fun updateItemRating(itemId: Int, ratingResult: RatingResult) {
        val current = _uiState.value as? OpticalOrderDetailUiState.Success ?: return
        val updatedItems = current.order.items.map { item ->
            if (item.id == itemId) item.copy(
                rating = com.eyecare.app.domain.model.RatingSummary(
                    rating = ratingResult.rating,
                    comment = ratingResult.comment,
                    createdAt = ratingResult.createdAt,
                )
            ) else item
        }
        _uiState.value = OpticalOrderDetailUiState.Success(current.order.copy(items = updatedItems))
    }

    fun uploadProof(senderName: String, referenceNumber: String, imageFile: File) {
        if (isUploading) return
        val current = _uiState.value as? OpticalOrderDetailUiState.Success ?: return

        // Validate inputs locally
        val senderError = PaymentProofInspector.validateSenderName(senderName)
        if (senderError != null) {
            _uiState.value = current.copy(uploadState = ProofUploadState.Error(senderError))
            return
        }
        val refError = PaymentProofInspector.validateReferenceNumber(referenceNumber)
        if (refError != null) {
            _uiState.value = current.copy(uploadState = ProofUploadState.Error(refError))
            return
        }

        isUploading = true
        _uiState.value = current.copy(uploadState = ProofUploadState.Uploading())

        viewModelScope.launch {
            try {
                repository.uploadPaymentProof(
                    orderId = orderId,
                    proof = PaymentProofUpload(
                        imageFile = imageFile,
                        senderName = senderName.trim(),
                        referenceNumber = referenceNumber.trim(),
                    ),
                ).fold(
                    onSuccess = {
                        // Refresh order to get authoritative state
                        load()
                    },
                    onFailure = { error ->
                        val message = when {
                            error is ApiDomainError && error.code == "PAYMENT_WINDOW_EXPIRED" -> {
                                load()
                                "Payment window has expired. The order has been refreshed."
                            }
                            error is ApiDomainError && error.code == "ORDER_NOT_AWAITING_PAYMENT" -> {
                                load()
                                "This order is no longer awaiting payment."
                            }
                            error is ApiDomainError && error.code == "PAYMENT_PROOF_RATE_LIMIT_REACHED" -> {
                                val retryAfter = error.retryAfterSeconds
                                if (retryAfter != null) {
                                    "Too many attempts. Please try again in ${retryAfter} seconds."
                                } else {
                                    "Too many attempts. Please try again later."
                                }
                            }
                            else -> "We couldn't upload your proof. Please try again."
                        }
                        val fresh = _uiState.value as? OpticalOrderDetailUiState.Success
                        if (fresh != null) {
                            _uiState.value = fresh.copy(uploadState = ProofUploadState.Error(message))
                        }
                    },
                )
            } finally {
                isUploading = false
            }
        }
    }

    fun clearUploadState() {
        val current = _uiState.value as? OpticalOrderDetailUiState.Success ?: return
        _uiState.value = current.copy(uploadState = ProofUploadState.Idle)
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
