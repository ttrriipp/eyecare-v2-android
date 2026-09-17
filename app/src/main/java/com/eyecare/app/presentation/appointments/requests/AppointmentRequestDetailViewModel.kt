package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.presentation.appointments.DayAvailability
import com.eyecare.app.presentation.appointments.RescheduleAvailabilityState
import com.eyecare.app.presentation.appointments.CANCELLATION_REASON_REQUIRED_MESSAGE
import com.eyecare.app.presentation.appointments.PATIENT_CANCELLATION_REASON_MAX_LENGTH
import com.eyecare.app.presentation.appointments.SAME_DAY_CANCELLATION_MESSAGE
import com.eyecare.app.presentation.appointments.availabilityWeekLength
import com.eyecare.app.presentation.appointments.earliestAppointmentRequestDate
import com.eyecare.app.presentation.appointments.isSameDayCancellationError
import com.eyecare.app.presentation.appointments.isSameDayInClinic
import com.eyecare.app.presentation.appointments.parseClinicDateTime
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

sealed interface RequestDetailState {
    data object Loading : RequestDetailState
    data class Data(
        val request: AppointmentRequest,
        val isCancelling: Boolean = false,
        val cancelError: String? = null,
        val isLinked: Boolean = false,
        val isRefreshing: Boolean = false,
        val showScheduleSheet: Boolean = false,
        val isUpdatingSchedule: Boolean = false,
        val scheduleError: String? = null,
        val scheduleWeekStart: String? = null,
        val scheduleDayAvailability: Map<String, DayAvailability> = emptyMap(),
        val scheduleAvailability: RescheduleAvailabilityState = RescheduleAvailabilityState.Idle,
    ) : RequestDetailState
    data class Error(val message: String) : RequestDetailState
    data object NotFound : RequestDetailState
}

@HiltViewModel
class AppointmentRequestDetailViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
    private val appointmentRepository: AppointmentV1Repository,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestDetailState>(RequestDetailState.Loading)
    val state: StateFlow<RequestDetailState> = _state.asStateFlow()

    private var linkedContext = false
    private var lastRequestId: Int? = null
    private var scheduleAvailabilityJob: Job? = null
    private var scheduleAvailabilityGeneration = 0L
    private var scheduleWeekJob: Job? = null
    private var scheduleWeekGeneration = 0L

    fun load(id: Int) {
        lastRequestId = id
        viewModelScope.launch {
            _state.value = RequestDetailState.Loading
            repository.getRequest(id)
                .onSuccess { request ->
                    _state.value = RequestDetailState.Data(
                        request = request,
                        isLinked = linkedContext,
                    )
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    when (apiError?.code) {
                        "REQUEST_NOT_OWNED" -> _state.value = RequestDetailState.NotFound
                        else -> _state.value = RequestDetailState.Error(
                            patientSafeAppointmentRequestError(
                                error = error,
                                fallback = "We couldn't load this request. Please try again.",
                            ),
                        )
                    }
                }
        }
    }

    fun cancel(reasonDetails: String) {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.request.status.isCancellable) return

        val reason = reasonDetails.trim()
        if (reason.isBlank() || reason.length > PATIENT_CANCELLATION_REASON_MAX_LENGTH) {
            _state.value = current.copy(cancelError = CANCELLATION_REASON_REQUIRED_MESSAGE)
            return
        }

        if (isSameDayInClinic(current.request.scheduledAt)) {
            _state.value = current.copy(cancelError = SAME_DAY_CANCELLATION_MESSAGE)
            return
        }

        _state.value = current.copy(isCancelling = true, cancelError = null)
        viewModelScope.launch {
            repository.cancelRequest(current.request.id, reason)
                .onSuccess { request ->
                    _state.value = current.copy(
                        request = request,
                        isCancelling = false,
                        cancelError = null,
                    )
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    if (isSameDayCancellationError(error)) {
                        _state.value = current.copy(
                            isCancelling = false,
                            cancelError = SAME_DAY_CANCELLATION_MESSAGE,
                        )
                    } else if (apiError?.code == "REQUEST_NOT_CANCELLABLE" ||
                        apiError?.hasAppointmentRequestFieldError("request") == true
                    ) {
                        refresh()
                    } else {
                        _state.value = current.copy(
                            isCancelling = false,
                            cancelError = patientSafeAppointmentRequestError(
                                error = error,
                                fallback = "We couldn't cancel this request. Please try again.",
                            ),
                        )
                    }
                }
        }
    }

    /**
     * Re-checks the request's status in place, keeping the current data (and its scroll
     * position) on screen instead of dropping to the full-screen [RequestDetailState.Loading]
     * spinner `load` uses for the first fetch.
     */
    fun refresh() {
        val id = lastRequestId ?: return
        val current = _state.value
        if (current !is RequestDetailState.Data) {
            load(id)
            return
        }

        _state.value = current.copy(isRefreshing = true)
        viewModelScope.launch {
            repository.getRequest(id)
                .onSuccess { request ->
                    _state.value = current.copy(
                        request = request,
                        isRefreshing = false,
                        cancelError = null,
                        scheduleError = null,
                    )
                }
                .onFailure {
                    _state.value = current.copy(isRefreshing = false)
                }
        }
    }

    fun setLinked(linked: Boolean) {
        linkedContext = linked
        val current = _state.value
        if (current is RequestDetailState.Data) {
            _state.value = current.copy(isLinked = linked)
        }
    }

    fun retry() {
        lastRequestId?.let(::load)
    }

    fun showScheduleSheet() {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.request.status.isCancellable) return

        val earliestDate = earliestAppointmentRequestDate()
        val requestedDate = maxOf(
            parseClinicDateTime(current.request.scheduledAt)?.toLocalDate()
                ?: earliestDate,
            earliestDate,
        ).toString()
        val requestedWeekStart = runCatching { LocalDate.parse(requestedDate) }
            .getOrDefault(earliestDate)
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val currentWeekStart = earliestDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStart = maxOf(requestedWeekStart, currentWeekStart).toString()

        _state.value = current.copy(
            showScheduleSheet = true,
            isUpdatingSchedule = false,
            scheduleError = null,
            scheduleWeekStart = weekStart,
            scheduleDayAvailability = emptyMap(),
            scheduleAvailability = RescheduleAvailabilityState.Idle,
        )
        loadScheduleWeekAvailability(weekStart)
        loadScheduleAvailability(requestedDate)
    }

    fun dismissScheduleSheet() {
        val current = _state.value
        if (current !is RequestDetailState.Data) return

        scheduleAvailabilityJob?.cancel()
        scheduleAvailabilityGeneration++
        scheduleWeekJob?.cancel()
        scheduleWeekGeneration++
        _state.value = current.copy(
            showScheduleSheet = false,
            isUpdatingSchedule = false,
            scheduleError = null,
            scheduleAvailability = RescheduleAvailabilityState.Idle,
        )
    }

    fun loadScheduleWeekAvailability(weekStart: String) {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.showScheduleSheet) return

        scheduleWeekJob?.cancel()
        val generation = ++scheduleWeekGeneration
        val earliestDate = earliestAppointmentRequestDate()
        val dates = scheduleWeekDates(weekStart).filter { !it.isBefore(earliestDate) }
        val request = current.request

        _state.value = current.copy(
            scheduleWeekStart = weekStart,
            scheduleDayAvailability = current.scheduleDayAvailability +
                dates.associate { it.toString() to DayAvailability.LOADING },
        )

        scheduleWeekJob = viewModelScope.launch {
            val results = dates.map { date ->
                async {
                    date.toString() to getScheduleAvailability(request, date.toString())
                }
            }.awaitAll()

            if (scheduleWeekGeneration != generation) return@launch
            val latest = _state.value as? RequestDetailState.Data ?: return@launch
            val resolved = results.associate { (date, result) ->
                date to result.fold(
                    onSuccess = ::scheduleDayAvailabilityVerdict,
                    onFailure = { DayAvailability.UNKNOWN },
                )
            }
            _state.value = latest.copy(
                scheduleDayAvailability = latest.scheduleDayAvailability + resolved,
            )
        }
    }

    fun loadScheduleAvailability(date: String) {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.showScheduleSheet) return
        val selectedDate = runCatching { LocalDate.parse(date) }.getOrNull() ?: return
        if (selectedDate.isBefore(earliestAppointmentRequestDate())) return

        scheduleAvailabilityJob?.cancel()
        val generation = ++scheduleAvailabilityGeneration
        val request = current.request
        _state.value = current.copy(
            scheduleAvailability = RescheduleAvailabilityState.Loading(date),
            scheduleError = null,
        )

        scheduleAvailabilityJob = viewModelScope.launch {
            getScheduleAvailability(request, date).fold(
                onSuccess = { availability ->
                    val latest = _state.value
                    if (latest is RequestDetailState.Data &&
                        generation == scheduleAvailabilityGeneration
                    ) {
                        _state.value = latest.copy(
                            scheduleAvailability = RescheduleAvailabilityState.Success(availability),
                            scheduleDayAvailability = latest.scheduleDayAvailability +
                                (date to scheduleDayAvailabilityVerdict(availability)),
                        )
                    }
                },
                onFailure = { error ->
                    val latest = _state.value
                    if (latest is RequestDetailState.Data &&
                        generation == scheduleAvailabilityGeneration
                    ) {
                        _state.value = latest.copy(
                            scheduleAvailability = RescheduleAvailabilityState.Error(
                                date = date,
                                message = patientSafeAppointmentRequestError(
                                    error = error,
                                    fallback = "We couldn't load available times. Try again.",
                                ),
                            ),
                        )
                    }
                },
            )
        }
    }

    fun retryScheduleAvailability() {
        val current = _state.value
        if (current !is RequestDetailState.Data) return
        val date = when (val availability = current.scheduleAvailability) {
            is RescheduleAvailabilityState.Error -> availability.date
            is RescheduleAvailabilityState.Loading -> availability.date
            is RescheduleAvailabilityState.Success -> availability.availability.date
            RescheduleAvailabilityState.Idle -> parseClinicDateTime(current.request.scheduledAt)
                ?.toLocalDate()
                ?.toString()
                ?: current.request.scheduledAt.take(10)
        }
        loadScheduleAvailability(date)
    }

    fun updateSchedule(
        scheduledAt: String,
        alternativeScheduledTimes: List<String> = emptyList(),
    ) {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.request.status.isCancellable) return
        val selectedDate = parseClinicDateTime(scheduledAt)?.toLocalDate() ?: return
        if (selectedDate.isBefore(earliestAppointmentRequestDate())) return

        _state.value = current.copy(
            isUpdatingSchedule = true,
            scheduleError = null,
        )
        viewModelScope.launch {
            repository.updateRequestSchedule(
                id = current.request.id,
                scheduledAt = scheduledAt,
                alternativeScheduledTimes = alternativeScheduledTimes,
            ).fold(
                onSuccess = { request ->
                    scheduleAvailabilityJob?.cancel()
                    scheduleAvailabilityGeneration++
                    scheduleWeekJob?.cancel()
                    scheduleWeekGeneration++
                    val latest = _state.value as? RequestDetailState.Data ?: current
                    _state.value = latest.copy(
                        request = request,
                        showScheduleSheet = false,
                        isUpdatingSchedule = false,
                        scheduleError = null,
                        scheduleAvailability = RescheduleAvailabilityState.Idle,
                    )
                },
                onFailure = { error ->
                    val latest = _state.value as? RequestDetailState.Data ?: current
                    _state.value = latest.copy(
                        isUpdatingSchedule = false,
                        scheduleError = scheduleUpdateError(error),
                    )
                },
            )
        }
    }

    private fun scheduleWeekDates(weekStart: String): List<LocalDate> {
        val start = runCatching { LocalDate.parse(weekStart) }
            .getOrElse { earliestAppointmentRequestDate() }
        return (0 until availabilityWeekLength).map { start.plusDays(it.toLong()) }
    }

    private suspend fun getScheduleAvailability(
        request: AppointmentRequest,
        date: String,
    ): Result<AppointmentAvailability> {
        request.appointmentId?.let { appointmentId ->
            return appointmentRepository.getAppointmentAvailability(date, appointmentId)
        }

        val appointmentTypeId = request.appointmentType?.id
            ?: return Result.failure(IllegalStateException("Missing appointment type"))
        return repository.getAvailability(date, appointmentTypeId)
            .map { it.toAppointmentAvailability(request) }
    }

    private fun AppointmentRequestAvailability.toAppointmentAvailability(
        request: AppointmentRequest,
    ) = AppointmentAvailability(
        date = date,
        timezone = timezone,
        intervalMinutes = intervalMinutes,
        visitReasonId = appointmentTypeId ?: request.appointmentType?.id ?: 0,
        visitDurationMinutes = visitDurationMinutes ?: slotDurationMinutes,
        optometristId = null,
        appointmentId = request.appointmentId,
        dayStatus = dayStatus,
        generatedAt = generatedAt,
        slots = slots.map { slot ->
            AppointmentSlot(
                startsAt = slot.startsAt,
                endsAt = slot.endsAt,
                available = slot.available,
                reason = slot.reason,
            )
        },
    )

    private fun scheduleDayAvailabilityVerdict(availability: AppointmentAvailability): DayAvailability = when {
        !availability.dayStatus.equals("open", ignoreCase = true) -> DayAvailability.CLOSED
        availability.slots.none { it.available } -> DayAvailability.FULL
        else -> DayAvailability.OPEN
    }

    private fun scheduleUpdateError(error: Throwable): String {
        val apiError = error as? ApiDomainError
        return when {
            apiError?.code == "REQUEST_NOT_RESCHEDULABLE" ->
                "This request can no longer be changed. Refresh and try again."
            apiError?.code == "SLOT_UNAVAILABLE" ||
                apiError?.hasAppointmentRequestFieldError("scheduled_at") == true ->
                "That time is no longer available. Choose another time."
            apiError?.httpStatus == 404 ->
                "This request is no longer available. Refresh and try again."
            else -> "We couldn't update this requested time. Please try again."
        }
    }
}
