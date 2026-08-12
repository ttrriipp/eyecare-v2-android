package com.eyecare.app.presentation.eyewear

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

sealed interface EstimateListUiState {
    data object Loading : EstimateListUiState
    data class Success(
        val items: List<Quotation>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
    ) : EstimateListUiState
    data object Empty : EstimateListUiState
    data class Error(val message: String) : EstimateListUiState
}

@HiltViewModel
class EstimateListViewModel @Inject constructor(
    private val repository: QuotationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EstimateListUiState>(EstimateListUiState.Loading)
    val uiState: StateFlow<EstimateListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var loadSequence = 0

    init { load() }

    fun refresh() {
        currentPage = 1
        load()
    }

    fun retry() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is EstimateListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load() {
        val seq = ++loadSequence
        _uiState.value = EstimateListUiState.Loading
        viewModelScope.launch {
            repository.getQuotations(filter = null, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = EstimateListUiState.Empty
                    else _uiState.value = EstimateListUiState.Success(
                        items = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = EstimateListUiState.Error(it.message ?: "Failed to load estimates")
                },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? EstimateListUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getQuotations(filter = null, page = currentPage).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    val all = current.items + result.data
                    _uiState.value = current.copy(
                        items = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        loadMoreError = it.message ?: "Failed to load more",
                    )
                },
            )
        }
    }
}
