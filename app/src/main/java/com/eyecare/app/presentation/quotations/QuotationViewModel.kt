package com.eyecare.app.presentation.quotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Quotation
import com.eyecare.app.domain.repository.QuotationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuotationListUiState {
    data object Loading : QuotationListUiState
    data class Success(
        val quotations: List<Quotation>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : QuotationListUiState
    data object Empty : QuotationListUiState
    data class Error(val message: String) : QuotationListUiState
}

sealed interface QuotationDetailUiState {
    data object Loading : QuotationDetailUiState
    data class Success(val quotation: Quotation) : QuotationDetailUiState
    data class Error(val message: String) : QuotationDetailUiState
}

@HiltViewModel
class QuotationViewModel @Inject constructor(
    private val repository: QuotationRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<QuotationListUiState>(QuotationListUiState.Loading)
    val listState: StateFlow<QuotationListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<QuotationDetailUiState>(QuotationDetailUiState.Loading)
    val detailState: StateFlow<QuotationDetailUiState> = _detailState.asStateFlow()

    private var currentPage = 1

    init { loadList() }

    fun refresh() { currentPage = 1; loadList() }

    fun loadMore() {
        val state = _listState.value as? QuotationListUiState.Success ?: return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    fun loadDetail(id: Int) {
        _detailState.value = QuotationDetailUiState.Loading
        viewModelScope.launch {
            _detailState.value = repository.getQuotation(id).fold(
                onSuccess = { QuotationDetailUiState.Success(it) },
                onFailure = { QuotationDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadList() {
        _listState.value = QuotationListUiState.Loading
        viewModelScope.launch {
            repository.getQuotations(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _listState.value = QuotationListUiState.Empty
                    else _listState.value = QuotationListUiState.Success(
                        quotations = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = QuotationListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _listState.value as? QuotationListUiState.Success ?: return
        _listState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            repository.getQuotations(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.quotations + result.data
                    _listState.value = current.copy(
                        quotations = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = current.copy(isLoadingMore = false) },
            )
        }
    }
}
