package com.eyecare.app.presentation.joborders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.JobOrder
import com.eyecare.app.domain.repository.JobOrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface JobOrderListUiState {
    data object Loading : JobOrderListUiState
    data class Success(
        val jobOrders: List<JobOrder>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
    ) : JobOrderListUiState
    data object Empty : JobOrderListUiState
    data class Error(val message: String) : JobOrderListUiState
}

sealed interface JobOrderDetailUiState {
    data object Loading : JobOrderDetailUiState
    data class Success(val jobOrder: JobOrder) : JobOrderDetailUiState
    data class Error(val message: String) : JobOrderDetailUiState
}

@HiltViewModel
class JobOrderViewModel @Inject constructor(
    private val repository: JobOrderRepository,
) : ViewModel() {

    private val _listState = MutableStateFlow<JobOrderListUiState>(JobOrderListUiState.Loading)
    val listState: StateFlow<JobOrderListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow<JobOrderDetailUiState>(JobOrderDetailUiState.Loading)
    val detailState: StateFlow<JobOrderDetailUiState> = _detailState.asStateFlow()

    private var currentPage = 1

    init { loadList() }

    fun refresh() { currentPage = 1; loadList() }

    fun loadMore() {
        val state = _listState.value as? JobOrderListUiState.Success ?: return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    fun loadDetail(id: Int) {
        _detailState.value = JobOrderDetailUiState.Loading
        viewModelScope.launch {
            _detailState.value = repository.getJobOrder(id).fold(
                onSuccess = { JobOrderDetailUiState.Success(it) },
                onFailure = { JobOrderDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadList() {
        _listState.value = JobOrderListUiState.Loading
        viewModelScope.launch {
            repository.getJobOrders(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    if (result.data.isEmpty()) _listState.value = JobOrderListUiState.Empty
                    else _listState.value = JobOrderListUiState.Success(
                        jobOrders = result.data,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = JobOrderListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _listState.value as? JobOrderListUiState.Success ?: return
        _listState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            repository.getJobOrders(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    val all = current.jobOrders + result.data
                    _listState.value = current.copy(
                        jobOrders = all,
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = { _listState.value = current.copy(isLoadingMore = false) },
            )
        }
    }
}
