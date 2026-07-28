package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentDetailUiState {
    data object Loading : AppointmentDetailUiState
    data class Success(
        val appointment: AppointmentV1,
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
    private val repository: AppointmentV1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: Int = checkNotNull(savedStateHandle["appointmentId"])

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
                onSuccess = { cancelled ->
                    _uiState.value = current.copy(
                        appointment = cancelled,
                        isCancelling = false,
                    )
                },
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
            repository.getAppointment(appointmentId).fold(
                onSuccess = { appointment ->
                    _uiState.value = AppointmentDetailUiState.Success(appointment)
                },
                onFailure = {
                    _uiState.value = AppointmentDetailUiState.Error(it.message ?: "Failed to load")
                },
            )
        }
    }
}
