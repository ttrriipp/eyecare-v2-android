package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.AppointmentV1Repository
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
        val isMutating: Boolean = false,
        val mutationError: String? = null,
        val mutationSuccess: String? = null,
    ) : MyAppointmentUiState

    data class Error(val message: String) : MyAppointmentUiState
}

@HiltViewModel
class MyAppointmentViewModel @Inject constructor(
    private val appointmentRequestRepository: AppointmentRequestRepository,
    private val appointmentRepository: AppointmentV1Repository,
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

    fun cancelRequest(requestId: Int, reasonDetails: String) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || current.isMutating) return

        val reason = reasonDetails.trim()
        if (reason.isBlank()) {
            _uiState.value = current.copy(mutationError = "Enter a reason for cancelling.")
            return
        }

        _uiState.value = current.copy(isMutating = true, mutationError = null, mutationSuccess = null)
        viewModelScope.launch {
            appointmentRequestRepository.cancelRequest(requestId, reason).fold(
                onSuccess = {
                    refetchAfterMutation("Request cancelled.")
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content) {
                        _uiState.value = latest.copy(
                            isMutating = false,
                            mutationError = patientSafeError(error),
                        )
                    }
                },
            )
        }
    }

    fun cancelAppointment(reasonDetails: String) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || current.isMutating) return
        val journey = current.journey
        if (journey !is CurrentAppointmentJourney.Appointment) return

        val reason = reasonDetails.trim()
        if (reason.isBlank()) {
            _uiState.value = current.copy(mutationError = "Enter a reason for cancelling.")
            return
        }

        if (isSameDayInClinic(journey.appointment.scheduledAt)) {
            _uiState.value = current.copy(
                mutationError = SAME_DAY_CANCELLATION_MESSAGE,
            )
            return
        }

        _uiState.value = current.copy(isMutating = true, mutationError = null, mutationSuccess = null)
        viewModelScope.launch {
            appointmentRepository.cancelAppointment(journey.appointment.id, reason).fold(
                onSuccess = {
                    refetchAfterMutation("Appointment cancelled.")
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content) {
                        _uiState.value = latest.copy(
                            isMutating = false,
                            mutationError = patientSafeError(error),
                        )
                    }
                },
            )
        }
    }

    fun clearMutationError() {
        val current = _uiState.value
        if (current is MyAppointmentUiState.Content) {
            _uiState.value = current.copy(mutationError = null)
        }
    }

    fun clearMutationSuccess() {
        val current = _uiState.value
        if (current is MyAppointmentUiState.Content) {
            _uiState.value = current.copy(mutationSuccess = null)
        }
    }

    fun clearRefreshError() {
        val current = _uiState.value
        if (current is MyAppointmentUiState.Content) {
            _uiState.value = current.copy(refreshError = null)
        }
    }

    private suspend fun refetchAfterMutation(successMessage: String) {
        refreshGeneration++
        val generation = refreshGeneration
        val result = appointmentRequestRepository.getCurrentAppointmentJourney()
        if (generation != refreshGeneration) return
        result.fold(
            onSuccess = { journey ->
                _uiState.value = MyAppointmentUiState.Content(
                    journey = journey,
                    mutationSuccess = successMessage,
                )
            },
            onFailure = {
                _uiState.value = MyAppointmentUiState.Content(
                    journey = CurrentAppointmentJourney.None,
                    mutationError = "Action completed but unable to refresh. Pull to retry.",
                )
            },
        )
    }
}

private fun patientSafeError(error: Throwable): String {
    val message = error.message ?: return "Something went wrong. Please try again."
    return when {
        message.contains("same-day", ignoreCase = true) -> SAME_DAY_CANCELLATION_MESSAGE
        message.contains("not allowed", ignoreCase = true) -> "This action is not allowed. Please contact the clinic."
        else -> "Something went wrong. Please try again."
    }
}
