package com.eyecare.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.data.local.TokenManager
import com.eyecare.app.domain.model.AuthError
import com.eyecare.app.domain.model.User
import com.eyecare.app.domain.repository.AuthRepository
import com.eyecare.app.domain.repository.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(
        val user: User,
        val isEditing: Boolean = false,
        val isSaving: Boolean = false,
        val editName: String = "",
        val editEmail: String = "",
        val editPhone: String = "",
        val fieldErrors: Map<String, List<String>> = emptyMap(),
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

    init { load() }

    fun retry() = load()

    fun startEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(
                isEditing = true,
                editName = current.user.name,
                editEmail = current.user.email,
                editPhone = current.user.phone ?: "",
                fieldErrors = emptyMap(),
                saveError = null,
                saveSuccess = false,
            )
        }
    }

    fun cancelEditing() {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(
                isEditing = false,
                fieldErrors = emptyMap(),
                saveError = null,
            )
        }
    }

    fun updateName(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editName = value, saveError = null)
        }
    }

    fun updateEmail(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editEmail = value, saveError = null)
        }
    }

    fun updatePhone(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editPhone = value, saveError = null)
        }
    }

    fun saveProfile() {
        val current = _uiState.value
        if (current !is ProfileUiState.Success) return
        _uiState.value = current.copy(
            isSaving = true,
            fieldErrors = emptyMap(),
            saveError = null,
            saveSuccess = false,
        )
        viewModelScope.launch {
            val phone = current.editPhone.ifBlank { null }
            authRepository.updateMe(
                UpdateProfileRequest(
                    name = current.editName,
                    email = current.editEmail,
                    phone = phone,
                ),
            ).fold(
                onSuccess = { user ->
                    _uiState.value = ProfileUiState.Success(
                        user = user,
                        isEditing = false,
                        saveSuccess = true,
                    )
                },
                onFailure = { error ->
                    if (error is AuthError.ValidationError) {
                        _uiState.value = current.copy(
                            isSaving = false,
                            fieldErrors = error.fieldErrors,
                            saveError = null,
                        )
                    } else {
                        _uiState.value = current.copy(
                            isSaving = false,
                            saveError = "We couldn't save your changes. Please try again.",
                        )
                    }
                },
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.value = authRepository.getMe().fold(
                onSuccess = { ProfileUiState.Success(it) },
                onFailure = { ProfileUiState.Error(it.message ?: "Failed to load profile") },
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            tokenManager.clearToken()
            _loggedOut.value = true
        }
    }
}

internal fun hasProfileChanges(
    user: User,
    name: String,
    email: String,
    phone: String,
): Boolean {
    val originalPhone = user.phone?.takeIf { it.isNotBlank() }
    val editedPhone = phone.takeIf { it.isNotBlank() }
    return name != user.name || email != user.email || editedPhone != originalPhone
}
