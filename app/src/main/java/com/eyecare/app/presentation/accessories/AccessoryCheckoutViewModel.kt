package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.CommerceApiCodes
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CheckoutUiState {
    data object Idle : CheckoutUiState
    data object Submitting : CheckoutUiState
    data class UploadingDiscountProof(val requestId: Int) : CheckoutUiState
    data class Success(
        val requestId: Int,
        val requiresDiscountProof: Boolean = false,
        val proofSubmitted: Boolean = false,
    ) : CheckoutUiState
    data class ProofUploadError(
        val requestId: Int,
        val message: String,
    ) : CheckoutUiState
    data class Error(
        val message: String,
        val isConflict: Boolean = false,
    ) : CheckoutUiState
}

@HiltViewModel
class AccessoryCheckoutViewModel @Inject constructor(
    private val repository: AccessoryOrderRequestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CheckoutUiState>(CheckoutUiState.Idle)
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val _selectedDiscount = MutableStateFlow("none")
    val selectedDiscount: StateFlow<String> = _selectedDiscount.asStateFlow()

    private var isSubmitting = false

    fun selectDiscount(discountType: String) {
        _selectedDiscount.value = discountType
    }

    fun submit(
        discountType: String,
        items: List<Pair<Int, Int>>,
        proof: DiscountProofUpload?,
    ) {
        if (isSubmitting) return
        val requestsDiscount = !discountType.equals("none", ignoreCase = true)
        if (requestsDiscount && proof == null) {
            _uiState.value = CheckoutUiState.Error(
                message = "Choose a JPG or PNG proof image before submitting your discount request.",
            )
            return
        }
        isSubmitting = true
        _uiState.value = CheckoutUiState.Submitting

        viewModelScope.launch {
            try {
                repository.submitRequest(discountType, items).fold(
                    onSuccess = { request ->
                        if (requestsDiscount && proof != null) {
                            uploadProof(request.id, proof)
                        } else {
                            _uiState.value = CheckoutUiState.Success(requestId = request.id)
                        }
                    },
                    onFailure = { error ->
                        val isConflict = error is ApiDomainError && error.code == CommerceApiCodes.ACTIVE_ORDER_REQUEST_EXISTS
                        _uiState.value = CheckoutUiState.Error(
                            message = if (isConflict) {
                                "You already have a pending order request. View your current requests."
                            } else {
                                "We couldn't submit your order request. Please try again."
                            },
                            isConflict = isConflict,
                        )
                    },
                )
            } finally {
                isSubmitting = false
            }
        }
    }

    fun submit(discountType: String, items: List<Pair<Int, Int>>) {
        submit(discountType = discountType, items = items, proof = null)
    }

    fun retryDiscountProof(proof: DiscountProofUpload) {
        val failedUpload = _uiState.value as? CheckoutUiState.ProofUploadError ?: return
        if (isSubmitting) return
        isSubmitting = true
        viewModelScope.launch {
            try {
                uploadProof(failedUpload.requestId, proof)
            } finally {
                isSubmitting = false
            }
        }
    }

    private suspend fun uploadProof(requestId: Int, proof: DiscountProofUpload) {
        _uiState.value = CheckoutUiState.UploadingDiscountProof(requestId)
        repository.uploadDiscountProof(requestId, proof).fold(
            onSuccess = {
                _uiState.value = CheckoutUiState.Success(
                    requestId = requestId,
                    proofSubmitted = true,
                )
            },
            onFailure = {
                _uiState.value = CheckoutUiState.ProofUploadError(
                    requestId = requestId,
                    message = "Your request was created, but the discount proof didn't upload. Retry the upload or open the request to send it there.",
                )
            },
        )
    }

    fun reset() {
        _uiState.value = CheckoutUiState.Idle
    }
}
