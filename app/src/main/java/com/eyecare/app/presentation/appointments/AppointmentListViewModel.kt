package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentListUiState {
    data object Loading : AppointmentListUiState
    data class Success(val appointments: List<Appointment>) : AppointmentListUiState
    data object Empty : AppointmentListUiState
    data class Error(val message: String) : AppointmentListUiState
}

@HiltViewModel
class AppointmentListViewModel @Inject constructor(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentListUiState>(AppointmentListUiState.Loading)
    val uiState: StateFlow<AppointmentListUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() { load() }

    private fun load() {
        _uiState.value = AppointmentListUiState.Loading
        viewModelScope.launch {
            _uiState.value = repository.getAppointments().fold(
                onSuccess = { list ->
                    if (list.isEmpty()) AppointmentListUiState.Empty
                    else AppointmentListUiState.Success(
                        list.sortedByDescending {
                            runCatching { java.time.Instant.parse(it.scheduledAt) }
                                .getOrElse { java.time.Instant.EPOCH }
                        }
                    )
                },
                onFailure = { AppointmentListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
