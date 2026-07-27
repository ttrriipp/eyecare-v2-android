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
        val editFullName: String = "",
        val editDateOfBirth: String = "",
        val editOccupation: String = "",
        val editAddress: String = "",
        val editGender: String = "",
        val editContactEmail: String = "",
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
            val u = current.user
            _uiState.value = current.copy(
                isEditing = true,
                editName = u.name,
                editEmail = u.email,
                editPhone = u.phone ?: "",
                editFullName = u.fullName ?: "",
                editDateOfBirth = u.dateOfBirth ?: "",
                editOccupation = u.occupation ?: "",
                editAddress = u.address ?: "",
                editGender = u.gender ?: "",
                editContactEmail = u.contactEmail ?: "",
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

    fun updateFullName(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editFullName = value, saveError = null)
        }
    }

    fun updateDateOfBirth(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editDateOfBirth = value, saveError = null)
        }
    }

    fun updateOccupation(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editOccupation = value, saveError = null)
        }
    }

    fun updateAddress(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editAddress = value, saveError = null)
        }
    }

    fun updateGender(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editGender = value, saveError = null)
        }
    }

    fun updateContactEmail(value: String) {
        val current = _uiState.value
        if (current is ProfileUiState.Success) {
            _uiState.value = current.copy(editContactEmail = value, saveError = null)
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
            authRepository.updateMe(
                UpdateProfileRequest(
                    name = current.editName,
                    email = current.editEmail,
                    phone = current.editPhone.ifBlank { null },
                    fullName = current.editFullName.ifBlank { null },
                    dateOfBirth = current.editDateOfBirth.ifBlank { null },
                    occupation = current.editOccupation.ifBlank { null },
                    address = current.editAddress.ifBlank { null },
                    gender = current.editGender.ifBlank { null },
                    contactEmail = current.editContactEmail.ifBlank { null },
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
    fullName: String = user.fullName ?: "",
    dateOfBirth: String = user.dateOfBirth ?: "",
    occupation: String = user.occupation ?: "",
    address: String = user.address ?: "",
    gender: String = user.gender ?: "",
    contactEmail: String = user.contactEmail ?: "",
): Boolean {
    val originalPhone = user.phone?.takeIf { it.isNotBlank() }
    val editedPhone = phone.takeIf { it.isNotBlank() }
    return name != user.name ||
        email != user.email ||
        editedPhone != originalPhone ||
        fullName != (user.fullName ?: "") ||
        dateOfBirth != (user.dateOfBirth ?: "") ||
        occupation != (user.occupation ?: "") ||
        address != (user.address ?: "") ||
        gender != (user.gender ?: "") ||
        contactEmail != (user.contactEmail ?: "")
}
