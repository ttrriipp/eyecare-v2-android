package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RescheduleAvailabilityState {
    data object Idle : RescheduleAvailabilityState
    data class Loading(val date: String) : RescheduleAvailabilityState
    data class Success(val availability: AppointmentAvailability) : RescheduleAvailabilityState
    data class Error(
        val date: String,
        val message: String,
    ) : RescheduleAvailabilityState
}

sealed interface AppointmentDetailUiState {
    data object Loading : AppointmentDetailUiState

    data class Success(
        val appointment: AppointmentV1,
        val frameReservations: List<FrameReservation> = emptyList(),
        val isCancelling: Boolean = false,
        val cancelError: String? = null,
        val showRescheduleSheet: Boolean = false,
        val isRescheduling: Boolean = false,
        val rescheduleError: String? = null,
        val rescheduleAvailability: RescheduleAvailabilityState = RescheduleAvailabilityState.Idle,
        val showRescheduleSuccessDialog: Boolean = false,
        val showRatingDialog: Boolean = false,
        val isSubmittingRating: Boolean = false,
        val ratingError: String? = null,
        val actionMessage: String? = null,
    ) : AppointmentDetailUiState

    data class Error(val message: String) : AppointmentDetailUiState
}

@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val repository: AppointmentV1Repository,
    private val reservationRepository: FrameReservationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: Int = savedStateHandle["appointmentId"] ?: -1
    private var availabilityJob: Job? = null
    private var availabilityGeneration = 0L

    private val _uiState = MutableStateFlow<AppointmentDetailUiState>(
        if (savedStateHandle.get<Int>("appointmentId") != null) AppointmentDetailUiState.Loading
        else AppointmentDetailUiState.Error("Missing appointment ID"),
    )
    val uiState: StateFlow<AppointmentDetailUiState> = _uiState.asStateFlow()

    init {
        if (appointmentId != -1) load()
    }

    fun refresh() = load()

    fun cancelAppointment() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        _uiState.value = current.copy(
            isCancelling = true,
            cancelError = null,
            actionMessage = null,
        )
        viewModelScope.launch {
            repository.cancelAppointment(appointmentId).fold(
                onSuccess = { cancelled ->
                    _uiState.value = current.copy(
                        appointment = cancelled,
                        isCancelling = false,
                        cancelError = null,
                        actionMessage = "Appointment cancelled. You can find it in History.",
                    )
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        isCancelling = false,
                        cancelError = patientSafeAppointmentError(AppointmentAction.CANCEL, error),
                        actionMessage = null,
                    )
                },
            )
        }
    }

    fun showRescheduleSheet() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        val date = parseClinicDateTime(current.appointment.scheduledAt)
            ?.toLocalDate()
            ?.toString()
            ?: current.appointment.scheduledAt.take(10)

        _uiState.value = current.copy(
            showRescheduleSheet = true,
            rescheduleError = null,
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
            actionMessage = null,
        )
        loadRescheduleAvailability(date)
    }

    fun dismissRescheduleSheet() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        availabilityJob?.cancel()
        availabilityGeneration++
        _uiState.value = current.copy(
            showRescheduleSheet = false,
            rescheduleError = null,
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
        )
    }

    fun loadRescheduleAvailability(date: String) {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        availabilityJob?.cancel()
        val generation = ++availabilityGeneration
        _uiState.value = current.copy(
            rescheduleAvailability = RescheduleAvailabilityState.Loading(date),
            rescheduleError = null,
        )
        availabilityJob = viewModelScope.launch {
            repository.getAppointmentAvailability(date, appointmentId).fold(
                onSuccess = { availability ->
                    val latest = _uiState.value
                    if (latest is AppointmentDetailUiState.Success &&
                        generation == availabilityGeneration
                    ) {
                        _uiState.value = latest.copy(
                            rescheduleAvailability = RescheduleAvailabilityState.Success(availability),
                            rescheduleError = null,
                        )
                    }
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is AppointmentDetailUiState.Success &&
                        generation == availabilityGeneration
                    ) {
                        _uiState.value = latest.copy(
                            rescheduleAvailability = RescheduleAvailabilityState.Error(
                                date = date,
                                message = patientSafeAppointmentError(
                                    AppointmentAction.AVAILABILITY,
                                    error,
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    fun clearRescheduleError() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success || current.rescheduleError == null) return
        _uiState.value = current.copy(rescheduleError = null)
    }

    fun rescheduleAppointment(scheduledAt: String) {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        _uiState.value = current.copy(
            isRescheduling = true,
            rescheduleError = null,
            actionMessage = null,
        )
        viewModelScope.launch {
            repository.rescheduleAppointment(appointmentId, scheduledAt).fold(
                onSuccess = { updatedAppointment ->
                    _uiState.value = current.copy(
                        appointment = updatedAppointment,
                        isRescheduling = false,
                        showRescheduleSheet = false,
                        rescheduleAvailability = RescheduleAvailabilityState.Idle,
                        showRescheduleSuccessDialog = true,
                    )
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        isRescheduling = false,
                        rescheduleError = patientSafeAppointmentError(
                            AppointmentAction.RESCHEDULE,
                            error,
                        ),
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

    fun showRatingDialog() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRatingDialog = true, ratingError = null)
    }

    fun dismissRatingDialog() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRatingDialog = false, ratingError = null)
    }

    fun submitRating(rating: Int, comment: String?) {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        if (rating !in 1..5) {
            _uiState.value = current.copy(ratingError = "Choose a rating from 1 to 5 stars.")
            return
        }
        if (comment != null && comment.length > 1000) {
            _uiState.value = current.copy(ratingError = "Keep your comment to 1,000 characters or less.")
            return
        }

        _uiState.value = current.copy(
            isSubmittingRating = true,
            ratingError = null,
            actionMessage = null,
        )
        viewModelScope.launch {
            repository.rateAppointment(appointmentId, rating, comment).fold(
                onSuccess = { visitRating ->
                    val updatedAppointment = current.appointment.copy(
                        isRateable = true,
                        visitRating = visitRating,
                    )
                    _uiState.value = current.copy(
                        appointment = updatedAppointment,
                        isSubmittingRating = false,
                        showRatingDialog = false,
                        actionMessage = "Thanks for sharing your feedback.",
                    )
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        isSubmittingRating = false,
                        ratingError = patientSafeAppointmentError(AppointmentAction.RATE, error),
                    )
                },
            )
        }
    }

    fun dismissActionMessage() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success || current.actionMessage == null) return
        _uiState.value = current.copy(actionMessage = null)
    }

    private fun load() {
        availabilityJob?.cancel()
        availabilityGeneration++
        viewModelScope.launch {
            val appointmentResult = repository.getAppointment(appointmentId)
            appointmentResult.fold(
                onSuccess = { appointment ->
                    val reservations = reservationRepository.getReservations()
                        .getOrElse { emptyList() }
                        .filter { it.appointment.id == appointmentId }
                    _uiState.value = AppointmentDetailUiState.Success(
                        appointment = appointment,
                        frameReservations = reservations,
                    )
                },
                onFailure = { error ->
                    _uiState.value = AppointmentDetailUiState.Error(
                        patientSafeAppointmentError(AppointmentAction.LOAD, error),
                    )
                },
            )
        }
    }
}

private enum class AppointmentAction {
    LOAD,
    AVAILABILITY,
    CANCEL,
    RESCHEDULE,
    RATE,
}

private fun patientSafeAppointmentError(
    action: AppointmentAction,
    error: Throwable,
): String = when (action) {
    AppointmentAction.LOAD ->
        "We couldn't load this appointment. Check your connection and try again."
    AppointmentAction.AVAILABILITY ->
        "We couldn't load available times. Try again."
    AppointmentAction.CANCEL ->
        "We couldn't cancel this appointment. Check your connection and try again."
    AppointmentAction.RESCHEDULE ->
        if (error is AppointmentError.ValidationError) {
            "That time is no longer available. Choose another time."
        } else {
            "We couldn't reschedule this appointment. Try again."
        }
    AppointmentAction.RATE -> when (error) {
        is AppointmentError.NotFound -> "This appointment is no longer available."
        is AppointmentError.ValidationError -> "This visit can't be rated yet."
        else -> "We couldn't submit your rating. Try again."
    }
}