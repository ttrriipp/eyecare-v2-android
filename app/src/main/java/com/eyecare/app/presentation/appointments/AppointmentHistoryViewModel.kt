package com.eyecare.app.presentation.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AppointmentError
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.repository.AppointmentV1Repository
import com.eyecare.app.domain.repository.PaginatedResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AppointmentHistoryUiState {
    data object Loading : AppointmentHistoryUiState

    data class Content(
        val appointments: List<AppointmentV1>,
        val isLoadingMore: Boolean = false,
        val hasMorePages: Boolean = false,
        val loadMoreError: String? = null,
        val isRefreshing: Boolean = false,
        val refreshError: String? = null,
        val ratingAppointmentId: Int? = null,
        val isSubmittingRating: Boolean = false,
        val ratingError: String? = null,
    ) : AppointmentHistoryUiState

    data object Empty : AppointmentHistoryUiState

    data class Error(val message: String) : AppointmentHistoryUiState
}

@HiltViewModel
class AppointmentHistoryViewModel @Inject constructor(
    private val appointmentRepository: AppointmentV1Repository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppointmentHistoryUiState>(AppointmentHistoryUiState.Loading)
    val uiState: StateFlow<AppointmentHistoryUiState> = _uiState.asStateFlow()

    private var currentPage = 1
    private var requestGeneration = 0L
    private var requestJob: Job? = null

    fun load() {
        requestJob?.cancel()
        val generation = ++requestGeneration
        currentPage = 1
        _uiState.value = AppointmentHistoryUiState.Loading
        requestJob = viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = 1).fold(
                onSuccess = { result ->
                    if (generation == requestGeneration) updateFromResult(result)
                },
                onFailure = { error ->
                    if (generation == requestGeneration) {
                        _uiState.value = AppointmentHistoryUiState.Error(message = historySafeError(error))
                    }
                },
            )
        }
    }

    fun refresh() {
        val current = _uiState.value
        if (current !is AppointmentHistoryUiState.Content) {
            load()
            return
        }
        requestJob?.cancel()
        val generation = ++requestGeneration
        _uiState.value = current.copy(
            isRefreshing = true,
            refreshError = null,
            loadMoreError = null,
        )
        requestJob = viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = 1).fold(
                onSuccess = { result ->
                    if (generation == requestGeneration) updateFromResult(result)
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (generation == requestGeneration && latest is AppointmentHistoryUiState.Content) {
                        _uiState.value = latest.copy(
                            isRefreshing = false,
                            refreshError = historySafeError(error),
                        )
                    }
                },
            )
        }
    }

    fun loadMore() {
        val current = _uiState.value
        if (current !is AppointmentHistoryUiState.Content ||
            current.isLoadingMore ||
            current.isRefreshing ||
            !current.hasMorePages
        ) return

        val nextPage = currentPage + 1
        requestJob?.cancel()
        val generation = ++requestGeneration
        _uiState.value = current.copy(isLoadingMore = true, loadMoreError = null)
        requestJob = viewModelScope.launch {
            appointmentRepository.getAppointmentHistory(page = nextPage).fold(
                onSuccess = { result ->
                    val latest = _uiState.value
                    if (generation == requestGeneration && latest is AppointmentHistoryUiState.Content) {
                        currentPage = result.currentPage
                        _uiState.value = latest.copy(
                            appointments = (latest.appointments + result.data).distinctBy { it.id },
                            isLoadingMore = false,
                            hasMorePages = result.hasMorePages,
                        )
                    }
                },
                onFailure = { error ->
                    val latest = _uiState.value
                    if (generation == requestGeneration && latest is AppointmentHistoryUiState.Content) {
                        _uiState.value = latest.copy(
                            isLoadingMore = false,
                            loadMoreError = historySafeError(error),
                        )
                    }
                },
            )
        }
    }

    fun showRatingDialog(appointmentId: Int) {
        val current = _uiState.value
        if (current !is AppointmentHistoryUiState.Content || current.isSubmittingRating) return
        val appointment = current.appointments.find { it.id == appointmentId } ?: return
        if (!appointment.isRateable) return
        _uiState.value = current.copy(
            ratingAppointmentId = appointmentId,
            ratingError = null,
        )
    }

    fun dismissRatingDialog() {
        val current = _uiState.value
        if (current !is AppointmentHistoryUiState.Content || current.isSubmittingRating) return
        _uiState.value = current.copy(
            ratingAppointmentId = null,
            ratingError = null,
        )
    }

    fun submitRating(rating: Int, comment: String?) {
        val current = _uiState.value
        val appointmentId = (current as? AppointmentHistoryUiState.Content)?.ratingAppointmentId
            ?: return
        if (current.isSubmittingRating) return

        if (rating !in 1..5) {
            _uiState.value = current.copy(ratingError = "Choose a rating from 1 to 5 stars.")
            return
        }
        if (comment != null && comment.length > 1000) {
            _uiState.value = current.copy(
                ratingError = "Keep your comment to 1,000 characters or less.",
            )
            return
        }

        _uiState.value = current.copy(
            isSubmittingRating = true,
            ratingError = null,
        )
        viewModelScope.launch {
            appointmentRepository.rateAppointment(appointmentId, rating, comment).fold(
                onSuccess = { visitRating ->
                    val latest = _uiState.value as? AppointmentHistoryUiState.Content ?: return@fold
                    _uiState.value = latest.copy(
                        appointments = latest.appointments.map { appointment ->
                            if (appointment.id == appointmentId) {
                                appointment.copy(isRateable = true, visitRating = visitRating)
                            } else {
                                appointment
                            }
                        },
                        ratingAppointmentId = null,
                        isSubmittingRating = false,
                        ratingError = null,
                    )
                },
                onFailure = { error ->
                    val latest = _uiState.value as? AppointmentHistoryUiState.Content ?: return@fold
                    _uiState.value = latest.copy(
                        isSubmittingRating = false,
                        ratingError = historyRatingError(error),
                    )
                },
            )
        }
    }

    fun retry() {
        load()
    }

    fun clearRefreshError() {
        val current = _uiState.value
        if (current is AppointmentHistoryUiState.Content) {
            _uiState.value = current.copy(refreshError = null)
        }
    }

    fun clearLoadMoreError() {
        val current = _uiState.value
        if (current is AppointmentHistoryUiState.Content) {
            _uiState.value = current.copy(loadMoreError = null)
        }
    }

    private fun updateFromResult(result: PaginatedResult<AppointmentV1>) {
        currentPage = result.currentPage
        if (result.data.isEmpty()) {
            _uiState.value = AppointmentHistoryUiState.Empty
        } else {
            _uiState.value = AppointmentHistoryUiState.Content(
                appointments = result.data,
                hasMorePages = result.hasMorePages,
            )
        }
    }
}

private fun historySafeError(@Suppress("UNUSED_PARAMETER") error: Throwable): String =
    "Something went wrong. Please try again."

private fun historyRatingError(error: Throwable): String = when (error) {
    is AppointmentError.NotFound -> "This appointment is no longer available."
    is AppointmentError.ValidationError -> "This visit can't be rated yet."
    else -> "We couldn't submit your rating. Try again."
}
