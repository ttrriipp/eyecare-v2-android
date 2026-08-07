package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.VisitRating
import com.eyecare.app.domain.repository.AppointmentV1Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentListUiState {
    data object Loading : AppointmentListUiState
    data class Success(
        val appointments: List<AppointmentV1>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val ratingAppointmentId: Int? = null,
        val isSubmittingRating: Boolean = false,
        val ratingError: String? = null,
    ) : AppointmentListUiState
    data object Empty : AppointmentListUiState
    data class Error(val message: String) : AppointmentListUiState
}

@HiltViewModel
class AppointmentListViewModel @Inject constructor(
    private val repository: AppointmentV1Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentListUiState>(AppointmentListUiState.Loading)
    val uiState: StateFlow<AppointmentListUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var lastPage = 1
    private var hasActivePatientLink = true

    fun refresh(hasActivePatientLink: Boolean = this.hasActivePatientLink) {
        currentPage = 1
        load(hasActivePatientLink)
    }

    fun loadMore() {
        val state = _uiState.value
        if (state !is AppointmentListUiState.Success) return
        if (state.isLoadingMore || !state.hasMorePages) return
        currentPage++
        loadMoreInternal()
    }

    fun showRatingDialog(appointmentId: Int) {
        val current = _uiState.value
        if (current !is AppointmentListUiState.Success) return
        _uiState.value = current.copy(ratingAppointmentId = appointmentId, ratingError = null)
    }

    fun dismissRatingDialog() {
        val current = _uiState.value
        if (current !is AppointmentListUiState.Success) return
        _uiState.value = current.copy(ratingAppointmentId = null, ratingError = null)
    }

    fun submitRating(rating: Int, comment: String?) {
        val current = _uiState.value
        if (current !is AppointmentListUiState.Success) return
        val appointmentId = current.ratingAppointmentId ?: return

        if (rating < 1 || rating > 5) {
            _uiState.value = current.copy(ratingError = "Rating must be between 1 and 5")
            return
        }
        if (comment != null && comment.length > 1000) {
            _uiState.value = current.copy(ratingError = "Comment must be 1000 characters or less")
            return
        }

        _uiState.value = current.copy(isSubmittingRating = true, ratingError = null)
        viewModelScope.launch {
            repository.rateAppointment(appointmentId, rating, comment).fold(
                onSuccess = { visitRating ->
                    val updatedAppointments = current.appointments.map { appt ->
                        if (appt.id == appointmentId) appt.copy(
                            isRateable = true,
                            visitRating = visitRating,
                        ) else appt
                    }
                    _uiState.value = current.copy(
                        appointments = updatedAppointments,
                        isSubmittingRating = false,
                        ratingAppointmentId = null,
                    )
                },
                onFailure = { error ->
                    val message = when (error) {
                        is AppointmentError.NotFound -> "This appointment is no longer available."
                        is AppointmentError.ValidationError -> "This visit can't be rated yet."
                        else -> error.message ?: "Failed to submit rating"
                    }
                    _uiState.value = current.copy(
                        isSubmittingRating = false,
                        ratingError = message,
                    )
                },
            )
        }
    }

    fun load(hasActivePatientLink: Boolean = this.hasActivePatientLink) {
        this.hasActivePatientLink = hasActivePatientLink
        _uiState.value = AppointmentListUiState.Loading
        if (!hasActivePatientLink) {
            _uiState.value = AppointmentListUiState.Success(appointments = emptyList())
            return
        }

        viewModelScope.launch {
            repository.getAppointments(page = 1).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    lastPage = result.lastPage
                    if (result.data.isEmpty()) {
                        _uiState.value = AppointmentListUiState.Empty
                    } else {
                        _uiState.value = AppointmentListUiState.Success(
                            appointments = result.data.sortedByDescending {
                                runCatching { java.time.Instant.parse(it.scheduledAt) }
                                    .getOrElse { java.time.Instant.EPOCH }
                            },
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = { _uiState.value = AppointmentListUiState.Error(it.message ?: "Failed to load") },
            )
        }
    }

    private fun loadMoreInternal() {
        val current = _uiState.value as? AppointmentListUiState.Success ?: return
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        viewModelScope.launch {
            repository.getAppointments(page = currentPage).fold(
                onSuccess = { result ->
                    currentPage = result.currentPage
                    lastPage = result.lastPage
                    val all = current.appointments + result.data
                    _uiState.value = current.copy(
                        appointments = all.sortedByDescending {
                            runCatching { java.time.Instant.parse(it.scheduledAt) }
                                .getOrElse { java.time.Instant.EPOCH }
                        },
                        isLoadingMore = false,
                        hasMorePages = result.hasMorePages,
                    )
                },
                onFailure = {
                    _uiState.value = current.copy(
                        isLoadingMore = false,
                        loadMoreError = it.message ?: "Failed to load more",
                    )
                },
            )
        }
    }
}
