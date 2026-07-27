package com.eyecare.app.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.repository.FrameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class FrameSortOption(val apiValue: String, val label: String) {
    NAME("name", "Name"),
    NEWEST("newest", "Newest"),
}

data class FrameListFilters(
    val search: String = "",
    val sort: FrameSortOption = FrameSortOption.NAME,
)

sealed interface FrameListUiState {
    data object Loading : FrameListUiState
    data class Success(
        val frames: List<Frame>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val filters: FrameListFilters = FrameListFilters(),
    ) : FrameListUiState
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

    init { load() }

    fun refresh() {
        currentPage = 1
        currentFrames.clear()
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

    fun loadMore() {
        val current = _uiState.value as? FrameListUiState.Success ?: return
        if (current.isLoadingMore || !current.hasMorePages) return
        _uiState.value = current.copy(isLoadingMore = true)
        viewModelScope.launch {
            val result = repository.getFrames(
                page = currentPage + 1,
                search = filters.search.takeIf { it.isNotBlank() },
                sort = filters.sort.apiValue,
            )
            result.fold(
                onSuccess = { frames ->
                    currentPage++
                    currentFrames.addAll(frames)
                    val hasMore = repository.hasMorePages(currentPage)
                    _uiState.value = current.copy(
                        frames = currentFrames.toList(),
                        isLoadingMore = false,
                        hasMorePages = hasMore,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(isLoadingMore = false)
                },
            )
        }
    }

    private fun resetAndLoad() {
        currentPage = 1
        currentFrames.clear()
        load()
    }

    private fun load() {
        val current = _uiState.value
        if (current !is FrameListUiState.Success) {
            _uiState.value = FrameListUiState.Loading
        }
        viewModelScope.launch {
            repository.getFrames(
                page = 1,
                search = filters.search.takeIf { it.isNotBlank() },
                sort = filters.sort.apiValue,
            ).fold(
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
                onFailure = {
                    _uiState.value = FrameListUiState.Error(it.message ?: "Failed to load frames")
                },
            )
        }
    }
}
