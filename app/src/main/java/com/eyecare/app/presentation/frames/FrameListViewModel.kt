package com.eyecare.app.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.isArReady
import com.eyecare.app.domain.repository.FrameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FrameSortOption(val apiValue: String, val label: String) {
    NAME("name", "Name"),
    NEWEST("newest", "Newest"),
}

data class FrameListFilters(
    val search: String = "",
    val sort: FrameSortOption = FrameSortOption.NAME,
    val brand: String? = null,
    val category: String? = null,
    val arOnly: Boolean = false,
) {
    val hasLocalFilters: Boolean
        get() = brand != null || category != null || arOnly

    fun matches(frame: Frame): Boolean =
        (brand == null || frame.brand.equals(brand, ignoreCase = true)) &&
            (category == null || frame.category.equals(category, ignoreCase = true)) &&
            (!arOnly || frame.variants.any { it.isArReady })
}

sealed interface FrameListUiState {
    data object Loading : FrameListUiState
    data class Success(
        val frames: List<Frame>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val filters: FrameListFilters = FrameListFilters(),
        val isRefreshing: Boolean = false,
        val message: String? = null,
    ) : FrameListUiState {
        val visibleFrames: List<Frame>
            get() = frames.filter(filters::matches)
    }
    data class Error(val message: String) : FrameListUiState
}

@HiltViewModel
class FrameListViewModel @Inject constructor(
    private val repository: FrameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<FrameListUiState>(FrameListUiState.Loading)
    val uiState: StateFlow<FrameListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var currentFrames = mutableListOf<Frame>()
    private var filters = FrameListFilters()
    private var searchJob: Job? = null
    private var loadJob: Job? = null

    init { load() }

    fun refresh() {
        currentPage = 1
        load()
    }

    fun search(query: String) {
        filters = filters.copy(search = query)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            resetAndLoad()
        }
    }

    fun selectSort(sort: FrameSortOption) {
        filters = filters.copy(sort = sort)
        resetAndLoad()
    }

    fun selectBrand(brand: String?) {
        updateLocalFilters { copy(brand = brand) }
    }

    fun selectCategory(category: String?) {
        updateLocalFilters { copy(category = category) }
    }

    fun setArOnly(enabled: Boolean) {
        updateLocalFilters { copy(arOnly = enabled) }
    }

    fun clearCatalogFilters() {
        updateLocalFilters { copy(brand = null, category = null, arOnly = false) }
    }

    fun clearSearch() {
        searchJob?.cancel()
        filters = filters.copy(search = "")
        resetAndLoad()
    }

    fun clearMessage() {
        val current = _uiState.value as? FrameListUiState.Success ?: return
        _uiState.value = current.copy(message = null)
    }

    fun loadMore() {
        val current = _uiState.value as? FrameListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        _uiState.value = current.copy(isLoadingMore = true, message = null)
        val page = currentPage + 1
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = repository.getFrames(
                page = page,
                search = filters.search.takeIf { it.isNotBlank() },
                sort = filters.sort.apiValue,
            )
            if (!isActive) return@launch
            result.fold(
                onSuccess = { frames ->
                    currentPage = page
                    frames.forEach { frame ->
                        if (currentFrames.none { it.id == frame.id }) currentFrames.add(frame)
                    }
                    val hasMore = repository.hasMorePages(currentPage)
                    _uiState.value = current.copy(
                        frames = currentFrames.toList(),
                        isLoadingMore = false,
                        hasMorePages = hasMore,
                        filters = filters,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        message = "Couldn't load more frames. Please try again.",
                    )
                },
            )
        }
    }

    private fun resetAndLoad() {
        currentPage = 1
        load()
    }

    private fun load() {
        val current = _uiState.value
        if (current !is FrameListUiState.Success) {
            _uiState.value = FrameListUiState.Loading
        } else {
            _uiState.value = current.copy(
                isRefreshing = true,
                isLoadingMore = false,
                filters = filters,
                message = null,
            )
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = repository.getFrames(
                page = 1,
                search = filters.search.takeIf { it.isNotBlank() },
                sort = filters.sort.apiValue,
            )
            if (!isActive) return@launch
            result.fold(
                onSuccess = { frames ->
                    currentFrames.clear()
                    currentFrames.addAll(frames)
                    val hasMore = repository.hasMorePages(1)
                    _uiState.value = FrameListUiState.Success(
                        frames = frames,
                        hasMorePages = hasMore,
                        filters = filters,
                    )
                },
                onFailure = { error ->
                    if (current is FrameListUiState.Success) {
                        _uiState.value = current.copy(
                            isRefreshing = false,
                            filters = filters,
                            message = "Couldn't refresh frames. Please try again.",
                        )
                    } else {
                        _uiState.value = FrameListUiState.Error(
                            error.message ?: "We couldn't load frames. Please try again.",
                        )
                    }
                },
            )
        }
    }

    private fun updateLocalFilters(update: FrameListFilters.() -> FrameListFilters) {
        filters = filters.update()
        val current = _uiState.value as? FrameListUiState.Success ?: return
        _uiState.value = current.copy(filters = filters, message = null)
    }
}
