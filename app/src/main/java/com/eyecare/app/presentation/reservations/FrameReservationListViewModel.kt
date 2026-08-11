package com.eyecare.app.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReservationListUiState {
    data object Loading : ReservationListUiState
    data class Success(val reservations: List<FrameReservation>) : ReservationListUiState
    data class Error(val message: String) : ReservationListUiState
}

@HiltViewModel
class FrameReservationListViewModel @Inject constructor(
    private val repository: FrameReservationRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReservationListUiState>(ReservationListUiState.Loading)
    val uiState: StateFlow<ReservationListUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    private fun load() {
        _uiState.value = ReservationListUiState.Loading
        viewModelScope.launch {
            repository.getReservations().fold(
                onSuccess = { _uiState.value = ReservationListUiState.Success(it) },
                onFailure = { _uiState.value = ReservationListUiState.Error(it.message ?: "Failed to load reservations") },
            )
        }
    }
}
