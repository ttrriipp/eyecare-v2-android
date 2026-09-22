package com.eyecare.app.presentation.accessories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Accessory
import com.eyecare.app.domain.model.AccessoryQuery
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.repository.AccessoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccessoryCatalogUiState {
    data object Loading : AccessoryCatalogUiState
    data class Success(
        val items: List<Accessory>,
        val hasMorePages: Boolean = false,
        val isLoadingMore: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
    ) : AccessoryCatalogUiState
    data object Empty : AccessoryCatalogUiState
    data class Error(val message: String) : AccessoryCatalogUiState
}

@HiltViewModel
class AccessoryCatalogViewModel @Inject constructor(
    private val repository: AccessoryRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AccessoryCatalogUiState>(AccessoryCatalogUiState.Loading)
    val uiState: StateFlow<AccessoryCatalogUiState> = _uiState.asStateFlow()

    private var currentQuery = AccessoryQuery()
    private var currentPage = 1
    private var loadSequence = 0
    private var searchJob: Job? = null

    init { load() }

    fun updateSearch(search: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val normalizedSearch = search.trim().take(100).ifBlank { null }
            currentQuery = currentQuery.copy(search = normalizedSearch, page = 1)
            currentPage = 1
            load()
        }
    }

    fun updateSort(sort: String?) {
        currentQuery = currentQuery.copy(sort = sort, page = 1)
        currentPage = 1
        load()
    }

    fun updateMinimumRating(rating: Int?) {
        currentQuery = currentQuery.copy(minimumRating = rating, page = 1)
        currentPage = 1
        load()
    }

    fun updateRated(rated: String?) {
        currentQuery = currentQuery.copy(rated = rated, page = 1)
        currentPage = 1
        load()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is AccessoryCatalogUiState.Success) {
            refreshInternal(current)
        } else {
            currentPage = 1
            load()
        }
    }

    private fun refreshInternal(current: AccessoryCatalogUiState.Success) {
        val seq = ++loadSequence
        _uiState.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.getAccessories(currentQuery.copy(page = 1)).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    _uiState.value = if (result.data.isEmpty()) {
                        AccessoryCatalogUiState.Empty
                    } else {
                        AccessoryCatalogUiState.Success(
                            items = result.data,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = {
                    if (seq != loadSequence) return@launch
                    _uiState.value = current.copy(isRefreshing = false)
                },
            )
        }
    }

    fun retry() {
        currentPage = 1
        load()
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is AccessoryCatalogUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        loadMoreInternal(currentPage + 1)
    }

    private fun load() {
        currentPage = 1
        val seq = ++loadSequence
        _uiState.value = AccessoryCatalogUiState.Loading
        viewModelScope.launch {
            repository.getAccessories(currentQuery).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    _uiState.value = if (result.data.isEmpty()) {
                        AccessoryCatalogUiState.Empty
                    } else {
                        AccessoryCatalogUiState.Success(
                            items = result.data,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = { error ->
                    if (seq != loadSequence) return@launch
                    _uiState.value = AccessoryCatalogUiState.Error(
                        message = catalogLoadErrorMessage(error),
                    )
                },
            )
        }
    }

    private fun loadMoreInternal(page: Int) {
        val current = _uiState.value as? AccessoryCatalogUiState.Success ?: return
        val seq = loadSequence
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getAccessories(currentQuery.copy(page = page)).fold(
                onSuccess = { result ->
                    if (seq != loadSequence) return@launch
                    currentPage = result.currentPage
                    val all = (current.items + result.data).distinctBy { it.id }
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

    private fun catalogLoadErrorMessage(error: Throwable): String = when (error) {
        is ApiDomainError -> when (error.code) {
            AuthApiCodes.ACTIVE_PATIENT_LINK_REQUIRED ->
                "Link your clinic account to browse accessories."
            else -> error.message.takeIf(String::isNotBlank) ?: GENERIC_LOAD_ERROR
        }
        else -> GENERIC_LOAD_ERROR
    }

    private companion object {
        const val GENERIC_LOAD_ERROR = "We couldn't load accessories. Check your connection and try again."
    }
}
