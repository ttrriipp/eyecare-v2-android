package com.eyecare.app.presentation.appointments.booking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BookingResult {
    data class Success(val appointment: AppointmentV1) : BookingResult
    data class Error(val message: String) : BookingResult
}

data class BookingState(
    val step: Int = 1,
    val appointmentTypes: List<AppointmentType> = emptyList(),
    val appointmentTypesLoading: Boolean = true,
    val appointmentTypesError: String? = null,
    val selectedTypeId: Int? = null,
    val selectedTypeName: String? = null,
    val selectedTypeRequiresReferral: Boolean = false,
    val selectedDate: String? = null,
    val selectedDateTime: String? = null,
    val referringSource: String? = null,
    val availability: AppointmentAvailability? = null,
    val availabilityLoading: Boolean = false,
    val availabilityError: String? = null,
    val availabilityNotice: String? = null,
    val isLoading: Boolean = false,
    val result: BookingResult? = null,
)

@HiltViewModel
class BookAppointmentViewModel @Inject constructor(
    private val repository: AppointmentV1Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BookingState())
    val uiState: StateFlow<BookingState> = _uiState.asStateFlow()

    init {
        loadAppointmentTypes()
    }

    fun retryAppointmentTypes() = loadAppointmentTypes()

    private fun loadAppointmentTypes() {
        _uiState.update { it.copy(appointmentTypesLoading = true, appointmentTypesError = null) }
        viewModelScope.launch {
            repository.getAppointmentTypes().fold(
                onSuccess = { types ->
                    _uiState.update { it.copy(appointmentTypes = types, appointmentTypesLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            appointmentTypesLoading = false,
                            appointmentTypesError = error.message ?: "Failed to load appointment types",
                        )
                    }
                },
            )
        }
    }

    fun selectType(type: AppointmentType) {
        _uiState.update {
            it.copy(
                step = 2,
                selectedTypeId = type.id,
                selectedTypeName = type.name,
                selectedTypeRequiresReferral = type.requiresReferral,
                selectedDate = null,
                selectedDateTime = null,
                referringSource = null,
                availability = null,
                availabilityError = null,
                availabilityNotice = null,
            )
        }
    }

    fun updateReferringSource(value: String) {
        _uiState.update { it.copy(referringSource = value) }
    }

    fun selectDate(date: String) {
        val typeId = _uiState.value.selectedTypeId ?: return
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
        fetchAvailability(date, typeId)
    }

    fun retryAvailability() {
        val state = _uiState.value
        val date = state.selectedDate ?: return
        val typeId = state.selectedTypeId ?: return
        _uiState.update {
            it.copy(
                availability = null,
                availabilityLoading = true,
                availabilityError = null,
            )
        }
        fetchAvailability(date, typeId)
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
        val typeId = state.selectedTypeId ?: return
        val dateTime = state.selectedDateTime ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.createAppointment(
                appointmentTypeId = typeId,
                scheduledAt = dateTime,
                contactNotes = contactNotes?.takeIf { it.isNotBlank() },
                referringSource = state.referringSource?.takeIf { it.isNotBlank() },
            )
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
                        val selectedTypeId = current.selectedTypeId
                        _uiState.update {
                            it.copy(
                                step = 3,
                                selectedDateTime = null,
                                availability = null,
                                availabilityLoading = date != null && selectedTypeId != null,
                                availabilityError = null,
                                availabilityNotice = STALE_SLOT_MESSAGE,
                                isLoading = false,
                                result = null,
                            )
                        }
                        if (date != null && selectedTypeId != null) {
                            fetchAvailability(date, selectedTypeId)
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

    private fun fetchAvailability(date: String, typeId: Int) {
        viewModelScope.launch {
            repository.getAppointmentAvailability(date, typeId).fold(
                onSuccess = { availability ->
                    _uiState.update { state ->
                        if (state.selectedDate != date || state.selectedTypeId != typeId) state
                        else state.copy(
                            availability = availability,
                            availabilityLoading = false,
                            availabilityError = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        if (state.selectedDate != date || state.selectedTypeId != typeId) state
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
