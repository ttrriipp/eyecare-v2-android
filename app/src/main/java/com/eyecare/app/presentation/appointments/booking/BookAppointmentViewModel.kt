package com.eyecare.app.presentation.appointments.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.VisitReason
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
    val visitReasons: List<VisitReason> = emptyList(),
    val visitReasonsLoading: Boolean = true,
    val visitReasonsError: String? = null,
    val selectedReasonId: Int? = null,
    val selectedReasonName: String? = null,
    val selectedDate: String? = null,
    val selectedDateTime: String? = null,
    val availability: AppointmentAvailability? = null,
    val availabilityLoading: Boolean = false,
    val availabilityError: String? = null,
    val availabilityNotice: String? = null,
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
        _uiState.update {
            it.copy(
                step = 2,
                selectedReasonId = id,
                selectedReasonName = name,
                selectedDate = null,
                selectedDateTime = null,
                availability = null,
                availabilityError = null,
                availabilityNotice = null,
            )
        }
    }

    fun selectDate(date: String) {
        val reasonId = _uiState.value.selectedReasonId ?: return
        _uiState.update {
            it.copy(
                step = 3,
                selectedDate = date,
                selectedDateTime = null,
                availability = null,
                availabilityLoading = true,
                availabilityError = null,
                availabilityNotice = null,
            )
        }
        fetchAvailability(date, reasonId)
    }

    fun retryAvailability() {
        val state = _uiState.value
        val date = state.selectedDate ?: return
        val reasonId = state.selectedReasonId ?: return
        _uiState.update {
            it.copy(
                availability = null,
                availabilityLoading = true,
                availabilityError = null,
            )
        }
        fetchAvailability(date, reasonId)
    }

    fun selectTime(startsAt: String) {
        val isAvailable = _uiState.value.availability?.slots
            ?.any { it.startsAt == startsAt && it.available } == true
        if (!isAvailable) return
        _uiState.update {
            it.copy(step = 4, selectedDateTime = startsAt, availabilityNotice = null)
        }
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
            result.fold(
                onSuccess = { appointment ->
                    _uiState.update {
                        it.copy(isLoading = false, result = BookingResult.Success(appointment))
                    }
                },
                onFailure = { error ->
                    if (error is AppointmentError.ValidationError && error.code == SLOT_UNAVAILABLE) {
                        val current = _uiState.value
                        val date = current.selectedDate
                        val selectedReasonId = current.selectedReasonId
                        _uiState.update {
                            it.copy(
                                step = 3,
                                selectedDateTime = null,
                                availability = null,
                                availabilityLoading = date != null && selectedReasonId != null,
                                availabilityError = null,
                                availabilityNotice = STALE_SLOT_MESSAGE,
                                isLoading = false,
                                result = null,
                            )
                        }
                        if (date != null && selectedReasonId != null) {
                            fetchAvailability(date, selectedReasonId)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                result = BookingResult.Error(error.message ?: "Booking failed"),
                            )
                        }
                    }
                },
            )
        }
    }

    private fun fetchAvailability(date: String, reasonId: Int) {
        viewModelScope.launch {
            repository.getAppointmentAvailability(date, reasonId).fold(
                onSuccess = { availability ->
                    _uiState.update { state ->
                        if (state.selectedDate != date || state.selectedReasonId != reasonId) state
                        else state.copy(
                            availability = availability,
                            availabilityLoading = false,
                            availabilityError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        if (state.selectedDate != date || state.selectedReasonId != reasonId) state
                        else state.copy(
                            availability = null,
                            availabilityLoading = false,
                            availabilityError = error.message ?: "Unable to load available times",
                        )
                    }
                },
            )
        }
    }

    companion object {
        private const val SLOT_UNAVAILABLE = "SLOT_UNAVAILABLE"
        const val STALE_SLOT_MESSAGE = "That time was just taken. Choose another available time."
    }
}
