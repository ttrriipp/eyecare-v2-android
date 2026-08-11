package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentType
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
private const val maxReferralLength = 255
private const val maxAlternatives = 2

/**
 * A 4-step wizard: [Type] → [Schedule] (primary + alternatives) → [Details] (reason, referral,
 * identity) → [Review]. [Submitting], [Success], and [SubmissionError] are transient outcomes.
 */
sealed interface RequestStep {
    data class Type(
        val types: List<AppointmentType> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val selectedType: AppointmentType? = null,
    ) : RequestStep

    data class Schedule(
        val selectedType: AppointmentType,
        val date: String? = null,
        val availability: AppointmentRequestAvailability? = null,
        val isLoadingAvailability: Boolean = false,
        val availabilityError: String? = null,
        val primarySlot: AvailabilitySlot? = null,
        val alternativeSlots: List<AvailabilitySlot> = emptyList(),
        val reasonDraft: String = "",
        val referringSourceDraft: String? = null,
        val identityDraft: AppointmentRequestIdentity? = null,
    ) : RequestStep

    data class Details(
        val selectedType: AppointmentType,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val identityRequired: Boolean,
        val reason: String = "",
        val reasonError: String? = null,
        val referringSource: String = "",
        val referringSourceError: String? = null,
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
        val selectedType: AppointmentType,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String?,
        val identity: AppointmentRequestIdentity? = null,
    ) : RequestStep

    data class Submitting(
        val selectedType: AppointmentType,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String?,
        val identity: AppointmentRequestIdentity? = null,
    ) : RequestStep

    data class Success(
        val request: AppointmentRequest,
        val isFrameReservationOrigin: Boolean = false,
    ) : RequestStep

    data class SubmissionError(
        val selectedType: AppointmentType,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String?,
        val identity: AppointmentRequestIdentity? = null,
        val errorCode: String?,
        val errorMessage: String,
    ) : RequestStep
}

@HiltViewModel
class RequestAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
) : ViewModel() {

    private val _step = MutableStateFlow<RequestStep>(RequestStep.Type())
    val step: StateFlow<RequestStep> = _step.asStateFlow()

    private var availabilityJob: Job? = null
    private var currentLoadKey: Pair<Int, String>? = null // (typeId, date)

    init {
        loadTypes()
    }

    fun loadTypes() {
        _step.value = RequestStep.Type(isLoading = true)
        viewModelScope.launch {
            repository.getAppointmentTypes()
                .onSuccess { types ->
                    val current = _step.value as? RequestStep.Type ?: return@onSuccess
                    _step.value = current.copy(
                        types = types,
                        isLoading = false,
                        error = null,
                    )
                }
                .onFailure { error ->
                    val current = _step.value as? RequestStep.Type ?: return@onFailure
                    _step.value = current.copy(
                        isLoading = false,
                        error = error.message ?: "Failed to load appointment types",
                    )
                }
        }
    }

    fun retryTypes() = loadTypes()

    fun selectType(type: AppointmentType) {
        val current = _step.value as? RequestStep.Type ?: return
        _step.value = current.copy(selectedType = type)
    }

    fun confirmType() {
        val current = _step.value as? RequestStep.Type ?: return
        val type = current.selectedType ?: return
        _step.value = RequestStep.Schedule(selectedType = type)
    }

    fun selectDate(date: String) {
        val current = _step.value as? RequestStep.Schedule ?: return
        val preservePrimary = current.date == date
        availabilityJob?.cancel()
        val loadKey = Pair(current.selectedType.id, date)
        currentLoadKey = loadKey
        _step.value = current.copy(
            date = date,
            availability = null,
            isLoadingAvailability = true,
            availabilityError = null,
            primarySlot = if (preservePrimary) current.primarySlot else null,
            alternativeSlots = if (preservePrimary) current.alternativeSlots else emptyList(),
        )
        availabilityJob = viewModelScope.launch {
            repository.getAvailability(date, current.selectedType.id)
                .onSuccess { availability ->
                    if (currentLoadKey == loadKey) {
                        val schedule = _step.value as? RequestStep.Schedule ?: return@onSuccess
                        if (availability.appointmentTypeId != null &&
                            availability.appointmentTypeId != schedule.selectedType.id
                        ) return@onSuccess
                        _step.value = schedule.copy(
                            availability = availability,
                            isLoadingAvailability = false,
                            availabilityError = null,
                        )
                    }
                }
                .onFailure { error ->
                    if (currentLoadKey == loadKey) {
                        val schedule = _step.value as? RequestStep.Schedule ?: return@onFailure
                        _step.value = schedule.copy(
                            isLoadingAvailability = false,
                            availabilityError = error.message ?: "Failed to load availability",
                        )
                    }
                }
        }
    }

    fun selectPrimarySlot(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        if (!slot.available) return
        _step.value = current.copy(
            primarySlot = slot,
            alternativeSlots = current.alternativeSlots.filter { it.startsAt != slot.startsAt },
        )
    }

    fun addAlternative(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        if (!slot.available) return
        if (slot.startsAt == current.primarySlot?.startsAt) return
        if (current.alternativeSlots.any { it.startsAt == slot.startsAt }) return
        if (current.alternativeSlots.size >= maxAlternatives) return
        _step.value = current.copy(
            alternativeSlots = current.alternativeSlots + slot,
        )
    }

    fun removeAlternative(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        _step.value = current.copy(
            alternativeSlots = current.alternativeSlots.filter { it.startsAt != slot.startsAt },
        )
    }

    fun retryAvailability() {
        val current = _step.value as? RequestStep.Schedule ?: return
        val date = current.date ?: return
        selectDate(date)
    }

    fun confirmSchedule(
        identityDetailsRequired: Boolean = false,
        initialIdentity: AppointmentRequestIdentity? = null,
    ) {
        val current = _step.value as? RequestStep.Schedule ?: return
        val date = current.date ?: return
        val primarySlot = current.primarySlot ?: return
        val identitySeed = current.identityDraft ?: initialIdentity

        _step.value = RequestStep.Details(
            selectedType = current.selectedType,
            date = date,
            primarySlot = primarySlot,
            alternativeSlots = current.alternativeSlots,
            identityRequired = identityDetailsRequired,
            reason = current.reasonDraft,
            referringSource = if (current.selectedType.requiresReferral) {
                current.referringSourceDraft.orEmpty()
            } else {
                ""
            },
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

    fun backToType() {
        val current = _step.value as? RequestStep.Schedule ?: return
        val types = _step.value.let {
            (it as? RequestStep.Schedule)?.let { emptyList<AppointmentType>() } ?: emptyList()
        }
        _step.value = RequestStep.Type(
            types = types,
            isLoading = false,
            selectedType = current.selectedType,
        )
        loadTypes()
    }

    fun backToSchedule() {
        val current = _step.value as? RequestStep.Details ?: return
        _step.value = RequestStep.Schedule(
            selectedType = current.selectedType,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reasonDraft = current.reason,
            referringSourceDraft = if (current.selectedType.requiresReferral) {
                current.referringSource.ifBlank { null }
            } else {
                null
            },
            identityDraft = current.toIdentityDraftOrNull(),
        )
        selectDate(current.date)
    }

    fun updateReason(reason: String) {
        val current = _step.value as? RequestStep.Details ?: return
        _step.value = current.copy(reason = reason, reasonError = null)
    }

    fun updateReferringSource(source: String) {
        val current = _step.value as? RequestStep.Details ?: return
        _step.value = current.copy(referringSource = source, referringSourceError = null)
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
        val current = _step.value as? RequestStep.Details ?: return
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

    fun confirmDetails() {
        val current = _step.value as? RequestStep.Details ?: return

        val reason = current.reason.trim()
        val reasonError = when {
            reason.isBlank() -> "Reason for visit is required"
            reason.length > 1000 -> "Reason must be 1000 characters or less"
            else -> null
        }

        var referringSourceError: String? = null
        val trimmedReferring = current.referringSource.trim()
        if (current.selectedType.requiresReferral) {
            when {
                trimmedReferring.isBlank() -> referringSourceError = "Referral source is required for this appointment type"
                trimmedReferring.length > maxReferralLength -> referringSourceError = "Referral source must be $maxReferralLength characters or less"
            }
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

        if (reasonError != null || referringSourceError != null || errors.isNotEmpty()) {
            _step.value = current.copy(
                reason = reason,
                reasonError = reasonError,
                referringSourceError = referringSourceError,
                errors = errors,
            )
            return
        }

        _step.value = RequestStep.Review(
            selectedType = current.selectedType,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = reason,
            referringSource = if (current.selectedType.requiresReferral) trimmedReferring else null,
            identity = identity,
        )
    }

    fun backFromReview() {
        val current = _step.value as? RequestStep.Review ?: return
        val identity = current.identity
        _step.value = RequestStep.Details(
            selectedType = current.selectedType,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            identityRequired = identity != null,
            reason = current.reason,
            referringSource = current.referringSource.orEmpty(),
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
        val current = _step.value as? RequestStep.Review ?: return
        _step.value = RequestStep.Submitting(
            selectedType = current.selectedType,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = current.reason,
            referringSource = current.referringSource,
            identity = current.identity,
        )
        viewModelScope.launch {
            val alternativeTimes = current.alternativeSlots
                .map { it.startsAt }

            repository.createRequest(
                appointmentTypeId = current.selectedType.id,
                scheduledAt = current.primarySlot.startsAt,
                reasonForVisit = current.reason,
                alternativeScheduledTimes = alternativeTimes.ifEmpty { null },
                referringSource = current.referringSource,
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
                    selectedType = submitting.selectedType,
                    date = submitting.date,
                    primarySlot = submitting.primarySlot,
                    alternativeSlots = submitting.alternativeSlots,
                    reason = submitting.reason,
                    referringSource = submitting.referringSource,
                    identity = submitting.identity,
                    errorCode = apiError?.code,
                    errorMessage = apiError?.message ?: "Failed to submit request",
                )
            }
        }
    }

    fun handleSubmissionError() {
        val current = _step.value as? RequestStep.SubmissionError ?: return
        when (current.errorCode) {
            "SLOT_UNAVAILABLE" -> {
                _step.value = RequestStep.Schedule(
                    selectedType = current.selectedType,
                    date = current.date,
                    reasonDraft = current.reason,
                    referringSourceDraft = current.referringSource,
                    identityDraft = current.identity,
                )
                selectDate(current.date)
            }
            "ACTIVE_REQUEST_LIMIT_REACHED" -> {
                _step.value = current.copy(
                    errorMessage = "You already have pending requests. Please wait for them to be resolved or cancel one.",
                )
            }
            else -> {
                _step.value = RequestStep.Review(
                    selectedType = current.selectedType,
                    date = current.date,
                    primarySlot = current.primarySlot,
                    alternativeSlots = current.alternativeSlots,
                    reason = current.reason,
                    referringSource = current.referringSource,
                    identity = current.identity,
                )
            }
        }
    }

    private fun RequestStep.Details.toIdentityDraftOrNull(): AppointmentRequestIdentity? {
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
