package com.eyecare.app.presentation.prescriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Prescription
import com.eyecare.app.domain.repository.PrescriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface PrescriptionDetailUiState {
    data object Loading : PrescriptionDetailUiState
    data class Success(val prescription: Prescription) : PrescriptionDetailUiState
    data class Error(val message: String) : PrescriptionDetailUiState
}

@HiltViewModel
class PrescriptionDetailViewModel @Inject constructor(
    private val repository: PrescriptionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrescriptionDetailUiState>(PrescriptionDetailUiState.Loading)
    val uiState: StateFlow<PrescriptionDetailUiState> = _uiState.asStateFlow()

    private var currentId: Int = 0

    fun load(id: Int) {
        currentId = id
        _uiState.value = PrescriptionDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getPrescription(id).fold(
                onSuccess = { PrescriptionDetailUiState.Success(it) },
                onFailure = { PrescriptionDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retry() {
        if (currentId != 0) load(currentId)
    }
}
