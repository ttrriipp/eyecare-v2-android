package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AccessoryOrderRequest
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.OrderRequestStatus
import com.eyecare.app.domain.repository.AccessoryOrderRequestRepository
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
    ) : RequestDetailUiState {
        val canCancel: Boolean get() = request.status == OrderRequestStatus.PENDING && !isCancelling
    }
    data class Error(
        val message: String,
        val isNotFound: Boolean = false,
    ) : RequestDetailUiState
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
                        if (error is ApiDomainError && error.code == "ORDER_REQUEST_NOT_ACTIONABLE") {
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
        load()
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