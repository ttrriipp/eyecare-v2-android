package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestStep {
    data object ChooseDate : RequestStep
    data class ChooseSlot(
        val date: String,
        val availability: AppointmentRequestAvailability? = null,
        val isLoading: Boolean = false,
        val error: String? = null,
        val selectedSlot: AvailabilitySlot? = null,
    ) : RequestStep
    data class EnterReason(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String = "",
        val reasonError: String? = null,
    ) : RequestStep
    data class Review(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
    ) : RequestStep
    data class Submitting(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
    ) : RequestStep
    data class Success(
        val request: AppointmentRequest,
        val isFrameReservationOrigin: Boolean = false,
    ) : RequestStep
    data class SubmissionError(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
        val errorCode: String?,
        val errorMessage: String,
    ) : RequestStep
}

@HiltViewModel
class RequestAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
) : ViewModel() {

    private val _step = MutableStateFlow<RequestStep>(RequestStep.ChooseDate)
    val step: StateFlow<RequestStep> = _step.asStateFlow()

    private var availabilityJob: Job? = null
    private var currentLoadDate: String? = null

    fun selectDate(date: String) {
        availabilityJob?.cancel()
        currentLoadDate = date
        _step.value = RequestStep.ChooseSlot(date = date, isLoading = true)
        availabilityJob = viewModelScope.launch {
            repository.getAvailability(date)
                .onSuccess { availability ->
                    if (currentLoadDate == date) {
                        _step.value = RequestStep.ChooseSlot(
                            date = date,
                            availability = availability,
                        )
                    }
                }
                .onFailure { error ->
                    if (currentLoadDate == date) {
                        _step.value = RequestStep.ChooseSlot(
                            date = date,
                            error = error.message ?: "Failed to load availability",
                        )
                    }
                }
        }
    }

    fun selectSlot(slot: AvailabilitySlot) {
        val current = _step.value
        if (current !is RequestStep.ChooseSlot || !slot.available) return
        _step.value = current.copy(selectedSlot = slot)
    }

    fun confirmSlot() {
        val current = _step.value
        if (current !is RequestStep.ChooseSlot || current.selectedSlot == null) return
        _step.value = RequestStep.EnterReason(
            date = current.date,
            slot = current.selectedSlot,
        )
    }

    fun updateReason(reason: String) {
        val current = _step.value
        if (current is RequestStep.EnterReason) {
            _step.value = current.copy(reason = reason, reasonError = null)
        }
    }

    fun confirmReason() {
        val current = _step.value
        if (current !is RequestStep.EnterReason) return
        val trimmed = current.reason.trim()
        if (trimmed.isBlank()) {
            _step.value = current.copy(reasonError = "Reason for visit is required")
            return
        }
        if (trimmed.length > 1000) {
            _step.value = current.copy(reasonError = "Reason must be 1000 characters or less")
            return
        }
        _step.value = RequestStep.Review(
            date = current.date,
            slot = current.slot,
            reason = trimmed,
        )
    }

    fun submit(isFrameReservationOrigin: Boolean = false) {
        val current = _step.value
        if (current !is RequestStep.Review) return
        _step.value = RequestStep.Submitting(
            date = current.date,
            slot = current.slot,
            reason = current.reason,
        )
        viewModelScope.launch {
            repository.createRequest(
                scheduledAt = current.slot.startsAt,
                reasonForVisit = current.reason,
            ).onSuccess { request ->
                _step.value = RequestStep.Success(
                    request = request,
                    isFrameReservationOrigin = isFrameReservationOrigin,
                )
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                val submitting = _step.value as? RequestStep.Submitting ?: return@launch
                _step.value = RequestStep.SubmissionError(
                    date = submitting.date,
                    slot = submitting.slot,
                    reason = submitting.reason,
                    errorCode = apiError?.code,
                    errorMessage = apiError?.message ?: "Failed to submit request",
                )
            }
        }
    }

    fun retryAvailability() {
        val current = _step.value
        if (current is RequestStep.ChooseSlot) {
            selectDate(current.date)
        }
    }

    fun backToSlotSelection() {
        val current = _step.value
        if (current is RequestStep.EnterReason) {
            _step.value = RequestStep.ChooseSlot(date = current.date)
            selectDate(current.date)
        }
    }

    fun backToReason() {
        val current = _step.value
        if (current is RequestStep.Review) {
            _step.value = RequestStep.EnterReason(
                date = current.date,
                slot = current.slot,
                reason = current.reason,
            )
        }
    }

    fun handleSubmissionError() {
        val current = _step.value
        if (current is RequestStep.SubmissionError) {
            when (current.errorCode) {
                "SLOT_UNAVAILABLE" -> {
                    _step.value = RequestStep.ChooseSlot(date = current.date)
                    selectDate(current.date)
                }
                "ACTIVE_REQUEST_LIMIT_REACHED" -> {
                    _step.value = RequestStep.SubmissionError(
                        date = current.date,
                        slot = current.slot,
                        reason = current.reason,
                        errorCode = current.errorCode,
                        errorMessage = "You already have pending requests. Please wait for them to be resolved or cancel one.",
                    )
                }
                else -> {
                    _step.value = RequestStep.Review(
                        date = current.date,
                        slot = current.slot,
                        reason = current.reason,
                    )
                }
            }
        }
    }
}
