package com.eyecare.app.presentation.billingrecords

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.BillingRecord
import com.eyecare.app.domain.repository.BillingRecordRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BillingRecordDetailUiState {
    data object Loading : BillingRecordDetailUiState
    data class Success(val record: BillingRecord) : BillingRecordDetailUiState
    data class Error(val message: String) : BillingRecordDetailUiState
}

@HiltViewModel
class BillingRecordDetailViewModel @Inject constructor(
    private val repository: BillingRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BillingRecordDetailUiState>(BillingRecordDetailUiState.Loading)
    val uiState: StateFlow<BillingRecordDetailUiState> = _uiState.asStateFlow()

    private var currentId: Int = 0

    fun load(id: Int) {
        currentId = id
        _uiState.value = BillingRecordDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getBillingRecord(id).fold(
                onSuccess = { BillingRecordDetailUiState.Success(it) },
                onFailure = { BillingRecordDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retry() {
        if (currentId != 0) load(currentId)
    }
}
