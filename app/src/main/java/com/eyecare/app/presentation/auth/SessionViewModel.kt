package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        resolveSession()
    }

    fun resolveSession() {
        val token = tokenManager.getToken()
        if (token == null) {
            _state.value = SessionState.Unauthenticated
            return
        }

        viewModelScope.launch {
            _state.value = SessionState.Checking
            authRepository.getMe()
                .onSuccess { account ->
                    _state.value = when (account.linkStatus) {
                        PatientLinkStatus.LINKED -> SessionState.Linked(account)
                        PatientLinkStatus.UNLINKED,
                        PatientLinkStatus.PENDING_REVIEW,
                        PatientLinkStatus.UNKNOWN -> SessionState.Limited(account)
                    }
                }
                .onFailure { error ->
                    val message = error.message ?: "Something went wrong"
                    if (message.contains("401") || error is com.eyecare.app.domain.model.ApiDomainError && error.httpStatus == 401) {
                        tokenManager.clearToken()
                        _state.value = SessionState.Unauthenticated
                    } else {
                        _state.value = SessionState.TransientFailure(message)
                    }
                }
        }
    }

    fun signOut() {
        tokenManager.clearToken()
        _state.value = SessionState.Unauthenticated
    }
}
