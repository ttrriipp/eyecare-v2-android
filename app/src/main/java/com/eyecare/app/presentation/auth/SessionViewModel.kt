package com.eyecare.app.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _state = MutableStateFlow<SessionState>(SessionState.Checking)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private var sessionJob: Job? = null
    private var sessionGeneration = 0L

    init {
        resolveSession()
    }

    fun resolveSession() {
        val generation = ++sessionGeneration
        sessionJob?.cancel()
        val token = tokenManager.getToken()
        if (token == null) {
            _state.value = SessionState.Unauthenticated
            return
        }

        sessionJob = viewModelScope.launch {
            _state.value = SessionState.Checking
            authRepository.getMe()
                .onSuccess { account ->
                    if (generation != sessionGeneration) return@onSuccess
                    _state.value = stateFor(account)
                }
                .onFailure { error ->
                    if (generation != sessionGeneration) return@onFailure
                    val apiError = error as? ApiDomainError
                    val message = if (apiError?.httpStatus == 429) {
                        "Too many requests. Please wait before trying again."
                    } else {
                        error.message ?: "Something went wrong"
                    }
                    if (message.contains("401") || apiError?.httpStatus == 401) {
                        tokenManager.clearToken()
                        _state.value = SessionState.Unauthenticated
                    } else {
                        _state.value = SessionState.TransientFailure(message)
                    }
                }
        }
    }

    /**
     * Re-checks the account while the app returns to the foreground.
     *
     * Linking can happen from the admin portal while this app stays open, so the existing
     * session state cannot be treated as permanent. This refresh deliberately leaves the
     * current state visible while the request is in flight and ignores transient failures.
     */
    fun refreshSession() {
        if (_state.value !is SessionState.Linked && _state.value !is SessionState.Limited) return

        val generation = ++sessionGeneration
        sessionJob?.cancel()
        val token = tokenManager.getToken()
        if (token == null) {
            _state.value = SessionState.Unauthenticated
            return
        }

        sessionJob = viewModelScope.launch {
            authRepository.getMe()
                .onSuccess { account ->
                    if (generation != sessionGeneration) return@onSuccess
                    _state.value = stateFor(account)
                }
                .onFailure { error ->
                    if (generation != sessionGeneration) return@onFailure
                    val apiError = error as? ApiDomainError
                    if (error.message?.contains("401") == true || apiError?.httpStatus == 401) {
                        tokenManager.clearToken()
                        _state.value = SessionState.Unauthenticated
                    }
                    // Keep the current authenticated state for transient refresh failures.
                }
        }
    }

    fun setLinkedAccount(account: PatientAccount) {
        if (account.linkStatus == PatientLinkStatus.LINKED) {
            sessionGeneration++
            sessionJob?.cancel()
            sessionJob = null
            _state.value = SessionState.Linked(account)
        }
    }

    fun adoptAccount(account: PatientAccount) {
        sessionGeneration++
        sessionJob?.cancel()
        sessionJob = null
        _state.value = when (account.linkStatus) {
            PatientLinkStatus.LINKED -> SessionState.Linked(account)
            PatientLinkStatus.UNLINKED,
            PatientLinkStatus.PENDING_REVIEW,
            PatientLinkStatus.UNKNOWN -> SessionState.Limited(account)
        }
    }

    fun signOut() {
        sessionGeneration++
        sessionJob?.cancel()
        sessionJob = null
        tokenManager.clearToken()
        _state.value = SessionState.Unauthenticated
    }

    private fun stateFor(account: PatientAccount): SessionState = when (account.linkStatus) {
        PatientLinkStatus.LINKED -> SessionState.Linked(account)
        PatientLinkStatus.UNLINKED,
        PatientLinkStatus.PENDING_REVIEW,
        PatientLinkStatus.UNKNOWN -> SessionState.Limited(account)
    }
}
