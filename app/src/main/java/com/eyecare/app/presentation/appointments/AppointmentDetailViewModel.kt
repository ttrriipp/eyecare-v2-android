package com.eyecare.app.presentation.appointments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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
        val isCancelling: Boolean = false,
        val cancelError: String? = null,
        val showRescheduleSheet: Boolean = false,
        val isRescheduling: Boolean = false,
        val rescheduleError: String? = null,
        val rescheduleWeekStart: String? = null,
        val rescheduleDayAvailability: Map<String, DayAvailability> = emptyMap(),
        val rescheduleAvailability: RescheduleAvailabilityState = RescheduleAvailabilityState.Idle,
        val showRescheduleSuccessDialog: Boolean = false,
        val showRatingDialog: Boolean = false,
        val isSubmittingRating: Boolean = false,
        val ratingError: String? = null,
        val showRatingSuccessDialog: Boolean = false,
        val actionMessage: String? = null,
    ) : AppointmentDetailUiState

    data class Error(val message: String) : AppointmentDetailUiState
}

@HiltViewModel
class AppointmentDetailViewModel @Inject constructor(
    private val repository: AppointmentV1Repository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val appointmentId: Int = savedStateHandle["appointmentId"] ?: -1
    private var availabilityJob: Job? = null
    private var availabilityGeneration = 0L
    private var weekJob: Job? = null
    private var weekGeneration = 0L

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
        val today = LocalDate.now(CLINIC_TIME_ZONE)
        val appointmentWeekStart = runCatching { LocalDate.parse(date) }.getOrDefault(today)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStart = maxOf(appointmentWeekStart, currentWeekStart).toString()

        _uiState.value = current.copy(
            showRescheduleSheet = true,
            rescheduleError = null,
            rescheduleWeekStart = weekStart,
            rescheduleDayAvailability = emptyMap(),
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
            actionMessage = null,
        )
        loadRescheduleWeekAvailability(weekStart)
        loadRescheduleAvailability(date)
    }

    fun dismissRescheduleSheet() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        availabilityJob?.cancel()
        availabilityGeneration++
        weekJob?.cancel()
        weekGeneration++
        _uiState.value = current.copy(
            showRescheduleSheet = false,
            rescheduleError = null,
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
        )
    }

    /** The seven dates a reschedule week strip shows, starting at [weekStart]. */
    private fun rescheduleWeekDates(weekStart: String): List<LocalDate> {
        val start = runCatching { LocalDate.parse(weekStart) }
            .getOrElse { LocalDate.now(CLINIC_TIME_ZONE) }
        return (0 until availabilityWeekLength).map { start.plusDays(it.toLong()) }
    }

    /**
     * Fetches all seven visible days in parallel so the strip can mark each one open, closed, or
     * fully booked before the patient commits a tap. The reschedule availability endpoint answers
     * one date per call, which is why this fans out rather than requesting a range.
     */
    fun loadRescheduleWeekAvailability(weekStart: String) {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return

        weekJob?.cancel()
        val generation = ++weekGeneration
        val today = LocalDate.now(CLINIC_TIME_ZONE)
        val dates = rescheduleWeekDates(weekStart).filter { !it.isBefore(today) }

        _uiState.value = current.copy(
            rescheduleWeekStart = weekStart,
            rescheduleDayAvailability = current.rescheduleDayAvailability +
                dates.associate { it.toString() to DayAvailability.LOADING },
        )

        weekJob = viewModelScope.launch {
            val results = dates.map { date ->
                async {
                    date.toString() to repository.getAppointmentAvailability(date.toString(), appointmentId)
                }
            }.awaitAll()

            if (weekGeneration != generation) return@launch
            val latest = _uiState.value as? AppointmentDetailUiState.Success ?: return@launch

            val resolved = results.associate { (date, result) ->
                date to result.fold(
                    onSuccess = { availability -> dayAvailabilityVerdict(availability) },
                    onFailure = { DayAvailability.UNKNOWN },
                )
            }
            _uiState.value = latest.copy(
                rescheduleDayAvailability = latest.rescheduleDayAvailability + resolved,
            )
        }
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
                            rescheduleDayAvailability = latest.rescheduleDayAvailability +
                                (date to dayAvailabilityVerdict(availability)),
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
                    val latest = _uiState.value as? AppointmentDetailUiState.Success ?: current
                    _uiState.value = latest.copy(
                        appointment = updatedAppointment,
                        isSubmittingRating = false,
                        showRatingDialog = false,
                        showRatingSuccessDialog = true,
                    )
                },
                onFailure = { error ->
                    val latest = _uiState.value as? AppointmentDetailUiState.Success ?: current
                    _uiState.value = latest.copy(
                        isSubmittingRating = false,
                        ratingError = patientSafeAppointmentError(AppointmentAction.RATE, error),
                    )
                },
            )
        }
    }

    fun dismissRatingSuccessDialog() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success) return
        _uiState.value = current.copy(showRatingSuccessDialog = false)
    }

    fun dismissActionMessage() {
        val current = _uiState.value
        if (current !is AppointmentDetailUiState.Success || current.actionMessage == null) return
        _uiState.value = current.copy(actionMessage = null)
    }

    private fun load() {
        availabilityJob?.cancel()
        availabilityGeneration++
        weekJob?.cancel()
        weekGeneration++
        // A refresh re-fetches over an already-loaded screen (e.g. the status-guidance "Retry"
        // action) - preserve any open dialog/transient UI state instead of defaulting it away,
        // so refreshing doesn't silently close a dialog and discard what the patient typed.
        val previous = _uiState.value as? AppointmentDetailUiState.Success
        viewModelScope.launch {
            val appointmentResult = repository.getAppointment(appointmentId)
            appointmentResult.fold(
                onSuccess = { appointment ->
                    _uiState.value = previous?.copy(
                        appointment = appointment,
                    ) ?: AppointmentDetailUiState.Success(
                        appointment = appointment,
                    )
                },
                onFailure = { error ->
                    // Only the first load has no prior state to fall back on; a background
                    // refresh failure over an already-loaded screen keeps that screen visible
                    // rather than replacing it with a full-page error.
                    if (previous == null) {
                        _uiState.value = AppointmentDetailUiState.Error(
                            patientSafeAppointmentError(AppointmentAction.LOAD, error),
                        )
                    }
                },
            )
        }
    }
}

private fun dayAvailabilityVerdict(availability: AppointmentAvailability): DayAvailability = when {
    !availability.dayStatus.equals("open", ignoreCase = true) -> DayAvailability.CLOSED
    availability.slots.none { it.available } -> DayAvailability.FULL
    else -> DayAvailability.OPEN
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