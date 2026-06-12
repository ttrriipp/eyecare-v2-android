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
import java.time.LocalDate
import javax.inject.Inject

sealed interface PrescriptionListUiState {
    data object Loading : PrescriptionListUiState
    data class Success(val prescriptions: List<Prescription>) : PrescriptionListUiState
    data object Empty : PrescriptionListUiState
    data class Error(val message: String) : PrescriptionListUiState
}

sealed interface PrescriptionDetailUiState {
    data object Idle : PrescriptionDetailUiState
    data object Loading : PrescriptionDetailUiState
    data class Success(val prescription: Prescription) : PrescriptionDetailUiState
    data class Error(val message: String) : PrescriptionDetailUiState
}

@HiltViewModel
class PrescriptionViewModel @Inject constructor(
    private val repository: PrescriptionRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<PrescriptionListUiState>(PrescriptionListUiState.Loading)
    val listState: StateFlow<PrescriptionListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<PrescriptionDetailUiState>(PrescriptionDetailUiState.Idle)
    val detailState: StateFlow<PrescriptionDetailUiState> = _detailState.asStateFlow()

    init { loadList() }

    fun refresh() { loadList() }

    fun loadDetail(id: Int) {
        _detailState.value = PrescriptionDetailUiState.Loading
        viewModelScope.launch {
            _detailState.value = repository.getPrescription(id).fold(
                onSuccess = { PrescriptionDetailUiState.Success(it) },
                onFailure = { PrescriptionDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    /** Returns true if expiresAt is a past date. */
    fun isExpired(expiresAt: String?): Boolean {
        if (expiresAt == null) return false
        return runCatching { LocalDate.parse(expiresAt.take(10)).isBefore(LocalDate.now()) }
            .getOrElse { false }
    }

    private fun loadList() {
        _listState.value = PrescriptionListUiState.Loading
        viewModelScope.launch {
            _listState.value = repository.getPrescriptions().fold(
                onSuccess = { list ->
                    if (list.isEmpty()) PrescriptionListUiState.Empty
                    else PrescriptionListUiState.Success(list.sortedByDescending { it.prescribedAt })
                },
                onFailure = { PrescriptionListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
