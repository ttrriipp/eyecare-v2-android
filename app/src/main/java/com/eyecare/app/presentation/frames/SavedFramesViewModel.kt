package com.eyecare.app.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.SavedFrame
import com.eyecare.app.domain.repository.SavedFrameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SavedFramesInlineErrorAction {
    data object RetryRefresh : SavedFramesInlineErrorAction
    data object RetryLoadMore : SavedFramesInlineErrorAction
    data class RetryRemove(val productVariantId: Int) : SavedFramesInlineErrorAction
}

sealed interface SavedFramesUiState {
    data object Loading : SavedFramesUiState

    data class Success(
        val items: List<SavedFrame>,
        val currentPage: Int,
        val canLoadMore: Boolean,
        val removingVariantIds: Set<Int> = emptySet(),
        val isRefreshing: Boolean = false,
        val isLoadingMore: Boolean = false,
        val inlineError: String? = null,
        val inlineErrorAction: SavedFramesInlineErrorAction? = null,
        val successMessage: String? = null,
    ) : SavedFramesUiState

    data class Error(val patientSafeMessage: String) : SavedFramesUiState
}

@HiltViewModel
class SavedFramesViewModel @Inject constructor(
    private val repository: SavedFrameRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<SavedFramesUiState>(SavedFramesUiState.Loading)
    val uiState: StateFlow<SavedFramesUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() {
        val current = _uiState.value
        if (current is SavedFramesUiState.Success) {
            _uiState.value = current.copy(
                isRefreshing = true,
                inlineError = null,
                inlineErrorAction = null,
                successMessage = null,
            )
        } else {
            _uiState.value = SavedFramesUiState.Loading
        }
        load()
    }

    fun loadMore() {
        val current = _uiState.value as? SavedFramesUiState.Success ?: return
        if (!current.canLoadMore || current.isLoadingMore) return

        _uiState.value = current.copy(
            isLoadingMore = true,
            inlineError = null,
            inlineErrorAction = null,
            successMessage = null,
        )
        viewModelScope.launch {
            repository.getSavedFrames(page = current.currentPage + 1).fold(
                onSuccess = { page ->
                    val latest = _uiState.value as? SavedFramesUiState.Success ?: return@launch
                    val existingIds = latest.items.map { it.productVariantId }.toSet()
                    val newItems = page.items.filter { it.productVariantId !in existingIds }
                    _uiState.value = latest.copy(
                        items = latest.items + newItems,
                        currentPage = page.currentPage,
                        canLoadMore = page.currentPage < page.lastPage,
                        isLoadingMore = false,
                        inlineErrorAction = null,
                        successMessage = null,
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? SavedFramesUiState.Success ?: return@launch
                    _uiState.value = latest.copy(
                        isLoadingMore = false,
                        inlineError = "Couldn't load more. Try again.",
                        inlineErrorAction = SavedFramesInlineErrorAction.RetryLoadMore,
                        successMessage = null,
                    )
                },
            )
        }
    }

    fun removeSavedFrame(productVariantId: Int) {
        val current = _uiState.value as? SavedFramesUiState.Success ?: return
        if (productVariantId in current.removingVariantIds) return

        _uiState.value = current.copy(
            removingVariantIds = current.removingVariantIds + productVariantId,
            inlineError = null,
            inlineErrorAction = null,
            successMessage = null,
        )
        viewModelScope.launch {
            repository.remove(productVariantId).fold(
                onSuccess = {
                    val latest = _uiState.value as? SavedFramesUiState.Success ?: return@launch
                    val removedFrame = latest.items.firstOrNull { it.productVariantId == productVariantId }
                    _uiState.value = latest.copy(
                        items = latest.items.filter { it.productVariantId != productVariantId },
                        removingVariantIds = latest.removingVariantIds - productVariantId,
                        inlineError = null,
                        inlineErrorAction = null,
                        successMessage = "Removed " +
                            (removedFrame?.variant?.product?.name ?: "Frame") +
                            " from saved frames.",
                    )
                },
                onFailure = {
                    val latest = _uiState.value as? SavedFramesUiState.Success ?: return@launch
                    _uiState.value = latest.copy(
                        removingVariantIds = latest.removingVariantIds - productVariantId,
                        inlineError = "Couldn't remove this frame. Try again.",
                        inlineErrorAction = SavedFramesInlineErrorAction.RetryRemove(productVariantId),
                        successMessage = null,
                    )
                },
            )
        }
    }

    fun clearInlineError() {
        val current = _uiState.value as? SavedFramesUiState.Success ?: return
        _uiState.value = current.copy(inlineError = null, inlineErrorAction = null)
    }

    fun clearSuccessMessage() {
        val current = _uiState.value as? SavedFramesUiState.Success ?: return
        _uiState.value = current.copy(successMessage = null)
    }

    private fun load() {
        viewModelScope.launch {
            repository.getSavedFrames(page = 1).fold(
                onSuccess = { page ->
                    val previous = _uiState.value
                    val items = page.items
                    if (items.isEmpty()) {
                        _uiState.value = SavedFramesUiState.Success(
                            items = emptyList(),
                            currentPage = 1,
                            canLoadMore = false,
                        )
                    } else {
                        val previousRemoving = (previous as? SavedFramesUiState.Success)?.removingVariantIds ?: emptySet()
                        _uiState.value = SavedFramesUiState.Success(
                            items = items,
                            currentPage = page.currentPage,
                            canLoadMore = page.currentPage < page.lastPage,
                            removingVariantIds = previousRemoving,
                            inlineErrorAction = null,
                            successMessage = null,
                        )
                    }
                },
                onFailure = {
                    val previous = _uiState.value
                    if (previous is SavedFramesUiState.Success && previous.items.isNotEmpty()) {
                        _uiState.value = previous.copy(
                            isRefreshing = false,
                            inlineError = "Couldn't refresh. Try again.",
                            inlineErrorAction = SavedFramesInlineErrorAction.RetryRefresh,
                            successMessage = null,
                        )
                    } else {
                        _uiState.value = SavedFramesUiState.Error(
                            "We couldn't load your saved frames. Check your connection and try again.",
                        )
                    }
                },
            )
        }
    }
}
