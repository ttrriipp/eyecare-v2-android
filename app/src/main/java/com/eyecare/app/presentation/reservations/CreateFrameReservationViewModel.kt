package com.eyecare.app.presentation.reservations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.FrameReservationError
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

sealed interface CreateReservationUiState {
    data object LoadingAppointments : CreateReservationUiState
    data class AppointmentLoadError(val message: String) : CreateReservationUiState
    data class Ready(
        val eligibleAppointments: List<AppointmentV1>,
        val selectedAppointmentId: Int? = null,
        val isSubmitting: Boolean = false,
        val appointmentFieldError: String? = null,
        val itemFieldError: String? = null,
        val genericError: String? = null,
    ) : CreateReservationUiState
    data object NoEligibleAppointments : CreateReservationUiState
    data class Success(val reservation: FrameReservation) : CreateReservationUiState
}

@HiltViewModel(assistedFactory = CreateFrameReservationViewModel.Factory::class)
class CreateFrameReservationViewModel @AssistedInject constructor(
    private val reservationRepository: FrameReservationRepository,
    private val appointmentRepository: AppointmentV1Repository,
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
        _uiState.value = current.copy(selectedAppointmentId = appointmentId, appointmentFieldError = null, genericError = null)
    }

    fun submit() {
        val current = _uiState.value
        if (current !is CreateReservationUiState.Ready) return
        val appointmentId = current.selectedAppointmentId ?: return
        if (current.isSubmitting) return

        _uiState.value = current.copy(isSubmitting = true, appointmentFieldError = null, itemFieldError = null, genericError = null)
        viewModelScope.launch {
            reservationRepository.createReservation(
                variantIds = listOf(variantId),
                appointmentId = appointmentId,
            ).fold(
                onSuccess = { _uiState.value = CreateReservationUiState.Success(it) },
                onFailure = { error ->
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
                },
            )
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
                    _uiState.value = CreateReservationUiState.NoEligibleAppointments
                } else {
                    _uiState.value = CreateReservationUiState.Ready(eligibleAppointments = eligible)
                }
            } catch (e: Exception) {
                _uiState.value = CreateReservationUiState.AppointmentLoadError(
                    e.message ?: "Failed to load appointments"
                )
            }
        }
    }
}
