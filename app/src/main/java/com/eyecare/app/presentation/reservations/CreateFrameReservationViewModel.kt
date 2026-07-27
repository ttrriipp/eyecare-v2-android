package com.eyecare.app.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreateReservationUiState {
    data object Idle : CreateReservationUiState
    data object Submitting : CreateReservationUiState
    data class Success(val reservation: FrameReservation) : CreateReservationUiState
    data class Error(val message: String) : CreateReservationUiState
}

@HiltViewModel(assistedFactory = CreateFrameReservationViewModel.Factory::class)
class CreateFrameReservationViewModel @AssistedInject constructor(
    private val repository: FrameReservationRepository,
    @Assisted("frameId") val frameId: Int,
    @Assisted("variantId") val variantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("frameId") frameId: Int,
            @Assisted("variantId") variantId: Int,
        ): CreateFrameReservationViewModel
    }

    private val _uiState = MutableStateFlow<CreateReservationUiState>(CreateReservationUiState.Idle)
    val uiState: StateFlow<CreateReservationUiState> = _uiState.asStateFlow()

    fun submit(appointmentId: Int? = null) {
        val current = _uiState.value
        if (current is CreateReservationUiState.Submitting) return
        _uiState.value = CreateReservationUiState.Submitting
        viewModelScope.launch {
            repository.createReservation(
                variantIds = listOf(variantId),
                appointmentId = appointmentId,
            ).fold(
                onSuccess = { _uiState.value = CreateReservationUiState.Success(it) },
                onFailure = { _uiState.value = CreateReservationUiState.Error(it.message ?: "Failed to create reservation") },
            )
        }
    }
}
