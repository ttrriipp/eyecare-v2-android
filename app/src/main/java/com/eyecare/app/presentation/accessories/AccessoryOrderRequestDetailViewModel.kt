package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.CommerceApiCodes
import com.eyecare.app.domain.model.DiscountProofStatus
import com.eyecare.app.domain.model.DiscountProofUpload
import com.eyecare.app.domain.model.DiscountType
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
import com.eyecare.app.presentation.eyewear.PaymentProofInspector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestDetailUiState {
    data object Loading : RequestDetailUiState
    data class Success(
        val request: AccessoryOrderRequest,
        val isCancelling: Boolean = false,
        val uploadState: DiscountProofUploadState = DiscountProofUploadState.Idle,
        val isRefreshing: Boolean = false,
        val refreshErrorMessage: String? = null,
    ) : RequestDetailUiState {
        val canCancel: Boolean
            get() = request.status == OrderRequestStatus.PENDING &&
                !isCancelling && !isRefreshing && uploadState !is DiscountProofUploadState.Uploading
    }
    data class Error(
        val message: String,
        val isNotFound: Boolean = false,
    ) : RequestDetailUiState
}

sealed interface DiscountProofUploadState {
    data object Idle : DiscountProofUploadState
    data object Uploading : DiscountProofUploadState
    data class Error(val message: String) : DiscountProofUploadState
}

@HiltViewModel
class AccessoryOrderRequestDetailViewModel @Inject constructor(
    private val repository: AccessoryOrderRequestRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle,
) : ViewModel() {

    private val requestId: Int = savedStateHandle.get<Int>("requestId") ?: 0

    private val _uiState = MutableStateFlow<RequestDetailUiState>(RequestDetailUiState.Loading)
    val uiState: StateFlow<RequestDetailUiState> = _uiState.asStateFlow()

    private var isCancelling = false
    private var isUploading = false
    private var isRefreshing = false

    init { load() }

    fun cancel() {
        val current = _uiState.value
        if (current !is RequestDetailUiState.Success) return
        if (!current.canCancel || isCancelling) return

        isCancelling = true
        _uiState.value = current.copy(isCancelling = true)

        viewModelScope.launch {
            try {
                repository.cancelRequest(requestId).fold(
                    onSuccess = { cancelled ->
                        _uiState.value = RequestDetailUiState.Success(request = cancelled)
                    },
                    onFailure = { error ->
                        if (error is ApiDomainError && error.code == CommerceApiCodes.ORDER_REQUEST_NOT_ACTIONABLE) {
                            // Refresh to get authoritative state
                            load()
                        } else {
                            _uiState.value = current.copy(isCancelling = false)
                        }
                    },
                )
            } finally {
                isCancelling = false
            }
        }
    }

    fun retry() {
        val current = _uiState.value
        if (current !is RequestDetailUiState.Success) {
            load()
            return
        }
        if (isRefreshing || isCancelling || isUploading) return

        isRefreshing = true
        _uiState.value = current.copy(isRefreshing = true, refreshErrorMessage = null)
        viewModelScope.launch {
            try {
                repository.getRequest(requestId).fold(
                    onSuccess = { request ->
                        val latest = _uiState.value as? RequestDetailUiState.Success
                        if (latest != null) {
                            _uiState.value = latest.copy(
                                request = request,
                                isRefreshing = false,
                                refreshErrorMessage = null,
                            )
                        }
                    },
                    onFailure = {
                        val latest = _uiState.value as? RequestDetailUiState.Success
                        if (latest != null) {
                            _uiState.value = latest.copy(
                                isRefreshing = false,
                                refreshErrorMessage = "Couldn't refresh. Pull down to try again.",
                            )
                        }
                    },
                )
            } finally {
                isRefreshing = false
            }
        }
    }

    fun uploadDiscountProof(proof: DiscountProofUpload) {
        if (isUploading || isRefreshing) return
        val current = _uiState.value as? RequestDetailUiState.Success ?: return

        val canUpload = current.request.status == OrderRequestStatus.PENDING &&
            current.request.requestedDiscountType != DiscountType.NONE &&
            current.request.discountProofStatus in setOf(
                DiscountProofStatus.NOT_SUBMITTED,
                DiscountProofStatus.REJECTED,
            )
        if (!canUpload) {
            if (proof.deleteAfterUpload) proof.imageFile.delete()
            _uiState.value = current.copy(
                uploadState = DiscountProofUploadState.Error(
                    when {
                        current.request.requestedDiscountType == DiscountType.NONE ->
                            "This request does not have a discount proof to upload."
                        current.request.discountProofStatus == DiscountProofStatus.PENDING ->
                            "Your discount proof is already under review."
                        current.request.discountProofStatus == DiscountProofStatus.ACCEPTED ->
                            "Your discount proof has already been accepted."
                        else -> "This request is no longer accepting discount proof uploads."
                    },
                ),
            )
            return
        }

        val validationError = sequenceOf(
            PaymentProofInspector.validateMimeType(proof.mimeType),
            PaymentProofInspector.validateFileSize(proof.imageFile.length()),
            PaymentProofInspector.validateDimensions(proof.width, proof.height),
        ).filterNotNull().firstOrNull()
        if (validationError != null) {
            _uiState.value = current.copy(uploadState = DiscountProofUploadState.Error(validationError))
            return
        }

        isUploading = true
        _uiState.value = current.copy(uploadState = DiscountProofUploadState.Uploading)
        viewModelScope.launch {
            try {
                repository.uploadDiscountProof(requestId, proof).fold(
                    onSuccess = { load() },
                    onFailure = { error ->
                        when (error) {
                            is ApiDomainError -> when (error.code) {
                                CommerceApiCodes.DISCOUNT_PROOF_RATE_LIMIT_REACHED -> {
                                    val message = error.retryAfterSeconds?.let {
                                        "Too many upload attempts. Try again in $it seconds."
                                    } ?: "Too many upload attempts. Try again later."
                                    val fresh = _uiState.value as? RequestDetailUiState.Success
                                    if (fresh != null) {
                                        _uiState.value = fresh.copy(uploadState = DiscountProofUploadState.Error(message))
                                    }
                                }
                                CommerceApiCodes.DISCOUNT_PROOF_NOT_REQUESTED -> {
                                    load()
                                }
                                CommerceApiCodes.ORDER_REQUEST_NOT_ACTIONABLE -> {
                                    load()
                                }
                                else -> setUploadError("We couldn't upload your discount proof. Please try again.")
                            }
                            else -> setUploadError("We couldn't upload your discount proof. Please try again.")
                        }
                    },
                )
            } finally {
                isUploading = false
            }
        }
    }

    fun clearUploadState() {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        _uiState.value = current.copy(uploadState = DiscountProofUploadState.Idle)
    }

    private fun setUploadError(message: String) {
        val current = _uiState.value as? RequestDetailUiState.Success ?: return
        _uiState.value = current.copy(uploadState = DiscountProofUploadState.Error(message))
    }

    private fun load() {
        _uiState.value = RequestDetailUiState.Loading
        viewModelScope.launch {
            repository.getRequest(requestId).fold(
                onSuccess = { request ->
                    _uiState.value = RequestDetailUiState.Success(request = request)
                },
                onFailure = { error ->
                    val isNotFound = error is ApiDomainError && error.httpStatus == 404
                    _uiState.value = RequestDetailUiState.Error(
                        message = if (isNotFound) {
                            "This request could not be found."
                        } else {
                            "We couldn't load this request. Check your connection and try again."
                        },
                        isNotFound = isNotFound,
                    )
                },
            )
        }
    }
}
