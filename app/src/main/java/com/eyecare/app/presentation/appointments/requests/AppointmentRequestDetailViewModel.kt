package com.eyecare.app.presentation.appointments.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.repository.AppointmentRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface RequestDetailState {
    data object Loading : RequestDetailState
    data class Data(
        val request: AppointmentRequest,
        val isCancelling: Boolean = false,
        val cancelError: String? = null,
        val isLinked: Boolean = false,
    ) : RequestDetailState
    data class Error(val message: String) : RequestDetailState
    data object NotFound : RequestDetailState
}

@HiltViewModel
class AppointmentRequestDetailViewModel @Inject constructor(
    private val repository: AppointmentRequestRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<RequestDetailState>(RequestDetailState.Loading)
    val state: StateFlow<RequestDetailState> = _state.asStateFlow()

    fun load(id: Int) {
        viewModelScope.launch {
            _state.value = RequestDetailState.Loading
            repository.getRequest(id)
                .onSuccess { request ->
                    _state.value = RequestDetailState.Data(request = request)
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    when (apiError?.code) {
                        "REQUEST_NOT_OWNED" -> _state.value = RequestDetailState.NotFound
                        else -> _state.value = RequestDetailState.Error(
                            apiError?.message ?: "Failed to load request"
                        )
                    }
                }
        }
    }

    fun cancel() {
        val current = _state.value
        if (current !is RequestDetailState.Data || !current.request.status.isCancellable) return

        _state.value = current.copy(isCancelling = true, cancelError = null)
        viewModelScope.launch {
            repository.cancelRequest(current.request.id)
                .onSuccess { request ->
                    _state.value = RequestDetailState.Data(request = request)
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    when (apiError?.code) {
                        "REQUEST_NOT_CANCELLABLE" -> {
                            load(current.request.id)
                        }
                        else -> {
                            _state.value = current.copy(
                                isCancelling = false,
                                cancelError = apiError?.message ?: "Failed to cancel request",
                            )
                        }
                    }
                }
        }
    }

    fun setLinked(linked: Boolean) {
        val current = _state.value
        if (current is RequestDetailState.Data) {
            _state.value = current.copy(isLinked = linked)
        }
    }

    fun retry() {
        val current = _state.value
        if (current is RequestDetailState.Data) {
            load(current.request.id)
        }
    }
}
