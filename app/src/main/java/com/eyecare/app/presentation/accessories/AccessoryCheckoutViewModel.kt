package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
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
    data class Success(val requestId: Int) : CheckoutUiState
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

    fun submit(discountType: String, items: List<Pair<Int, Int>>) {
        if (isSubmitting) return
        isSubmitting = true
        _uiState.value = CheckoutUiState.Submitting

        viewModelScope.launch {
            try {
                repository.submitRequest(discountType, items).fold(
                    onSuccess = { request ->
                        _uiState.value = CheckoutUiState.Success(requestId = request.id)
                    },
                    onFailure = { error ->
                        val isConflict = error is ApiDomainError && error.code == "ACTIVE_ORDER_REQUEST_EXISTS"
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

    fun reset() {
        _uiState.value = CheckoutUiState.Idle
    }
}