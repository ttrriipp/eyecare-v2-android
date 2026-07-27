package com.eyecare.app.presentation.frames

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Frame
import com.eyecare.app.domain.model.FrameVariant
import com.eyecare.app.domain.repository.FrameRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FrameDetailUiState {
    data object Loading : FrameDetailUiState
    data class Success(
        val frame: Frame,
        val selectedVariant: FrameVariant,
    ) : FrameDetailUiState
    data class Error(val message: String) : FrameDetailUiState
}

@HiltViewModel(assistedFactory = FrameDetailViewModel.Factory::class)
class FrameDetailViewModel @AssistedInject constructor(
    private val repository: FrameRepository,
    @Assisted private val frameId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(frameId: Int): FrameDetailViewModel
    }

    private val _uiState = MutableStateFlow<FrameDetailUiState>(FrameDetailUiState.Loading)
    val uiState: StateFlow<FrameDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun selectVariant(variant: FrameVariant) {
        val current = _uiState.value as? FrameDetailUiState.Success ?: return
        _uiState.value = current.copy(selectedVariant = variant)
    }

    fun refresh() = load()

    private fun load() {
        viewModelScope.launch {
            _uiState.value = repository.getFrame(frameId).fold(
                onSuccess = { frame ->
                    val firstVariant = frame.variants.firstOrNull()
                        ?: return@fold FrameDetailUiState.Error("No variants available")
                    FrameDetailUiState.Success(frame = frame, selectedVariant = firstVariant)
                },
                onFailure = { FrameDetailUiState.Error(it.message ?: "Failed to load frame") },
            )
        }
    }
}
