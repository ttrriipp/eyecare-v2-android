package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.EyewearSummary
import com.eyecare.app.domain.repository.EyewearRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class EyewearFilter(val apiValue: String, val label: String) {
    CURRENT("current", "Current"),
    HISTORY("history", "History"),
}

sealed interface EyewearListUiState {
    data object Loading : EyewearListUiState
    data class Success(
        val items: List<EyewearSummary>,
        val filter: EyewearFilter,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
    ) : EyewearListUiState
    data class Empty(val filter: EyewearFilter) : EyewearListUiState
    data class Error(val message: String) : EyewearListUiState
}

@HiltViewModel
class EyewearListViewModel @Inject constructor(
    private val repository: EyewearRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EyewearListUiState>(EyewearListUiState.Loading)
    val uiState: StateFlow<EyewearListUiState> = _uiState.asStateFlow()

    private var currentFilter = EyewearFilter.CURRENT
    private var currentPage = 1
    private var loadSequence = 0

    init { load(EyewearFilter.CURRENT) }

    fun selectFilter(filter: EyewearFilter) {
        if (filter == currentFilter) return
        currentFilter = filter
        currentPage = 1
        load(filter)
    }

    fun refresh() {
        currentPage = 1
        load(currentFilter)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is EyewearListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    private fun load(filter: EyewearFilter) {
        val seq = ++loadSequence
        _uiState.value = EyewearListUiState.Loading
        viewModelScope.launch {
            repository.getEyewear(filter = filter.apiValue, page = 1).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _uiState.value = EyewearListUiState.Empty(filter)
                    else _uiState.value = EyewearListUiState.Success(
                        items = result.data,
                        filter = filter,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = EyewearListUiState.Error(it.message ?: "Failed to load")
                },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? EyewearListUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getEyewear(filter = currentFilter.apiValue, page = currentPage).fold(
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
