package com.eyecare.app.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import com.eyecare.app.domain.repository.SavedFrameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface FrameDetailUiState {
    data object Loading : FrameDetailUiState
    data class Success(
        val frame: Frame,
        val selectedVariant: FrameVariant,
        val isRefreshing: Boolean = false,
        val isSavingVariant: Boolean = false,
        val saveError: String? = null,
        val message: String? = null,
    ) : FrameDetailUiState
    data class Error(val message: String) : FrameDetailUiState
}

@HiltViewModel(assistedFactory = FrameDetailViewModel.Factory::class)
class FrameDetailViewModel @AssistedInject constructor(
    private val repository: FrameRepository,
    private val savedFrameRepository: SavedFrameRepository,
    @Assisted private val frameId: Int,
    @Assisted private val requestedVariantId: Int?,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(frameId: Int, requestedVariantId: Int?): FrameDetailViewModel
    }

    private val _uiState = MutableStateFlow<FrameDetailUiState>(FrameDetailUiState.Loading)
    val uiState: StateFlow<FrameDetailUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var saveJob: Job? = null

    init { load() }

    fun selectVariant(variant: FrameVariant) {
        val current = _uiState.value as? FrameDetailUiState.Success ?: return
        _uiState.value = current.copy(selectedVariant = variant, message = null, saveError = null)
    }

    fun refresh() {
        val current = _uiState.value
        if (current is FrameDetailUiState.Success) {
            _uiState.value = current.copy(isRefreshing = true, message = null)
        } else {
            _uiState.value = FrameDetailUiState.Loading
        }
        load()
    }

    fun clearMessage() {
        val current = _uiState.value as? FrameDetailUiState.Success ?: return
        _uiState.value = current.copy(message = null)
    }

    fun toggleSaved() {
        val current = _uiState.value as? FrameDetailUiState.Success ?: return
        if (current.isSavingVariant || saveJob?.isActive == true) return

        val variant = current.selectedVariant
        _uiState.value = current.copy(isSavingVariant = true, saveError = null)

        saveJob = viewModelScope.launch {
            val result = if (variant.isSaved) {
                savedFrameRepository.remove(variant.id)
            } else {
                savedFrameRepository.save(variant.id)
            }
            result.fold(
                onSuccess = {
                    val latest = _uiState.value as? FrameDetailUiState.Success ?: return@launch
                    val updatedVariant = variant.copy(isSaved = !variant.isSaved)
                    val updatedFrame = latest.frame.copy(
                        variants = latest.frame.variants.map {
                            if (it.id == variant.id) updatedVariant else it
                        },
                    )
                    val selectedVariant = updatedFrame.variants
                        .firstOrNull { it.id == latest.selectedVariant.id }
                        ?: latest.selectedVariant
                    _uiState.value = latest.copy(
                        frame = updatedFrame,
                        selectedVariant = selectedVariant,
                        isSavingVariant = false,
                    )
                },
                onFailure = { error ->
                    val latest = _uiState.value as? FrameDetailUiState.Success ?: return@launch
                    val message = when {
                        !variant.isSaved && (error as? ApiDomainError)?.httpStatus == 422 ->
                            "This option can no longer be saved. Try refreshing."
                        variant.isSaved -> "Couldn't remove this frame. Try again."
                        else -> "Couldn't save this frame. Try again."
                    }
                    _uiState.value = latest.copy(
                        isSavingVariant = false,
                        saveError = message,
                    )
                },
            )
        }
    }

    fun clearSaveError() {
        val current = _uiState.value as? FrameDetailUiState.Success ?: return
        _uiState.value = current.copy(saveError = null)
    }

    private fun load() {
        val previous = _uiState.value
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val result = repository.getFrame(frameId)
            if (!isActive) return@launch
            _uiState.value = result.fold(
                onSuccess = { frame ->
                    val firstVariant = frame.variants.firstOrNull()
                        ?: return@fold if (previous is FrameDetailUiState.Success) {
                            previous.copy(
                                isRefreshing = false,
                                message = "This frame has no available options.",
                            )
                        } else {
                            FrameDetailUiState.Error("This frame has no available options.")
                        }
                    val previousVariantId = (previous as? FrameDetailUiState.Success)?.selectedVariant?.id
                    val selectedVariant = frame.variants.firstOrNull { it.id == previousVariantId }
                        ?: frame.variants.firstOrNull { it.id == requestedVariantId }
                        ?: frame.variants.firstOrNull { it.name == (previous as? FrameDetailUiState.Success)?.selectedVariant?.name }
                        ?: firstVariant
                    FrameDetailUiState.Success(
                        frame = frame,
                        selectedVariant = selectedVariant,
                        isSavingVariant = saveJob?.isActive == true,
                    )
                },
                onFailure = {
                    if (previous is FrameDetailUiState.Success) {
                        previous.copy(
                            isRefreshing = false,
                            message = "Couldn't refresh frame. Please try again.",
                        )
                    } else {
                        FrameDetailUiState.Error(
                            it.message ?: "We couldn't load this frame. Please try again.",
                        )
                    }
                },
            )
        }
    }
}
