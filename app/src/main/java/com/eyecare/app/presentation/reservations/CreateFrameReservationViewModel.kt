package com.eyecare.app.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationError
import com.eyecare.app.domain.model.MAX_RESERVATION_ITEMS
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.OffsetDateTime

sealed interface CreateReservationUiState {
    data object LoadingAppointments : CreateReservationUiState
    data class AppointmentLoadError(val message: String) : CreateReservationUiState
    data class Ready(
        val eligibleAppointments: List<AppointmentV1>,
        val selectedAppointmentId: Int? = null,
        val existingReservationsByAppointment: Map<Int, FrameReservation> = emptyMap(),
        val isSubmitting: Boolean = false,
        val appointmentFieldError: String? = null,
        val itemFieldError: String? = null,
        val genericError: String? = null,
    ) : CreateReservationUiState
    data object NoEligibleAppointments : CreateReservationUiState
    /**
     * No scheduled visit yet, but the patient has at least one PENDING appointment request —
     * reservations aren't available until the clinic confirms it into a scheduled visit, so
     * this replaces the generic "book an appointment" prompt with a pointer to the existing
     * request instead of inviting a duplicate one.
     */
    data class RequestPending(val primaryRequestId: Int, val pendingCount: Int) : CreateReservationUiState
    /**
     * No scheduled visit and no pending request, but the patient's most recent request ended
     * without one (rejected/cancelled/expired) — show that outcome rather than silently
     * falling back to the generic "no appointment on file" copy.
     */
    data class PriorRequestStatus(val status: AppointmentRequestStatus) : CreateReservationUiState
    data class Success(val reservation: FrameReservation) : CreateReservationUiState
}

/** An appointment may have at most one active (unheld) reservation. */

/**
 * Whether reserving [variantId] for an appointment that already has [existingReservation]
 * should create a new reservation, fold into the existing one, or be refused outright.
 */
internal sealed interface MergeOutcome {
    data object None : MergeOutcome
    data class AlreadyReserved(val reservation: FrameReservation) : MergeOutcome
    data class Full(val reservation: FrameReservation) : MergeOutcome
    data class Mergeable(val reservation: FrameReservation) : MergeOutcome
    data class Blocked(val reservation: FrameReservation) : MergeOutcome
}

internal fun mergeOutcome(existingReservation: FrameReservation?, variantId: Int): MergeOutcome {
    if (existingReservation == null) return MergeOutcome.None
    if (existingReservation.items.any { it.productVariantId == variantId }) {
        return MergeOutcome.AlreadyReserved(existingReservation)
    }
    // Once the clinic has pulled frames (isHeld), the app cannot modify the reservation.
    if (existingReservation.isHeld) {
        return MergeOutcome.Blocked(existingReservation)
    }
    if (existingReservation.items.size >= MAX_RESERVATION_ITEMS) {
        return MergeOutcome.Full(existingReservation)
    }
    return MergeOutcome.Mergeable(existingReservation)
}

@HiltViewModel(assistedFactory = CreateFrameReservationViewModel.Factory::class)
class CreateFrameReservationViewModel @AssistedInject constructor(
    private val reservationRepository: FrameReservationRepository,
    private val appointmentRepository: AppointmentV1Repository,
    private val appointmentRequestRepository: AppointmentRequestRepository,
    @Assisted("frameId") val frameId: Int,
    @Assisted("variantId") val variantId: Int,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(
            @Assisted("frameId") frameId: Int,
            @Assisted("variantId") variantId: Int,
        ): CreateFrameReservationViewModel
    }

    private val _uiState = MutableStateFlow<CreateReservationUiState>(CreateReservationUiState.LoadingAppointments)
    val uiState: StateFlow<CreateReservationUiState> = _uiState.asStateFlow()

    init { loadEligibleAppointments() }

    fun retryLoadAppointments() = loadEligibleAppointments()

    fun selectAppointment(appointmentId: Int) {
        val current = _uiState.value
        if (current !is CreateReservationUiState.Ready) return
        _uiState.value = current.copy(
            selectedAppointmentId = appointmentId,
            appointmentFieldError = null,
            itemFieldError = null,
            genericError = null,
        )
    }

    fun submit() {
        val current = _uiState.value
        if (current !is CreateReservationUiState.Ready) return
        val appointmentId = current.selectedAppointmentId ?: return
        if (current.isSubmitting) return

        when (val outcome = mergeOutcome(current.existingReservationsByAppointment[appointmentId], variantId)) {
            is MergeOutcome.Blocked -> _uiState.value = current.copy(
                appointmentFieldError = "The clinic is already handling this reservation " +
                    "(${reservationChipLabel(outcome.reservation.isHeld).lowercase()}) — you can't add " +
                    "frames to it from the app. Ask the clinic at your visit.",
            )
            is MergeOutcome.AlreadyReserved -> _uiState.value = current.copy(
                itemFieldError = "This frame is already part of your reservation for this appointment.",
            )
            is MergeOutcome.Full -> _uiState.value = current.copy(
                itemFieldError = "This reservation already has the maximum of $MAX_RESERVATION_ITEMS frames.",
            )
            is MergeOutcome.Mergeable -> mergeIntoExisting(current, outcome.reservation, appointmentId)
            MergeOutcome.None -> createNew(current, appointmentId)
        }
    }

    private fun createNew(current: CreateReservationUiState.Ready, appointmentId: Int) {
        _uiState.value = current.copy(isSubmitting = true, appointmentFieldError = null, itemFieldError = null, genericError = null)
        viewModelScope.launch {
            reservationRepository.createReservation(
                variantIds = listOf(variantId),
                appointmentId = appointmentId,
            ).fold(
                onSuccess = { _uiState.value = CreateReservationUiState.Success(it) },
                onFailure = { error -> handleCreateFailure(current, error) },
            )
        }
    }

    /**
     * One add-item call replaces the old cancel-then-recreate dance. If the clinic has
     * already pulled the frames (isHeld), mergeOutcome returns Blocked and this is never reached.
     */
    private fun mergeIntoExisting(
        current: CreateReservationUiState.Ready,
        existingReservation: FrameReservation,
        appointmentId: Int,
    ) {
        _uiState.value = current.copy(isSubmitting = true, appointmentFieldError = null, itemFieldError = null, genericError = null)
        viewModelScope.launch {
            reservationRepository.addItem(existingReservation.id, variantId).fold(
                onSuccess = { _uiState.value = CreateReservationUiState.Success(it) },
                onFailure = { error -> handleCreateFailure(current, error) },
            )
        }
    }

    private fun handleCreateFailure(current: CreateReservationUiState.Ready, error: Throwable) {
        when (error) {
            is FrameReservationError.ValidationError -> {
                val appointmentError = error.fieldErrors["appointment_id"]?.firstOrNull()
                val itemError = error.fieldErrors["items"]?.firstOrNull()
                    ?: error.fieldErrors["items.0.product_variant_id"]?.firstOrNull()

                if (appointmentError != null) {
                    // Invalid appointment — clear selection and reload
                    _uiState.value = current.copy(
                        isSubmitting = false,
                        selectedAppointmentId = null,
                        appointmentFieldError = appointmentError,
                    )
                    loadEligibleAppointments()
                } else if (itemError != null) {
                    // Invalid item — keep appointment selection
                    _uiState.value = current.copy(
                        isSubmitting = false,
                        itemFieldError = itemError,
                    )
                } else {
                    _uiState.value = current.copy(
                        isSubmitting = false,
                        genericError = error.fieldErrors.values.flatten().firstOrNull() ?: "Validation failed",
                    )
                }
            }
            else -> {
                _uiState.value = current.copy(
                    isSubmitting = false,
                    genericError = error.message ?: "Failed to create reservation",
                )
            }
        }
    }

    fun refreshAfterBooking() {
        loadEligibleAppointments()
    }

    private fun loadEligibleAppointments() {
        _uiState.value = CreateReservationUiState.LoadingAppointments
        viewModelScope.launch {
            val allAppointments = mutableListOf<AppointmentV1>()
            var page = 1
            var lastPage = 1

            try {
                do {
                    val result = appointmentRepository.getAppointments(page = page)
                    result.fold(
                        onSuccess = { paginatedResult ->
                            allAppointments.addAll(paginatedResult.data)
                            lastPage = paginatedResult.lastPage
                            page++
                        },
                        onFailure = {
                            _uiState.value = CreateReservationUiState.AppointmentLoadError(
                                it.message ?: "Failed to load appointments"
                            )
                            return@launch
                        },
                    )
                } while (page <= lastPage)

                val now = Instant.now()
                val eligible = allAppointments
                    .distinctBy { it.id }
                    .filter { isReservationEligible(it, now) }
                    .sortedBy { it.scheduledAt }

                if (eligible.isEmpty()) {
                    _uiState.value = resolveNoEligibleState()
                } else {
                    // Best-effort: if this fails, the picker just falls back to the old
                    // behavior of discovering a conflict only when the create call rejects it.
                    val existingByAppointment = reservationRepository.getReservations()
                        .getOrDefault(emptyList())
                        .filter { !it.isHeld }
                        .associateBy { it.appointment.id }
                    _uiState.value = CreateReservationUiState.Ready(
                        eligibleAppointments = eligible,
                        existingReservationsByAppointment = existingByAppointment,
                    )
                }
            } catch (e: Exception) {
                _uiState.value = CreateReservationUiState.AppointmentLoadError(
                    e.message ?: "Failed to load appointments"
                )
            }
        }
    }

    /**
     * A patient with no eligible scheduled visit may still have an appointment request in
     * flight (or one that recently ended without a visit) - reflect that instead of the
     * generic "book an appointment" prompt, which would otherwise invite a duplicate request.
     * Best-effort: any failure to load requests falls back to the generic empty state, the
     * same degradation the appointment-load path already uses for reservation lookups.
     */
    private suspend fun resolveNoEligibleState(): CreateReservationUiState {
        val requests = appointmentRequestRepository.getRequests(page = 1)
            .getOrNull()?.data.orEmpty()

        val pending = requests.filter { it.status == AppointmentRequestStatus.PENDING }
        if (pending.isNotEmpty()) {
            val primary = pending.minByOrNull { parseRequestInstant(it.scheduledAt) } ?: pending.first()
            return CreateReservationUiState.RequestPending(
                primaryRequestId = primary.id,
                pendingCount = pending.size,
            )
        }

        val unresolvedStatuses = setOf(
            AppointmentRequestStatus.REJECTED,
            AppointmentRequestStatus.CANCELLED,
            AppointmentRequestStatus.EXPIRED,
            AppointmentRequestStatus.UNKNOWN,
        )
        val mostRecentUnresolved = requests
            .filter { it.status in unresolvedStatuses }
            .maxByOrNull { parseRequestInstant(it.createdAt) }
        if (mostRecentUnresolved != null) {
            return CreateReservationUiState.PriorRequestStatus(mostRecentUnresolved.status)
        }

        return CreateReservationUiState.NoEligibleAppointments
    }
}

private fun parseRequestInstant(value: String): Instant =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrElse { Instant.EPOCH }
