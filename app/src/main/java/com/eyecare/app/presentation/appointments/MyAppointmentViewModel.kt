package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.domain.repository.AppointmentRequestRepository
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

sealed interface MyAppointmentUiState {
    data object Loading : MyAppointmentUiState

    data class Content(
        val journey: CurrentAppointmentJourney,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
        val isMutating: Boolean = false,
        val mutationError: String? = null,
        val mutationSuccess: String? = null,
        val showRescheduleSheet: Boolean = false,
        val isRescheduling: Boolean = false,
        val rescheduleError: String? = null,
        val rescheduleWeekStart: String? = null,
        val rescheduleDayAvailability: Map<String, DayAvailability> = emptyMap(),
        val rescheduleAvailability: RescheduleAvailabilityState = RescheduleAvailabilityState.Idle,
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
    private var availabilityJob: Job? = null
    private var availabilityGeneration = 0L
    private var weekJob: Job? = null
    private var weekGeneration = 0L
    private var refreshGeneration = 0

    fun load() {
        loadJob?.cancel()
        cancelRescheduleJobs()
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
                        message = patientSafeError(error),
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
        if (current is MyAppointmentUiState.Content && current.isMutating) return
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
                            refreshError = patientSafeError(error),
                        )
                    }
                },
            )
        }
    }

    fun cancelRequest(requestId: Int, reasonDetails: String) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || current.isMutating || current.isRefreshing) return
        val pendingRequest = current.journey as? CurrentAppointmentJourney.PendingRequest
        if (pendingRequest?.request?.id != requestId || !pendingRequest.request.status.isCancellable) return

        val reason = reasonDetails.trim()
        if (reason.isBlank() || reason.length > PATIENT_CANCELLATION_REASON_MAX_LENGTH) {
            _uiState.value = current.copy(mutationError = CANCELLATION_REASON_REQUIRED_MESSAGE)
            return
        }

        if (isSameDayInClinic(pendingRequest.request.scheduledAt)) {
            _uiState.value = current.copy(mutationError = SAME_DAY_CANCELLATION_MESSAGE)
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
        if (current !is MyAppointmentUiState.Content || current.isMutating || current.isRefreshing) return
        val journey = current.journey
        if (journey !is CurrentAppointmentJourney.Appointment) return
        if (!journey.appointment.status.canCancel) return

        val reason = reasonDetails.trim()
        if (reason.isBlank() || reason.length > PATIENT_CANCELLATION_REASON_MAX_LENGTH) {
            _uiState.value = current.copy(mutationError = CANCELLATION_REASON_REQUIRED_MESSAGE)
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

    fun showRescheduleSheet() {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || current.isMutating) return
        val journey = current.journey as? CurrentAppointmentJourney.Appointment ?: return
        val policy = appointmentCurrentActionPolicy(
            appointment = journey.appointment,
            hasPendingReschedule = journey.pendingReschedule != null,
        )
        if (!policy.canRequestDifferentTime) return

        val earliestDate = earliestAppointmentRequestDate()
        val appointmentDate = parseClinicDateTime(journey.appointment.scheduledAt)?.toLocalDate()
            ?: earliestDate
        val selectedDate = maxOf(appointmentDate, earliestDate)
        val appointmentWeekStart = selectedDate.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
        )
        val currentWeekStart = earliestDate.with(
            TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY),
        )
        val weekStart = maxOf(appointmentWeekStart, currentWeekStart).toString()

        _uiState.value = current.copy(
            showRescheduleSheet = true,
            rescheduleError = null,
            rescheduleWeekStart = weekStart,
            rescheduleDayAvailability = emptyMap(),
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
        )
        loadRescheduleWeekAvailability(weekStart)
        loadRescheduleAvailability(selectedDate.toString())
    }

    fun dismissRescheduleSheet() {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content) return

        cancelRescheduleJobs()
        _uiState.value = current.copy(
            showRescheduleSheet = false,
            isRescheduling = false,
            rescheduleError = null,
            rescheduleAvailability = RescheduleAvailabilityState.Idle,
        )
    }

    fun loadRescheduleWeekAvailability(weekStart: String) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || !current.showRescheduleSheet) return
        val journey = current.journey as? CurrentAppointmentJourney.Appointment ?: return

        weekJob?.cancel()
        val generation = ++weekGeneration
        val earliestDate = earliestAppointmentRequestDate()
        val dates = rescheduleWeekDates(weekStart).filter { !it.isBefore(earliestDate) }

        _uiState.value = current.copy(
            rescheduleWeekStart = weekStart,
            rescheduleDayAvailability = current.rescheduleDayAvailability +
                dates.associate { it.toString() to DayAvailability.LOADING },
        )

        weekJob = viewModelScope.launch {
            val results = dates.map { date ->
                async {
                    date.toString() to appointmentRepository.getAppointmentAvailability(
                        date = date.toString(),
                        appointmentId = journey.appointment.id,
                    )
                }
            }.awaitAll()

            if (weekGeneration != generation) return@launch
            val latest = _uiState.value as? MyAppointmentUiState.Content ?: return@launch
            val resolved = results.associate { (date, result) ->
                date to result.fold(
                    onSuccess = ::appointmentDayAvailabilityVerdict,
                    onFailure = { DayAvailability.UNKNOWN },
                )
            }
            _uiState.value = latest.copy(
                rescheduleDayAvailability = latest.rescheduleDayAvailability + resolved,
            )
        }
    }

    fun loadRescheduleAvailability(date: String) {
        loadRescheduleAvailability(date, clearError = true)
    }

    fun retryRescheduleAvailability() {
        val current = _uiState.value as? MyAppointmentUiState.Content ?: return
        val failedDate = (current.rescheduleAvailability as? RescheduleAvailabilityState.Error)?.date
            ?: return
        loadRescheduleAvailability(failedDate)
    }

    fun rescheduleAppointment(scheduledAt: String) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || current.isMutating) return
        val journey = current.journey as? CurrentAppointmentJourney.Appointment ?: return
        val policy = appointmentCurrentActionPolicy(
            appointment = journey.appointment,
            hasPendingReschedule = journey.pendingReschedule != null,
        )
        if (!policy.canRequestDifferentTime) return

        val selectedDate = parseClinicDateTime(scheduledAt)?.toLocalDate() ?: return
        if (selectedDate.isBefore(earliestAppointmentRequestDate())) return

        _uiState.value = current.copy(
            isMutating = true,
            isRescheduling = true,
            rescheduleError = null,
            mutationError = null,
            mutationSuccess = null,
        )
        viewModelScope.launch {
            appointmentRequestRepository.createRebookingRequest(
                appointmentId = journey.appointment.id,
                scheduledAt = scheduledAt,
            ).fold(
                onSuccess = {
                    refetchAfterMutation("Time-change request sent.")
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content) {
                        _uiState.value = latest.copy(
                            isMutating = false,
                            isRescheduling = false,
                            rescheduleError = if (isAppointmentSlotUnavailableError(error)) {
                                "That time became unavailable. We refreshed the list; choose another slot."
                            } else {
                                patientSafeRescheduleError(error)
                            },
                        )
                        if (isAppointmentSlotUnavailableError(error)) {
                            loadRescheduleAvailability(
                                date = selectedDate.toString(),
                                clearError = false,
                            )
                        }
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

    private fun rescheduleWeekDates(weekStart: String): List<LocalDate> {
        val start = runCatching { LocalDate.parse(weekStart) }
            .getOrElse { earliestAppointmentRequestDate() }
        return (0 until availabilityWeekLength).map { start.plusDays(it.toLong()) }
    }

    private fun loadRescheduleAvailability(date: String, clearError: Boolean) {
        val current = _uiState.value
        if (current !is MyAppointmentUiState.Content || !current.showRescheduleSheet) return
        val journey = current.journey as? CurrentAppointmentJourney.Appointment ?: return
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        if (selectedDate.isBefore(earliestAppointmentRequestDate())) return

        availabilityJob?.cancel()
        val generation = ++availabilityGeneration
        _uiState.value = current.copy(
            rescheduleAvailability = RescheduleAvailabilityState.Loading(date),
            rescheduleError = if (clearError) null else current.rescheduleError,
        )
        availabilityJob = viewModelScope.launch {
            appointmentRepository.getAppointmentAvailability(
                date = date,
                appointmentId = journey.appointment.id,
            ).fold(
                onSuccess = { availability ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content &&
                        generation == availabilityGeneration
                    ) {
                        _uiState.value = latest.copy(
                            rescheduleAvailability = RescheduleAvailabilityState.Success(availability),
                            rescheduleError = if (clearError) null else latest.rescheduleError,
                            rescheduleDayAvailability = latest.rescheduleDayAvailability +
                                (date to appointmentDayAvailabilityVerdict(availability)),
                        )
                    }
                },
                onFailure = { _ ->
                    val latest = _uiState.value
                    if (latest is MyAppointmentUiState.Content &&
                        generation == availabilityGeneration
                    ) {
                        _uiState.value = latest.copy(
                            rescheduleAvailability = RescheduleAvailabilityState.Error(
                                date = date,
                                message = "We couldn't load available times. Try again.",
                            ),
                            rescheduleError = if (clearError) null else latest.rescheduleError,
                        )
                    }
                },
            )
        }
    }

    private fun cancelRescheduleJobs() {
        availabilityJob?.cancel()
        availabilityJob = null
        availabilityGeneration++
        weekJob?.cancel()
        weekJob = null
        weekGeneration++
    }

    private suspend fun refetchAfterMutation(successMessage: String) {
        cancelRescheduleJobs()
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
                val latest = _uiState.value
                if (latest is MyAppointmentUiState.Content) {
                    _uiState.value = latest.copy(
                        isMutating = false,
                        isRescheduling = false,
                        showRescheduleSheet = false,
                        rescheduleError = null,
                        rescheduleAvailability = RescheduleAvailabilityState.Idle,
                        mutationError = "Action completed but unable to refresh. Pull to retry.",
                    )
                }
            },
        )
    }
}

private fun patientSafeError(error: Throwable): String {
    val message = error.message ?: return "Something went wrong. Please try again."
    return when {
        isSameDayCancellationError(error) || message.contains("same-day", ignoreCase = true) ->
            SAME_DAY_CANCELLATION_MESSAGE
        isCancellationReasonValidationError(error) -> CANCELLATION_REASON_REQUIRED_MESSAGE
        message.contains("not allowed", ignoreCase = true) -> "This action is not allowed. Please contact the clinic."
        else -> "Something went wrong. Please try again."
    }
}
