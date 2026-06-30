package com.eyecare.app.presentation.appointments.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.VisitReason
import com.eyecare.app.domain.repository.AppointmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

sealed interface BookingResult {
    data class Success(val appointment: Appointment) : BookingResult
    data class Error(val message: String) : BookingResult
}

data class BookingState(
    val step: Int = 1,
    val visitReasons: List<VisitReason> = emptyList(),
    val visitReasonsLoading: Boolean = true,
    val visitReasonsError: String? = null,
    val selectedReasonId: Int? = null,
    val selectedReasonName: String? = null,
    val selectedDate: String? = null,
    val selectedDateTime: String? = null,
    val isLoading: Boolean = false,
    val result: BookingResult? = null,
) {
    // For backward compat in Step3
    val selectedReason: String? get() = selectedReasonName
}

@HiltViewModel
class BookAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingState())
    val uiState: StateFlow<BookingState> = _uiState.asStateFlow()

    init {
        loadVisitReasons()
    }

    fun retryVisitReasons() = loadVisitReasons()

    private fun loadVisitReasons() {
        _uiState.update { it.copy(visitReasonsLoading = true, visitReasonsError = null) }
        viewModelScope.launch {
            repository.getVisitReasons().fold(
                onSuccess = { reasons ->
                    _uiState.update { it.copy(visitReasons = reasons, visitReasonsLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            visitReasonsLoading = false,
                            visitReasonsError = error.message ?: "Failed to load visit reasons",
                        )
                    }
                },
            )
        }
    }

    fun selectReason(id: Int, name: String) {
        _uiState.update { it.copy(step = 2, selectedReasonId = id, selectedReasonName = name) }
    }

    fun selectDate(date: String) {
        _uiState.update { it.copy(step = 3, selectedDate = date) }
    }

    fun selectTime(time: String) {
        val date = _uiState.value.selectedDate ?: return
        val dateTime = "${date}T${time}:00Z"
        _uiState.update { it.copy(step = 4, selectedDateTime = dateTime) }
    }

    fun goBack() {
        _uiState.update { it.copy(step = (it.step - 1).coerceAtLeast(1)) }
    }

    fun submit(contactNotes: String?) {
        val state = _uiState.value
        val reasonId = state.selectedReasonId ?: return
        val dateTime = state.selectedDateTime ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createAppointment(reasonId, dateTime, contactNotes?.takeIf { it.isNotBlank() })
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

    companion object {
        private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

        /** Generate 30-minute time slots from 9:00 to 17:00 */
        fun generateTimeSlots(): List<String> {
            val slots = mutableListOf<String>()
            var time = LocalTime.of(9, 0)
            val end = LocalTime.of(17, 0)
            while (time.isBefore(end)) {
                slots.add(time.format(TIME_FORMAT))
                time = time.plusMinutes(30)
            }
            return slots
        }
    }
}
