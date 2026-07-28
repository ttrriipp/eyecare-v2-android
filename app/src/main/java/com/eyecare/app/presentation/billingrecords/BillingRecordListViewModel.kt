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

sealed interface BillingRecordListUiState {
    data object Loading : BillingRecordListUiState
    data class Success(
        val records: List<BillingRecord>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
    ) : BillingRecordListUiState
    data object Empty : BillingRecordListUiState
    data class Error(val message: String) : BillingRecordListUiState
}

@HiltViewModel
class BillingRecordListViewModel @Inject constructor(
    private val repository: BillingRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BillingRecordListUiState>(BillingRecordListUiState.Loading)
    val uiState: StateFlow<BillingRecordListUiState> = _uiState.asStateFlow()

    private var currentPage = 1

    init { load() }

    fun refresh() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is BillingRecordListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load() {
        _uiState.value = BillingRecordListUiState.Loading
        viewModelScope.launch {
            repository.getBillingRecords(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = BillingRecordListUiState.Empty
                    else _uiState.value = BillingRecordListUiState.Success(
                        records = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _uiState.value = BillingRecordListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? BillingRecordListUiState.Success ?: return
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getBillingRecords(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.records + result.data
                    _uiState.value = current.copy(
                        records = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        loadMoreError = it.message ?: "Failed to load more",
                    )
                },
            )
        }
    }
}
