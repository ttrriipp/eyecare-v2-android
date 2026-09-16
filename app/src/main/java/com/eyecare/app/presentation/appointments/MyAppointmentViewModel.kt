package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyAppointmentUiState {
    data object Loading : MyAppointmentUiState

    data class Content(
        val journey: CurrentAppointmentJourney,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
    ) : MyAppointmentUiState

    data class Error(val message: String) : MyAppointmentUiState
}

@HiltViewModel
class MyAppointmentViewModel @Inject constructor(
    private val appointmentRequestRepository: AppointmentRequestRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<MyAppointmentUiState>(MyAppointmentUiState.Loading)
    val uiState: StateFlow<MyAppointmentUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null
    private var refreshGeneration = 0

    fun load() {
        loadJob?.cancel()
        refreshGeneration++
        val generation = refreshGeneration
        _uiState.value = MyAppointmentUiState.Loading
        loadJob = viewModelScope.launch {
            val result = appointmentRequestRepository.getCurrentAppointmentJourney()
            if (generation != refreshGeneration) return@launch
            result.fold(
                onSuccess = { journey ->
                    _uiState.value = MyAppointmentUiState.Content(journey = journey)
                },
                onFailure = { error ->
                    _uiState.value = MyAppointmentUiState.Error(
                        message = error.message ?: "Unable to load your appointment. Please try again.",
                    )
                },
            )
        }
    }

    fun retry() {
        load()
    }

    fun refresh() {
        val current = _uiState.value
        if (current is MyAppointmentUiState.Loading) {
            load()
            return
        }
        refreshGeneration++
        val generation = refreshGeneration
        if (current is MyAppointmentUiState.Content) {
            _uiState.value = current.copy(isRefreshing = true, refreshError = null)
        }
        viewModelScope.launch {
            val result = appointmentRequestRepository.getCurrentAppointmentJourney()
            if (generation != refreshGeneration) return@launch
            result.fold(
                onSuccess = { journey ->
                    _uiState.value = MyAppointmentUiState.Content(journey = journey)
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content) {
                        _uiState.value = latest.copy(
                            isRefreshing = false,
                            refreshError = error.message ?: "Refresh failed. Pull to retry.",
                        )
                    }
                },
            )
        }
    }

    fun clearRefreshError() {
        val current = _uiState.value
        if (current is MyAppointmentUiState.Content) {
            _uiState.value = current.copy(refreshError = null)
        }
    }
}
