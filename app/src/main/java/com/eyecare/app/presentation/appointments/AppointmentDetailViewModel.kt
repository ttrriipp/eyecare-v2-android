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
    data class Success(val appointment: Appointment, val hasFeedback: Boolean = false) : AppointmentDetailUiState
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
