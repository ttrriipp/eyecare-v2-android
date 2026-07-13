package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.eyecare.app.domain.model.Appointment
import com.eyecare.app.domain.repository.AppointmentRepository
import com.eyecare.app.domain.repository.FeedbackRepository
import com.eyecare.app.presentation.navigation.AppointmentDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentDetailUiState {
    data object Loading : AppointmentDetailUiState
    data class Success(
        val appointment: Appointment,
        val hasFeedback: Boolean = false,
        val isCancelling: Boolean = false,
        val cancelError: String? = null,
        val showRescheduleSheet: Boolean = false,
        val isRescheduling: Boolean = false,
        val rescheduleError: String? = null,
        val showRescheduleSuccessDialog: Boolean = false,
    ) : AppointmentDetailUiState
    data class Error(val message: String) : AppointmentDetailUiState
}

@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val repository: AppointmentRepository,
    private val feedbackRepository: FeedbackRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: Int = savedStateHandle.toRoute<AppointmentDetail>().appointmentId

    private val _uiState = MutableStateFlow<AppointmentDetailUiState>(AppointmentDetailUiState.Loading)
    val uiState: StateFlow<AppointmentDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun refresh() = load()

    fun cancelAppointment() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(isCancelling = true, cancelError = null)
        viewModelScope.launch {
            repository.cancelAppointment(appointmentId).fold(
                onSuccess = { load() },
                onFailure = {
                    _uiState.value = current.copy(
                        isCancelling = false,
                        cancelError = it.message ?: "Failed to cancel appointment",
                    )
                },
            )
        }
    }

    fun showRescheduleSheet() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRescheduleSheet = true, rescheduleError = null)
    }

    fun dismissRescheduleSheet() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRescheduleSheet = false, rescheduleError = null)
    }

    fun clearRescheduleError() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success || current.rescheduleError == null) return
        _uiState.value = current.copy(rescheduleError = null)
    }

    fun rescheduleAppointment(scheduledAt: String) {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(isRescheduling = true, rescheduleError = null)
        viewModelScope.launch {
            repository.rescheduleAppointment(appointmentId, scheduledAt).fold(
                onSuccess = { updatedAppointment ->
                    // Use the appointment returned by the reschedule call directly, rather than
                    // re-fetching, so the screen reflects exactly what the server just confirmed.
                    _uiState.value = current.copy(
                        appointment = updatedAppointment,
                        isRescheduling = false,
                        showRescheduleSheet = false,
                        showRescheduleSuccessDialog = true,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isRescheduling = false,
                        rescheduleError = it.message ?: "Failed to reschedule appointment",
                    )
                },
            )
        }
    }

    fun dismissRescheduleSuccessDialog() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRescheduleSuccessDialog = false)
    }

    private fun load() {
        viewModelScope.launch {
            val appointmentResult = repository.getAppointment(appointmentId)
            val feedbackResult = feedbackRepository.getFeedbackHistory()
            _uiState.value = appointmentResult.fold(
                onSuccess = { appointment ->
                    val hasFeedback = feedbackResult.getOrDefault(emptyList())
                        .any { it.appointmentId == appointmentId }
                    AppointmentDetailUiState.Success(appointment, hasFeedback)
                },
                onFailure = { AppointmentDetailUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }
}
