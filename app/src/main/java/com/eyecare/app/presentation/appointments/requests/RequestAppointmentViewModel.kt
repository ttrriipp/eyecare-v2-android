package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

private val appointmentRequestZone = ZoneId.of("Asia/Manila")
private val appointmentRequestEmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private const val maxIdentityFieldLength = 255

/**
 * A 3-step wizard: [Schedule] (date + time), [ProfileAndReason] (reason for visit, plus the
 * patient's own profile when the account has no linked clinic record yet), then [Review].
 * [Submitting], [Success], and [SubmissionError] are transient outcomes of Review, not
 * additional steps the patient navigates through.
 */
sealed interface RequestStep {
    data class Schedule(
        val date: String? = null,
        val availability: AppointmentRequestAvailability? = null,
        val isLoadingAvailability: Boolean = false,
        val availabilityError: String? = null,
        val selectedSlot: AvailabilitySlot? = null,
        val reasonDraft: String = "",
        val identityDraft: AppointmentRequestIdentity? = null,
    ) : RequestStep

    data class ProfileAndReason(
        val date: String,
        val slot: AvailabilitySlot,
        val identityRequired: Boolean,
        val reason: String = "",
        val reasonError: String? = null,
        val phone: String = "",
        val email: String = "",
        val firstName: String = "",
        val middleName: String = "",
        val lastName: String = "",
        val dateOfBirth: String = "",
        val gender: AppointmentRequestGender? = null,
        val occupation: String = "",
        val address: String = "",
        val errors: Map<String, String> = emptyMap(),
    ) : RequestStep

    data class Review(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
        val identity: AppointmentRequestIdentity? = null,
    ) : RequestStep
    data class Submitting(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
        val identity: AppointmentRequestIdentity? = null,
    ) : RequestStep
    data class Success(
        val request: AppointmentRequest,
        val isFrameReservationOrigin: Boolean = false,
    ) : RequestStep
    data class SubmissionError(
        val date: String,
        val slot: AvailabilitySlot,
        val reason: String,
        val identity: AppointmentRequestIdentity? = null,
        val errorCode: String?,
        val errorMessage: String,
    ) : RequestStep
}

@HiltViewModel
class RequestAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
) : ViewModel() {

    private val _step = MutableStateFlow<RequestStep>(RequestStep.Schedule())
    val step: StateFlow<RequestStep> = _step.asStateFlow()

    private var availabilityJob: Job? = null
    private var currentLoadDate: String? = null

    fun selectDate(date: String) {
        val current = _step.value as? RequestStep.Schedule ?: RequestStep.Schedule()
        val preserveSelection = current.date == date
        availabilityJob?.cancel()
        currentLoadDate = date
        _step.value = current.copy(
            date = date,
            availability = null,
            isLoadingAvailability = true,
            availabilityError = null,
            selectedSlot = if (preserveSelection) current.selectedSlot else null,
        )
        availabilityJob = viewModelScope.launch {
            repository.getAvailability(date, appointmentTypeId = 0)
                .onSuccess { availability ->
                    if (currentLoadDate == date) {
                        val schedule = _step.value as? RequestStep.Schedule ?: return@onSuccess
                        _step.value = schedule.copy(
                            availability = availability,
                            isLoadingAvailability = false,
                            availabilityError = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (currentLoadDate == date) {
                        val schedule = _step.value as? RequestStep.Schedule ?: return@onFailure
                        _step.value = schedule.copy(
                            isLoadingAvailability = false,
                            availabilityError = error.message ?: "Failed to load availability",
                        )
                    }
                }
        }
    }

    fun selectSlot(slot: AvailabilitySlot) {
        val current = _step.value
        if (current !is RequestStep.Schedule || !slot.available) return
        _step.value = current.copy(selectedSlot = slot)
    }

    fun retryAvailability() {
        val current = _step.value
        if (current is RequestStep.Schedule && current.date != null) {
            selectDate(current.date)
        }
    }

    fun confirmSchedule(
        identityDetailsRequired: Boolean = false,
        initialIdentity: AppointmentRequestIdentity? = null,
    ) {
        val current = _step.value
        if (current !is RequestStep.Schedule) return
        val date = current.date ?: return
        val slot = current.selectedSlot ?: return
        val identitySeed = current.identityDraft ?: initialIdentity

        _step.value = RequestStep.ProfileAndReason(
            date = date,
            slot = slot,
            identityRequired = identityDetailsRequired,
            reason = current.reasonDraft,
            phone = identitySeed?.phone.orEmpty(),
            email = identitySeed?.email.orEmpty(),
            firstName = identitySeed?.firstName.orEmpty(),
            middleName = identitySeed?.middleName.orEmpty(),
            lastName = identitySeed?.lastName.orEmpty(),
            dateOfBirth = identitySeed?.dateOfBirth.orEmpty(),
            gender = identitySeed?.gender,
            occupation = identitySeed?.occupation.orEmpty(),
            address = identitySeed?.address.orEmpty(),
        )
    }

    fun updateReason(reason: String) {
        val current = _step.value
        if (current is RequestStep.ProfileAndReason) {
            _step.value = current.copy(reason = reason, reasonError = null)
        }
    }

    fun updateIdentity(
        phone: String? = null,
        email: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        gender: AppointmentRequestGender? = null,
        occupation: String? = null,
        address: String? = null,
    ) {
        val current = _step.value
        if (current is RequestStep.ProfileAndReason) {
            _step.value = current.copy(
                phone = phone ?: current.phone,
                email = email ?: current.email,
                firstName = firstName ?: current.firstName,
                middleName = middleName ?: current.middleName,
                lastName = lastName ?: current.lastName,
                dateOfBirth = dateOfBirth ?: current.dateOfBirth,
                gender = gender ?: current.gender,
                occupation = occupation ?: current.occupation,
                address = address ?: current.address,
                errors = emptyMap(),
            )
        }
    }

    fun confirmProfileAndReason() {
        val current = _step.value
        if (current !is RequestStep.ProfileAndReason) return

        val reason = current.reason.trim()
        val reasonError = when {
            reason.isBlank() -> "Reason for visit is required"
            reason.length > 1000 -> "Reason must be 1000 characters or less"
            else -> null
        }

        var identity: AppointmentRequestIdentity? = null
        val errors = mutableMapOf<String, String>()

        if (current.identityRequired) {
            val phone = current.phone.trim()
            val email = current.email.trim()
            val firstName = current.firstName.trim()
            val middleName = current.middleName.trim().ifBlank { null }
            val lastName = current.lastName.trim()
            val dateOfBirth = current.dateOfBirth.trim()
            val occupation = current.occupation.trim()
            val address = current.address.trim()

            if (phone.isBlank()) errors["phone"] = "A verified phone number is required"
            if (email.length > maxIdentityFieldLength) {
                errors["email"] = "Email must be 255 characters or less"
            } else if (email.isNotBlank() && !appointmentRequestEmailPattern.matches(email)) {
                errors["email"] = "Enter a valid email address"
            }
            if (firstName.isBlank()) errors["firstName"] = "First name is required"
            if (firstName.length > maxIdentityFieldLength) {
                errors["firstName"] = "First name must be 255 characters or less"
            }
            if (middleName != null && middleName.length > maxIdentityFieldLength) {
                errors["middleName"] = "Middle name must be 255 characters or less"
            }
            if (lastName.isBlank()) errors["lastName"] = "Last name is required"
            if (lastName.length > maxIdentityFieldLength) {
                errors["lastName"] = "Last name must be 255 characters or less"
            }
            if (dateOfBirth.isBlank()) {
                errors["dateOfBirth"] = "Date of birth is required"
            } else {
                val parsedDate = runCatching { LocalDate.parse(dateOfBirth) }.getOrNull()
                when {
                    parsedDate == null -> errors["dateOfBirth"] = "Enter a valid date of birth"
                    !parsedDate.isBefore(LocalDate.now(appointmentRequestZone)) -> {
                        errors["dateOfBirth"] = "Date of birth must be before today"
                    }
                }
            }
            if (current.gender == null) errors["gender"] = "Gender is required"
            if (occupation.isBlank()) errors["occupation"] = "Occupation is required"
            if (occupation.length > maxIdentityFieldLength) {
                errors["occupation"] = "Occupation must be 255 characters or less"
            }
            if (address.isBlank()) errors["address"] = "Address is required"
            if (address.length > maxIdentityFieldLength) {
                errors["address"] = "Address must be 255 characters or less"
            }

            if (errors.isEmpty()) {
                identity = AppointmentRequestIdentity(
                    phone = phone,
                    email = email.ifBlank { null },
                    firstName = firstName,
                    middleName = middleName,
                    lastName = lastName,
                    dateOfBirth = dateOfBirth,
                    gender = current.gender,
                    occupation = occupation,
                    address = address,
                )
            }
        }

        if (reasonError != null || errors.isNotEmpty()) {
            _step.value = current.copy(reason = reason, reasonError = reasonError, errors = errors)
            return
        }

        _step.value = RequestStep.Review(
            date = current.date,
            slot = current.slot,
            reason = reason,
            identity = identity,
        )
    }

    fun backToSchedule() {
        val current = _step.value
        if (current !is RequestStep.ProfileAndReason) return
        _step.value = RequestStep.Schedule(
            date = current.date,
            selectedSlot = current.slot,
            reasonDraft = current.reason,
            identityDraft = current.toIdentityDraftOrNull(),
        )
        selectDate(current.date)
    }

    fun backFromReview() {
        val current = _step.value
        if (current !is RequestStep.Review) return

        val identity = current.identity
        _step.value = RequestStep.ProfileAndReason(
            date = current.date,
            slot = current.slot,
            identityRequired = identity != null,
            reason = current.reason,
            phone = identity?.phone.orEmpty(),
            email = identity?.email.orEmpty(),
            firstName = identity?.firstName.orEmpty(),
            middleName = identity?.middleName.orEmpty(),
            lastName = identity?.lastName.orEmpty(),
            dateOfBirth = identity?.dateOfBirth.orEmpty(),
            gender = identity?.gender,
            occupation = identity?.occupation.orEmpty(),
            address = identity?.address.orEmpty(),
        )
    }

    fun submit(isFrameReservationOrigin: Boolean = false) {
        val current = _step.value
        if (current !is RequestStep.Review) return
        _step.value = RequestStep.Submitting(
            date = current.date,
            slot = current.slot,
            reason = current.reason,
            identity = current.identity,
        )
        viewModelScope.launch {
            repository.createRequest(
                appointmentTypeId = 0,
                scheduledAt = current.slot.startsAt,
                reasonForVisit = current.reason,
                identity = current.identity,
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
                    identity = submitting.identity,
                    errorCode = apiError?.code,
                    errorMessage = apiError?.message ?: "Failed to submit request",
                )
            }
        }
    }

    fun handleSubmissionError() {
        val current = _step.value
        if (current is RequestStep.SubmissionError) {
            when (current.errorCode) {
                "SLOT_UNAVAILABLE" -> {
                    _step.value = RequestStep.Schedule(
                        date = current.date,
                        reasonDraft = current.reason,
                        identityDraft = current.identity,
                    )
                    selectDate(current.date)
                }
                "ACTIVE_REQUEST_LIMIT_REACHED" -> {
                    _step.value = RequestStep.SubmissionError(
                        date = current.date,
                        slot = current.slot,
                        reason = current.reason,
                        identity = current.identity,
                        errorCode = current.errorCode,
                        errorMessage = "You already have pending requests. Please wait for them to be resolved or cancel one.",
                    )
                }
                else -> {
                    _step.value = RequestStep.Review(
                        date = current.date,
                        slot = current.slot,
                        reason = current.reason,
                        identity = current.identity,
                    )
                }
            }
        }
    }

    private fun RequestStep.ProfileAndReason.toIdentityDraftOrNull(): AppointmentRequestIdentity? {
        if (!identityRequired) return null
        return AppointmentRequestIdentity(
            phone = phone.ifBlank { null },
            email = email.ifBlank { null },
            firstName = firstName.ifBlank { null },
            middleName = middleName.ifBlank { null },
            lastName = lastName.ifBlank { null },
            dateOfBirth = dateOfBirth.ifBlank { null },
            gender = gender,
            occupation = occupation.ifBlank { null },
            address = address.ifBlank { null },
        )
    }
}
