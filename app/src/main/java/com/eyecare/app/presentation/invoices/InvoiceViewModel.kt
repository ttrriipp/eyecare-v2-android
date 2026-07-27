package com.eyecare.app.presentation.invoices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Invoice
import com.eyecare.app.domain.repository.InvoiceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface InvoiceListUiState {
    data object Loading : InvoiceListUiState
    data class Success(
        val invoices: List<Invoice>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : InvoiceListUiState
    data object Empty : InvoiceListUiState
    data class Error(val message: String) : InvoiceListUiState
}

sealed interface InvoiceDetailUiState {
    data object Loading : InvoiceDetailUiState
    data class Success(val invoice: Invoice) : InvoiceDetailUiState
    data class Error(val message: String) : InvoiceDetailUiState
}

@HiltViewModel
class InvoiceViewModel @Inject constructor(
    private val repository: InvoiceRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<InvoiceListUiState>(InvoiceListUiState.Loading)
    val listState: StateFlow<InvoiceListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<InvoiceDetailUiState>(InvoiceDetailUiState.Loading)
    val detailState: StateFlow<InvoiceDetailUiState> = _detailState.asStateFlow()

    private var currentPage = 1

    init { loadList() }

    fun refresh() { currentPage = 1; loadList() }

    fun loadMore() {
        val state = _listState.value as? InvoiceListUiState.Success ?: return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    fun loadDetail(id: Int) {
        _detailState.value = InvoiceDetailUiState.Loading
        viewModelScope.launch {
            _detailState.value = repository.getInvoice(id).fold(
                onSuccess = { InvoiceDetailUiState.Success(it) },
                onFailure = { InvoiceDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadList() {
        _listState.value = InvoiceListUiState.Loading
        viewModelScope.launch {
            repository.getInvoices(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _listState.value = InvoiceListUiState.Empty
                    else _listState.value = InvoiceListUiState.Success(
                        invoices = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = InvoiceListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _listState.value as? InvoiceListUiState.Success ?: return
        _listState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            repository.getInvoices(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.invoices + result.data
                    _listState.value = current.copy(
                        invoices = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = current.copy(isLoadingMore = false) },
            )
        }
    }
}
