package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

private val appointmentRequestZone = ZoneId.of("Asia/Manila")
private val appointmentRequestEmailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private const val maxIdentityFieldLength = 255
private const val maxReferralLength = 255
private const val maxReasonLength = 1000
internal const val maxAlternatives = 2

/** Days shown at once in the schedule step's date strip. */
internal const val availabilityWeekLength = 7

/**
 * How a single date in the week strip looks before the patient commits to it. The clinic's
 * availability endpoint answers one date per call, so the strip fetches its whole week in
 * parallel and caches the verdict per date.
 */
enum class DayAvailability { UNKNOWN, LOADING, OPEN, CLOSED, FULL }

/**
 * Which selection the schedule list is currently collecting. Splitting this into two explicit
 * phases is what lets every row carry exactly one meaning: in [PREFERRED] a tap chooses the one
 * preferred time, in [ALTERNATIVES] a tap toggles a numbered backup.
 */
enum class SchedulePhase { PREFERRED, ALTERNATIVES }

/**
 * The wizard steps a patient actually walks through. Identity is only present when the account
 * has no linked clinic record, so a linked patient sees four steps and an unlinked one sees five.
 */
enum class RequestStepId(val label: String) {
    TYPE("Type"),
    SCHEDULE("Schedule"),
    REASON("Reason"),
    IDENTITY("Details"),
    REVIEW("Review"),
}

/** The step labels for a run of the wizard, minus the identity step when it does not apply. */
fun requestStepLabels(identityRequired: Boolean): List<String> =
    RequestStepId.entries
        .filter { identityRequired || it != RequestStepId.IDENTITY }
        .map { it.label }

/** Zero-based index of [step] within [requestStepLabels], for the step indicator. */
fun requestStepIndex(step: RequestStepId, identityRequired: Boolean): Int =
    RequestStepId.entries
        .filter { identityRequired || it != RequestStepId.IDENTITY }
        .indexOf(step)
        .coerceAtLeast(0)

/**
 * A 4- or 5-step wizard: [Type] → [Schedule] (preferred time, then optional backups) →
 * [Reason] → [Identity] (unlinked accounts only) → [Review]. [Submitting], [Success], and
 * [SubmissionError] are transient outcomes.
 */
sealed interface RequestStep {
    /** True when this step holds patient-entered work that a stray Back would destroy. */
    val hasUnsavedInput: Boolean get() = false

    data class Type(
        val types: List<AppointmentType> = emptyList(),
        val isLoading: Boolean = true,
        val error: String? = null,
        val selectedType: AppointmentType? = null,
        val notice: String? = null,
    ) : RequestStep

    data class Schedule(
        val selectedType: AppointmentType,
        val identityRequired: Boolean,
        val weekStart: String,
        val dayAvailability: Map<String, DayAvailability> = emptyMap(),
        val date: String? = null,
        val primaryDate: String? = null,
        val availability: AppointmentRequestAvailability? = null,
        val isLoadingAvailability: Boolean = false,
        val availabilityError: String? = null,
        val phase: SchedulePhase = SchedulePhase.PREFERRED,
        val primarySlot: AvailabilitySlot? = null,
        val alternativeSlots: List<AvailabilitySlot> = emptyList(),
        val reasonDraft: String = "",
        val referringSourceDraft: String? = null,
        val identityDraft: AppointmentRequestIdentity? = null,
    ) : RequestStep {
        override val hasUnsavedInput: Boolean get() = primarySlot != null
    }

    data class Reason(
        val selectedType: AppointmentType,
        val identityRequired: Boolean,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String = "",
        val reasonError: String? = null,
        val referringSource: String = "",
        val referringSourceError: String? = null,
        val identityDraft: AppointmentRequestIdentity? = null,
    ) : RequestStep {
        override val hasUnsavedInput: Boolean
            get() = reason.isNotBlank() || referringSource.isNotBlank()
    }

    data class Identity(
        val selectedType: AppointmentType,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String,
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
        /** Field key the screen should scroll to and focus, consumed once. */
        val focusField: String? = null,
    ) : RequestStep {
        override val hasUnsavedInput: Boolean
            get() = listOf(email, firstName, middleName, lastName, dateOfBirth, occupation, address)
                .any { it.isNotBlank() } || gender != null
    }

    data class Review(
        val selectedType: AppointmentType,
        val identityRequired: Boolean,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String?,
        val identity: AppointmentRequestIdentity? = null,
        val isSubmitting: Boolean = false,
    ) : RequestStep {
        override val hasUnsavedInput: Boolean get() = true
    }

    data class Success(
        val request: AppointmentRequest,
        val isFrameReservationOrigin: Boolean = false,
    ) : RequestStep

    data class SubmissionError(
        val selectedType: AppointmentType,
        val identityRequired: Boolean,
        val date: String,
        val primarySlot: AvailabilitySlot,
        val alternativeSlots: List<AvailabilitySlot>,
        val reason: String,
        val referringSource: String?,
        val identity: AppointmentRequestIdentity? = null,
        val errorCode: String?,
        val errorMessage: String,
        val fieldErrors: Map<String, List<String>> = emptyMap(),
        val typeUnavailable: Boolean = false,
        val referralValidationFailure: Boolean = false,
        /** False once the error has no recovery left beyond leaving the flow. */
        val canRetry: Boolean = true,
    ) : RequestStep {
        override val hasUnsavedInput: Boolean get() = true
    }
}

@HiltViewModel
class RequestAppointmentViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _step = MutableStateFlow<RequestStep>(RequestStep.Type())
    val step: StateFlow<RequestStep> = _step.asStateFlow()

    private var availabilityJob: Job? = null
    private var weekJob: Job? = null
    private var availabilityGeneration = 0L
    private var weekGeneration = 0L
    private var typeCatalogGeneration = 0L

    /**
     * Everything the patient has typed, mirrored into [SavedStateHandle] so a low-memory process
     * kill does not cost them nine identity fields. Slot and type selections are re-derived from
     * the server on restore rather than trusted from a stale bundle.
     */
    private var draft: RequestDraft
        get() = savedStateHandle.get<RequestDraft>(draftKey) ?: RequestDraft()
        set(value) { savedStateHandle[draftKey] = value }

    init {
        loadTypes()
    }

    // ---------------------------------------------------------------- type step

    fun loadTypes(notice: String? = null, preserveSelection: AppointmentType? = null) {
        val generation = ++typeCatalogGeneration
        _step.value = RequestStep.Type(
            isLoading = true,
            notice = notice,
            selectedType = preserveSelection,
        )
        viewModelScope.launch {
            repository.getAppointmentTypes()
                .onSuccess { types ->
                    if (typeCatalogGeneration != generation) return@onSuccess
                    val current = _step.value as? RequestStep.Type ?: return@onSuccess
                    // Only keep a restored selection the catalog still offers.
                    val stillOffered = current.selectedType?.let { selected ->
                        types.firstOrNull { it.id == selected.id }
                    }
                    _step.value = current.copy(
                        types = types,
                        isLoading = false,
                        error = null,
                        selectedType = stillOffered,
                    )
                }
                .onFailure { error ->
                    if (typeCatalogGeneration != generation) return@onFailure
                    val current = _step.value as? RequestStep.Type ?: return@onFailure
                    _step.value = current.copy(
                        isLoading = false,
                        error = patientSafeAppointmentRequestError(
                            error = error,
                            fallback = "We couldn't load appointment types. Please try again.",
                        ),
                    )
                }
        }
    }

    fun retryTypes() = loadTypes()

    fun selectType(type: AppointmentType) {
        val current = _step.value as? RequestStep.Type ?: return
        _step.value = current.copy(selectedType = type)
    }

    fun confirmType(identityRequired: Boolean, initialIdentity: AppointmentRequestIdentity? = null) {
        val current = _step.value as? RequestStep.Type ?: return
        val type = current.selectedType ?: return
        val saved = draft
        draft = saved.copy(appointmentTypeId = type.id)
        val today = LocalDate.now(appointmentRequestZone)
        val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        _step.value = RequestStep.Schedule(
            selectedType = type,
            identityRequired = identityRequired,
            weekStart = weekStart,
            date = today.toString(),
            reasonDraft = saved.reason,
            referringSourceDraft = saved.referringSource,
            identityDraft = saved.toIdentityOrNull() ?: initialIdentity,
        )
        loadWeekAvailability(weekStart, type.id)
        selectDate(today.toString())
    }

    // ------------------------------------------------------------ schedule step

    /** The seven dates the strip currently shows, starting at [weekStart]. */
    fun weekDates(weekStart: String): List<LocalDate> {
        val start = runCatching { LocalDate.parse(weekStart) }
            .getOrElse { LocalDate.now(appointmentRequestZone) }
        return (0 until availabilityWeekLength).map { start.plusDays(it.toLong()) }
    }

    /**
     * Fetches all seven visible days in parallel so the strip can mark each one open, closed, or
     * fully booked before the patient commits a tap. The clinic API answers one date per call,
     * which is why this fans out rather than requesting a range.
     */
    private fun loadWeekAvailability(weekStart: String, appointmentTypeId: Int) {
        weekJob?.cancel()
        val generation = ++weekGeneration
        val today = LocalDate.now(appointmentRequestZone)
        val dates = weekDates(weekStart).filter { !it.isBefore(today) }

        val current = _step.value as? RequestStep.Schedule ?: return
        _step.value = current.copy(
            dayAvailability = current.dayAvailability +
                dates.associate { it.toString() to DayAvailability.LOADING },
        )

        weekJob = viewModelScope.launch {
            val results = dates.map { date ->
                async {
                    date.toString() to repository.getAvailability(date.toString(), appointmentTypeId)
                }
            }.awaitAll()

            if (weekGeneration != generation) return@launch
            val schedule = _step.value as? RequestStep.Schedule ?: return@launch
            if (schedule.selectedType.id != appointmentTypeId) return@launch

            val resolved = results.associate { (date, result) ->
                date to result.fold(
                    onSuccess = { availability ->
                        when {
                            !availability.dayStatus.equals("open", ignoreCase = true) ->
                                DayAvailability.CLOSED
                            availability.slots.none { it.available } -> DayAvailability.FULL
                            else -> DayAvailability.OPEN
                        }
                    },
                    onFailure = { DayAvailability.UNKNOWN },
                )
            }
            _step.value = schedule.copy(dayAvailability = schedule.dayAvailability + resolved)
        }
    }

    fun showWeek(weekStart: String) {
        val current = _step.value as? RequestStep.Schedule ?: return
        _step.value = current.copy(weekStart = weekStart)
        loadWeekAvailability(weekStart, current.selectedType.id)
    }

    fun retryWeek() {
        val current = _step.value as? RequestStep.Schedule ?: return
        loadWeekAvailability(current.weekStart, current.selectedType.id)
    }

    fun selectDate(date: String) {
        val current = _step.value as? RequestStep.Schedule ?: return
        availabilityJob?.cancel()
        val generation = ++availabilityGeneration
        _step.value = current.copy(
            date = date,
            availability = null,
            isLoadingAvailability = true,
            availabilityError = null,
        )
        availabilityJob = viewModelScope.launch {
            repository.getAvailability(date, current.selectedType.id)
                .onSuccess { availability ->
                    if (availabilityGeneration != generation) return@onSuccess
                    val schedule = _step.value as? RequestStep.Schedule ?: return@onSuccess
                    if (availability.appointmentTypeId != null &&
                        availability.appointmentTypeId != schedule.selectedType.id
                    ) return@onSuccess
                    val dayVerdict = when {
                        !availability.dayStatus.equals("open", ignoreCase = true) ->
                            DayAvailability.CLOSED
                        availability.slots.none { it.available } -> DayAvailability.FULL
                        else -> DayAvailability.OPEN
                    }
                    _step.value = schedule.copy(
                        availability = availability,
                        isLoadingAvailability = false,
                        availabilityError = null,
                        dayAvailability = schedule.dayAvailability + (date to dayVerdict),
                    )
                }
                .onFailure { error ->
                    if (availabilityGeneration != generation) return@onFailure
                    val schedule = _step.value as? RequestStep.Schedule ?: return@onFailure
                    _step.value = schedule.copy(
                        isLoadingAvailability = false,
                        availabilityError = patientSafeAppointmentRequestError(
                            error = error,
                            fallback = "We couldn't load times for this day. Please try again.",
                        ),
                    )
                }
        }
    }

    fun retryAvailability() {
        val current = _step.value as? RequestStep.Schedule ?: return
        selectDate(current.date ?: return)
    }

    /** Phase 1: a tap names the one preferred time. */
    fun selectPrimarySlot(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        if (!slot.available) return
        _step.value = current.copy(
            primarySlot = slot,
            primaryDate = current.date,
            // A time cannot be both the preferred choice and a backup for itself.
            alternativeSlots = current.alternativeSlots.filter { it.startsAt != slot.startsAt },
        )
    }

    fun startAddingAlternatives() {
        val current = _step.value as? RequestStep.Schedule ?: return
        if (current.primarySlot == null) return
        _step.value = current.copy(phase = SchedulePhase.ALTERNATIVES)
    }

    fun finishAddingAlternatives() {
        val current = _step.value as? RequestStep.Schedule ?: return
        _step.value = current.copy(phase = SchedulePhase.PREFERRED)
    }

    /** Phase 2: a tap toggles a numbered backup, in the order the patient adds them. */
    fun toggleAlternative(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        if (!slot.available) return
        if (slot.startsAt == current.primarySlot?.startsAt) return
        val existing = current.alternativeSlots.firstOrNull { it.startsAt == slot.startsAt }
        val updated = when {
            existing != null -> current.alternativeSlots.filter { it.startsAt != slot.startsAt }
            current.alternativeSlots.size >= maxAlternatives -> return
            else -> current.alternativeSlots + slot
        }
        _step.value = current.copy(alternativeSlots = updated)
    }

    fun removeAlternative(slot: AvailabilitySlot) {
        val current = _step.value as? RequestStep.Schedule ?: return
        _step.value = current.copy(
            alternativeSlots = current.alternativeSlots.filter { it.startsAt != slot.startsAt },
        )
    }

    fun confirmSchedule() {
        val current = _step.value as? RequestStep.Schedule ?: return
        val date = current.primaryDate ?: current.date ?: return
        val primarySlot = current.primarySlot ?: return
        _step.value = RequestStep.Reason(
            selectedType = current.selectedType,
            identityRequired = current.identityRequired,
            date = date,
            primarySlot = primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = current.reasonDraft,
            referringSource = if (current.selectedType.requiresReferral) {
                current.referringSourceDraft.orEmpty()
            } else {
                ""
            },
            identityDraft = current.identityDraft,
        )
    }

    fun backToType() {
        val current = _step.value as? RequestStep.Schedule ?: return
        availabilityJob?.cancel()
        weekJob?.cancel()
        availabilityGeneration++
        weekGeneration++
        // Re-fetch the catalog but keep the patient's choice highlighted while it loads.
        loadTypes(preserveSelection = current.selectedType)
    }

    // -------------------------------------------------------------- reason step

    fun updateReason(reason: String) {
        val current = _step.value as? RequestStep.Reason ?: return
        draft = draft.copy(reason = reason)
        _step.value = current.copy(reason = reason, reasonError = null)
    }

    fun updateReferringSource(source: String) {
        val current = _step.value as? RequestStep.Reason ?: return
        draft = draft.copy(referringSource = source.ifBlank { null })
        _step.value = current.copy(referringSource = source, referringSourceError = null)
    }

    fun confirmReason(initialIdentity: AppointmentRequestIdentity? = null) {
        val current = _step.value as? RequestStep.Reason ?: return

        val reason = current.reason.trim()
        val reasonError = when {
            reason.isBlank() -> "Tell the clinic what you'd like to be seen for."
            reason.length > maxReasonLength ->
                "Please shorten this to $maxReasonLength characters or fewer."
            else -> null
        }

        val trimmedReferring = current.referringSource.trim()
        val referringSourceError = if (current.selectedType.requiresReferral) {
            when {
                trimmedReferring.isBlank() ->
                    "${current.selectedType.name} needs a referral. Add who referred you."
                trimmedReferring.length > maxReferralLength ->
                    "Please shorten this to $maxReferralLength characters or fewer."
                else -> null
            }
        } else {
            null
        }

        if (reasonError != null || referringSourceError != null) {
            _step.value = current.copy(
                reason = reason,
                reasonError = reasonError,
                referringSourceError = referringSourceError,
            )
            return
        }

        val referringSource = if (current.selectedType.requiresReferral) trimmedReferring else ""
        if (current.identityRequired) {
            val seed = current.identityDraft ?: initialIdentity
            _step.value = RequestStep.Identity(
                selectedType = current.selectedType,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = reason,
                referringSource = referringSource,
                phone = seed?.phone.orEmpty(),
                email = seed?.email.orEmpty(),
                firstName = seed?.firstName.orEmpty(),
                middleName = seed?.middleName.orEmpty(),
                lastName = seed?.lastName.orEmpty(),
                dateOfBirth = seed?.dateOfBirth.orEmpty(),
                gender = seed?.gender,
                occupation = seed?.occupation.orEmpty(),
                address = seed?.address.orEmpty(),
            )
        } else {
            _step.value = RequestStep.Review(
                selectedType = current.selectedType,
                identityRequired = false,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = reason,
                referringSource = referringSource.ifBlank { null },
            )
        }
    }

    fun backToSchedule() {
        val current = _step.value as? RequestStep.Reason ?: return
        restoreSchedule(
            selectedType = current.selectedType,
            identityRequired = current.identityRequired,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reasonDraft = current.reason,
            referringSourceDraft = current.referringSource.ifBlank { null },
            identityDraft = current.identityDraft,
        )
    }

    // ------------------------------------------------------------ identity step

    fun updateIdentity(
        email: String? = null,
        firstName: String? = null,
        middleName: String? = null,
        lastName: String? = null,
        dateOfBirth: String? = null,
        gender: AppointmentRequestGender? = null,
        occupation: String? = null,
        address: String? = null,
    ) {
        val current = _step.value as? RequestStep.Identity ?: return
        // Clear only the edited field's error. Wiping the whole map would tell the patient every
        // problem is fixed the moment they touch one field.
        val touched = listOfNotNull(
            email?.let { "email" },
            firstName?.let { "firstName" },
            middleName?.let { "middleName" },
            lastName?.let { "lastName" },
            dateOfBirth?.let { "dateOfBirth" },
            gender?.let { "gender" },
            occupation?.let { "occupation" },
            address?.let { "address" },
        )
        val updated = current.copy(
            email = email ?: current.email,
            firstName = firstName ?: current.firstName,
            middleName = middleName ?: current.middleName,
            lastName = lastName ?: current.lastName,
            dateOfBirth = dateOfBirth ?: current.dateOfBirth,
            gender = gender ?: current.gender,
            occupation = occupation ?: current.occupation,
            address = address ?: current.address,
            errors = current.errors - touched.toSet(),
            focusField = null,
        )
        draft = draft.withIdentity(updated.toIdentityDraft())
        _step.value = updated
    }

    fun consumeIdentityFocus() {
        val current = _step.value as? RequestStep.Identity ?: return
        if (current.focusField == null) return
        _step.value = current.copy(focusField = null)
    }

    fun confirmIdentity() {
        val current = _step.value as? RequestStep.Identity ?: return
        val errors = validateIdentity(current)

        if (errors.isNotEmpty()) {
            _step.value = current.copy(
                errors = errors,
                focusField = identityFieldOrder.firstOrNull { errors.containsKey(it) },
            )
            return
        }

        _step.value = RequestStep.Review(
            selectedType = current.selectedType,
            identityRequired = true,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = current.reason,
            referringSource = current.referringSource.ifBlank { null },
            identity = AppointmentRequestIdentity(
                phone = current.phone.trim(),
                email = current.email.trim().ifBlank { null },
                firstName = current.firstName.trim(),
                middleName = current.middleName.trim().ifBlank { null },
                lastName = current.lastName.trim(),
                dateOfBirth = current.dateOfBirth.trim(),
                gender = current.gender,
                occupation = current.occupation.trim(),
                address = current.address.trim(),
            ),
        )
    }

    fun backToReason() {
        val current = _step.value as? RequestStep.Identity ?: return
        _step.value = RequestStep.Reason(
            selectedType = current.selectedType,
            identityRequired = true,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = current.reason,
            referringSource = current.referringSource,
            identityDraft = current.toIdentityDraft(),
        )
    }

    private fun validateIdentity(current: RequestStep.Identity): Map<String, String> {
        val errors = mutableMapOf<String, String>()
        val email = current.email.trim()
        val firstName = current.firstName.trim()
        val middleName = current.middleName.trim()
        val lastName = current.lastName.trim()
        val dateOfBirth = current.dateOfBirth.trim()
        val occupation = current.occupation.trim()
        val address = current.address.trim()

        if (current.phone.isBlank()) {
            errors["phone"] = "We couldn't read your verified phone number. Please sign in again."
        }
        when {
            email.length > maxIdentityFieldLength ->
                errors["email"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
            email.isNotBlank() && !appointmentRequestEmailPattern.matches(email) ->
                errors["email"] = "Enter an email like name@example.com, or leave this empty."
        }
        if (firstName.isBlank()) {
            errors["firstName"] = "Enter your first name."
        } else if (firstName.length > maxIdentityFieldLength) {
            errors["firstName"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
        }
        if (middleName.length > maxIdentityFieldLength) {
            errors["middleName"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
        }
        if (lastName.isBlank()) {
            errors["lastName"] = "Enter your last name."
        } else if (lastName.length > maxIdentityFieldLength) {
            errors["lastName"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
        }
        if (dateOfBirth.isBlank()) {
            errors["dateOfBirth"] = "Choose your date of birth."
        } else {
            val parsed = runCatching { LocalDate.parse(dateOfBirth) }.getOrNull()
            when {
                parsed == null -> errors["dateOfBirth"] = "Choose your date of birth."
                !parsed.isBefore(LocalDate.now(appointmentRequestZone)) ->
                    errors["dateOfBirth"] = "Date of birth must be in the past."
            }
        }
        if (current.gender == null) errors["gender"] = "Choose an option."
        if (occupation.isBlank()) {
            errors["occupation"] = "Enter your occupation."
        } else if (occupation.length > maxIdentityFieldLength) {
            errors["occupation"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
        }
        if (address.isBlank()) {
            errors["address"] = "Enter your home address."
        } else if (address.length > maxIdentityFieldLength) {
            errors["address"] = "Please shorten this to $maxIdentityFieldLength characters or fewer."
        }
        return errors
    }

    // -------------------------------------------------------------- review step

    fun backFromReview() {
        val current = _step.value as? RequestStep.Review ?: return
        if (current.isSubmitting) return
        val identity = current.identity
        if (current.identityRequired && identity != null) {
            _step.value = RequestStep.Identity(
                selectedType = current.selectedType,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource.orEmpty(),
                phone = identity.phone.orEmpty(),
                email = identity.email.orEmpty(),
                firstName = identity.firstName.orEmpty(),
                middleName = identity.middleName.orEmpty(),
                lastName = identity.lastName.orEmpty(),
                dateOfBirth = identity.dateOfBirth.orEmpty(),
                gender = identity.gender,
                occupation = identity.occupation.orEmpty(),
                address = identity.address.orEmpty(),
            )
        } else {
            _step.value = RequestStep.Reason(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource.orEmpty(),
                identityDraft = identity,
            )
        }
    }

    /** "Edit" from the review summary, jumping straight to the step that owns that content. */
    fun editFromReview(target: RequestStepId) {
        val current = _step.value as? RequestStep.Review ?: return
        if (current.isSubmitting) return
        when (target) {
            RequestStepId.TYPE -> {
                availabilityJob?.cancel()
                weekJob?.cancel()
                loadTypes(preserveSelection = current.selectedType)
            }
            RequestStepId.SCHEDULE -> restoreSchedule(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reasonDraft = current.reason,
                referringSourceDraft = current.referringSource,
                identityDraft = current.identity,
            )
            RequestStepId.REASON -> _step.value = RequestStep.Reason(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource.orEmpty(),
                identityDraft = current.identity,
            )
            RequestStepId.IDENTITY, RequestStepId.REVIEW -> backFromReview()
        }
    }

    fun submit(isFrameReservationOrigin: Boolean = false) {
        val current = _step.value as? RequestStep.Review ?: return
        if (current.isSubmitting) return
        // A stale/restored review state must never smuggle identity into a linked request.
        val identity = current.identity.takeIf { current.identityRequired }
        _step.value = current.copy(isSubmitting = true, identity = identity)
        viewModelScope.launch {
            repository.createRequest(
                appointmentTypeId = current.selectedType.id,
                scheduledAt = current.primarySlot.startsAt,
                reasonForVisit = current.reason,
                alternativeScheduledTimes = current.alternativeSlots
                    .map { it.startsAt }
                    .ifEmpty { null },
                referringSource = current.referringSource,
                identity = identity,
            ).onSuccess { request ->
                draft = RequestDraft()
                _step.value = RequestStep.Success(
                    request = request,
                    isFrameReservationOrigin = isFrameReservationOrigin,
                )
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                val code = apiError?.code
                _step.value = RequestStep.SubmissionError(
                    selectedType = current.selectedType,
                    identityRequired = current.identityRequired,
                    date = current.date,
                    primarySlot = current.primarySlot,
                    alternativeSlots = current.alternativeSlots,
                    reason = current.reason,
                    referringSource = current.referringSource,
                    identity = identity,
                    errorCode = code,
                    errorMessage = patientSafeAppointmentRequestError(
                        error = error,
                        fallback = "We couldn't send your request. Please try again.",
                    ),
                    fieldErrors = apiError?.fieldErrors.orEmpty(),
                    typeUnavailable = apiError?.isAppointmentTypeUnavailable() == true,
                    referralValidationFailure = apiError?.isReferralValidationFailure() == true,
                    // Nothing in this flow can clear an existing-request limit, so retrying here
                    // would loop. The screen offers a way out instead.
                    canRetry = code != "ACTIVE_REQUEST_LIMIT_REACHED",
                )
            }
        }
    }

    /**
     * Routes a failed submission back to the step that can actually fix it, carrying the
     * patient's drafts so nothing has to be retyped.
     */
    fun handleSubmissionError() {
        val current = _step.value as? RequestStep.SubmissionError ?: return
        if (current.typeUnavailable) {
            loadTypes(notice = "That appointment type is no longer offered. Please choose another.")
            return
        }
        if (current.referralValidationFailure) {
            _step.value = RequestStep.Reason(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource.orEmpty(),
                referringSourceError = "The clinic couldn't verify this referral. Please check it.",
                identityDraft = current.identity,
            )
            return
        }
        when (current.errorCode) {
            "IDENTITY_NOT_ALLOWED" -> _step.value = RequestStep.Review(
                selectedType = current.selectedType,
                identityRequired = false,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource,
                identity = null,
            )
            "SLOT_UNAVAILABLE" -> restoreSchedule(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = null,
                alternativeSlots = emptyList(),
                reasonDraft = current.reason,
                referringSourceDraft = current.referringSource,
                identityDraft = current.identity,
            )
            else -> _step.value = RequestStep.Review(
                selectedType = current.selectedType,
                identityRequired = current.identityRequired,
                date = current.date,
                primarySlot = current.primarySlot,
                alternativeSlots = current.alternativeSlots,
                reason = current.reason,
                referringSource = current.referringSource,
                identity = current.identity,
            )
        }
    }

    /** Leaves an unrecoverable submission error without discarding the flow's other state. */
    fun backFromSubmissionError() {
        val current = _step.value as? RequestStep.SubmissionError ?: return
        _step.value = RequestStep.Review(
            selectedType = current.selectedType,
            identityRequired = current.identityRequired,
            date = current.date,
            primarySlot = current.primarySlot,
            alternativeSlots = current.alternativeSlots,
            reason = current.reason,
            referringSource = current.referringSource,
            identity = current.identity,
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun restoreSchedule(
        selectedType: AppointmentType,
        identityRequired: Boolean,
        date: String?,
        primarySlot: AvailabilitySlot?,
        alternativeSlots: List<AvailabilitySlot>,
        reasonDraft: String,
        referringSourceDraft: String?,
        identityDraft: AppointmentRequestIdentity?,
    ) {
        val weekStart = (date?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now(appointmentRequestZone))
            .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        _step.value = RequestStep.Schedule(
            selectedType = selectedType,
            identityRequired = identityRequired,
            weekStart = weekStart,
            date = date,
            primaryDate = primarySlot?.let { date },
            primarySlot = primarySlot,
            alternativeSlots = alternativeSlots,
            reasonDraft = reasonDraft,
            referringSourceDraft = referringSourceDraft,
            identityDraft = identityDraft,
        )
        loadWeekAvailability(weekStart, selectedType.id)
        if (date != null) selectDate(date)
    }

    private fun RequestStep.Identity.toIdentityDraft() = AppointmentRequestIdentity(
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

    private companion object {
        const val draftKey = "appointment_request_draft"

        /** Field order used to pick which invalid field the screen scrolls to first. */
        val identityFieldOrder = listOf(
            "phone", "email", "firstName", "middleName", "lastName",
            "dateOfBirth", "gender", "occupation", "address",
        )
    }
}
