package com.eyecare.app.presentation.appointments.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BookingResult {
    data class Success(val appointment: Appointment) : BookingResult
    data class Error(val message: String) : BookingResult
}

data class BookingState(
    val step: Int = 1,
    val selectedReason: String? = null,
    val selectedDateTime: String? = null,
    val isLoading: Boolean = false,
    val result: BookingResult? = null,
)

@HiltViewModel
class BookAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingState())
    val uiState: StateFlow<BookingState> = _uiState.asStateFlow()

    fun selectReason(reason: String) {
        _uiState.update { it.copy(step = 2, selectedReason = reason) }
    }

    fun selectDateTime(dateTime: String) {
        _uiState.update { it.copy(step = 3, selectedDateTime = dateTime) }
    }

    fun goBack() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
    }

    fun submit(contactNotes: String?) {
        val state = _uiState.value
        val reason = state.selectedReason ?: return
        val dateTime = state.selectedDateTime ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createAppointment(reason, dateTime, contactNotes?.takeIf { it.isNotBlank() })
            _uiState.update {
                it.copy(
                    isLoading = false,
                    result = result.fold(
                        onSuccess = { appt -> BookingResult.Success(appt) },
                        onFailure = { err -> BookingResult.Error(err.message ?: "Booking failed") },
                    ),
                )
            }
        }
    }
}
