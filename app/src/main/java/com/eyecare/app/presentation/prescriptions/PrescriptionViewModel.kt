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
    data class Success(
        val prescriptions: List<Prescription>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : PrescriptionListUiState
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

    private var currentPage = 1

    init { loadList() }

    fun refresh() {
        currentPage = 1
        loadList()
    }

    fun loadMore() {
        val state = _listState.value
        if (state !is PrescriptionListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    fun loadDetail(id: Int) {
        _detailState.value = PrescriptionDetailUiState.Loading
        viewModelScope.launch {
            _detailState.value = repository.getPrescription(id).fold(
                onSuccess = { PrescriptionDetailUiState.Success(it) },
                onFailure = { PrescriptionDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun isExpired(expiresAt: String?): Boolean {
        if (expiresAt == null) return false
        return runCatching { LocalDate.parse(expiresAt.take(10)).isBefore(LocalDate.now()) }
            .getOrElse { false }
    }

    private fun loadList() {
        _listState.value = PrescriptionListUiState.Loading
        viewModelScope.launch {
            repository.getPrescriptions(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _listState.value = PrescriptionListUiState.Empty
                    else _listState.value = PrescriptionListUiState.Success(
                        prescriptions = result.data.sortedByDescending { it.prescribedAt },
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = PrescriptionListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _listState.value as? PrescriptionListUiState.Success ?: return
        _listState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            repository.getPrescriptions(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.prescriptions + result.data
                    _listState.value = current.copy(
                        prescriptions = all.sortedByDescending { it.prescribedAt },
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = current.copy(isLoadingMore = false) },
            )
        }
    }
}
