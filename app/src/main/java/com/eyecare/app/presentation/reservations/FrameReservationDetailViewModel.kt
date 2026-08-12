package com.eyecare.app.presentation.reservations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.FrameReservation
import com.eyecare.app.domain.model.canRemoveItems
import com.eyecare.app.domain.model.isCancellable
import com.eyecare.app.domain.repository.FrameReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReservationDetailUiState {
    data object Loading : ReservationDetailUiState
    data class Success(
        val reservation: FrameReservation,
        val isDeleting: Boolean = false,
        val deleteError: String? = null,
        val removingItemId: Int? = null,
        val removeItemError: String? = null,
    ) : ReservationDetailUiState

    /** The reservation is not in the patient's own list — never another patient's record. */
    data object NotFound : ReservationDetailUiState
    data class Error(val message: String) : ReservationDetailUiState

    /** Terminal: the reservation was deleted. Consume once in a LaunchedEffect and navigate back. */
    data object Deleted : ReservationDetailUiState
}

/**
 * The contract has no `GET /frame-reservations/{id}`, so detail resolves the reservation
 * from the patient's own unpaginated list rather than inventing a route.
 */
@HiltViewModel
class FrameReservationDetailViewModel @Inject constructor(
    private val repository: FrameReservationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val reservationId: Int = checkNotNull(savedStateHandle["reservationId"])

    private val _uiState = MutableStateFlow<ReservationDetailUiState>(ReservationDetailUiState.Loading)
    val uiState: StateFlow<ReservationDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun retry() = load()

    fun dismissDeleteError() {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        _uiState.value = current.copy(deleteError = null)
    }

    fun dismissRemoveItemError() {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        _uiState.value = current.copy(removeItemError = null)
    }

    fun deleteReservation() {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        if (current.isDeleting || !current.reservation.isCancellable) return

        _uiState.value = current.copy(isDeleting = true, deleteError = null)
        viewModelScope.launch {
            repository.deleteReservation(current.reservation.id).fold(
                onSuccess = {
                    _uiState.value = ReservationDetailUiState.Deleted
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        isDeleting = false,
                        deleteError = error.message ?: "Couldn't cancel this reservation. Please try again.",
                    )
                },
            )
        }
    }

    fun removeItem(itemId: Int) {
        val current = _uiState.value as? ReservationDetailUiState.Success ?: return
        if (current.removingItemId != null || !current.reservation.canRemoveItems) return

        _uiState.value = current.copy(removingItemId = itemId, removeItemError = null)
        viewModelScope.launch {
            repository.removeItem(current.reservation.id, itemId).fold(
                onSuccess = { result ->
                    if (result == null) {
                        // Last item removed — reservation deleted.
                        _uiState.value = ReservationDetailUiState.Deleted
                    } else {
                        _uiState.value = ReservationDetailUiState.Success(reservation = result)
                    }
                },
                onFailure = { error ->
                    _uiState.value = current.copy(
                        removingItemId = null,
                        removeItemError = error.message ?: "Couldn't remove this frame. Please try again.",
                    )
                },
            )
        }
    }

    private fun load() {
        _uiState.value = ReservationDetailUiState.Loading
        viewModelScope.launch {
            repository.getReservations().fold(
                onSuccess = { reservations ->
                    val match = reservations.firstOrNull { it.id == reservationId }
                    _uiState.value = if (match != null) {
                        ReservationDetailUiState.Success(match)
                    } else {
                        ReservationDetailUiState.NotFound
                    }
                },
                onFailure = {
                    _uiState.value = ReservationDetailUiState.Error(
                        it.message ?: "Failed to load reservation",
                    )
                },
            )
        }
    }
}
