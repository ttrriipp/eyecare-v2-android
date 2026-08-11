package com.eyecare.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val account: PatientAccount,
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val editFirstName: String = "",
        val editLastName: String = "",
        val saveError: String? = null,
        val saveSuccess: Boolean = false,
    ) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    private var loadJob: Job? = null

    init { load() }

    fun retry() = load()

    /**
     * Uses the account already resolved by the session flow while the profile request catches up.
     * This matters immediately after invitation linking, when Profile can resume before a fresh
     * GET /me response is available.
     */
    fun adoptAccount(account: PatientAccount) {
        val current = _uiState.value
        if (current is ProfileUiState.Success && current.account == account) return

        loadJob?.cancel()
        _uiState.value = ProfileUiState.Success(account = account)
    }

    fun startEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            val a = current.account
            _uiState.value = current.copy(
                isEditing = true,
                editFirstName = a.firstName ?: "",
                editLastName = a.lastName ?: "",
                saveError = null,
                saveSuccess = false,
            )
        }
    }

    fun cancelEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(isEditing = false, saveError = null)
        }
    }

    fun updateFirstName(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editFirstName = value, saveError = null)
        }
    }

    fun updateLastName(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editLastName = value, saveError = null)
        }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current !is ProfileUiState.Success) return

        val firstName = current.editFirstName.trim()
        val lastName = current.editLastName.trim()
        if (firstName.isBlank() || lastName.isBlank()) {
            _uiState.value = current.copy(saveError = "First and last name are required")
            return
        }

        viewModelScope.launch {
            _uiState.value = current.copy(isSaving = true, saveError = null)
            authRepository.updateAccountName(firstName, lastName)
                .onSuccess { account ->
                    _uiState.value = ProfileUiState.Success(account = account, saveSuccess = true)
                }
                .onFailure { error ->
                    _uiState.value = current.copy(
                        isSaving = false,
                        saveError = "We couldn't save your changes. Please try again.",
                    )
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logoutCurrent()
            tokenManager.clearToken()
            _loggedOut.value = true
        }
    }

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            // Only show the loading placeholder when there's nothing on screen yet —
            // a resume-triggered refresh should update in place, not blink back to Loading.
            if (_uiState.value !is ProfileUiState.Success) {
                _uiState.value = ProfileUiState.Loading
            }
            authRepository.getMe()
                .onSuccess { account ->
                    _uiState.value = ProfileUiState.Success(account = account)
                }
                .onFailure { error ->
                    val current = _uiState.value
                    if (current !is ProfileUiState.Success) {
                        _uiState.value = ProfileUiState.Error(error.message ?: "Failed to load profile")
                    }
                }
        }
    }
}
