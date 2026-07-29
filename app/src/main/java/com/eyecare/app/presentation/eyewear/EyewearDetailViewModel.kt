package com.eyecare.app.presentation.eyewear

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.EyewearDetail
import com.eyecare.app.domain.repository.EyewearRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EyewearDetailUiState {
    data object Loading : EyewearDetailUiState
    data class Success(val detail: EyewearDetail) : EyewearDetailUiState
    data class Error(val message: String) : EyewearDetailUiState
}

@HiltViewModel
class EyewearDetailViewModel @Inject constructor(
    private val repository: EyewearRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<EyewearDetailUiState>(EyewearDetailUiState.Loading)
    val uiState: StateFlow<EyewearDetailUiState> = _uiState.asStateFlow()

    private var currentKey: String = ""

    fun load(key: String) {
        currentKey = key
        _uiState.value = EyewearDetailUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getEyewearDetail(key).fold(
                onSuccess = { EyewearDetailUiState.Success(it) },
                onFailure = { EyewearDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    fun retry() {
        if (currentKey.isNotEmpty()) load(currentKey)
    }
}
