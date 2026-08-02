package com.eyecare.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountSecurityState {
    data object Loading : AccountSecurityState
    data class Overview(
        val contacts: List<AccountContact> = emptyList(),
        val error: String? = null,
    ) : AccountSecurityState
    data class StepUpOtp(
        val challenge: StepUpChallenge,
        val code: String = "",
        val error: String? = null,
        val pendingAction: StepUpAction,
    ) : AccountSecurityState
    data class AddContactOtp(
        val stepUpToken: String,
        val contactType: ContactType,
        val contactValue: String,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
    ) : AccountSecurityState
    data class ChangePassword(
        val stepUpToken: String,
        val currentPassword: String = "",
        val newPassword: String = "",
        val confirmPassword: String = "",
        val errors: Map<String, String> = emptyMap(),
        val successMessage: String? = null,
    ) : AccountSecurityState
    data class Result(val message: String, val contacts: List<AccountContact> = emptyList()) : AccountSecurityState
    data object SignedOut : AccountSecurityState
}

sealed interface StepUpAction {
    data class AddContact(val contactType: ContactType, val contactValue: String) : StepUpAction
    data class MakePrimary(val contactId: Int) : StepUpAction
    data class RemoveContact(val contactId: Int) : StepUpAction
    data object ChangePassword : StepUpAction
}

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountSecurityState>(AccountSecurityState.Loading)
    val state: StateFlow<AccountSecurityState> = _state.asStateFlow()

    fun loadContacts() {
        viewModelScope.launch {
            _state.value = AccountSecurityState.Loading
            accountRepository.getContacts()
                .onSuccess { contacts ->
                    _state.value = AccountSecurityState.Overview(contacts = contacts)
                }
                .onFailure { error ->
                    _state.value = AccountSecurityState.Overview(error = error.message ?: "Failed to load contacts")
                }
        }
    }

    fun startStepUp(action: StepUpAction) {
        viewModelScope.launch {
            accountRepository.requestStepUpOtp()
                .onSuccess { challenge ->
                    _state.value = AccountSecurityState.StepUpOtp(
                        challenge = challenge,
                        pendingAction = action,
                    )
                }
                .onFailure { error ->
                    _state.value = AccountSecurityState.Overview(error = error.message ?: "Failed to send code")
                }
        }
    }

    fun updateStepUpCode(code: String) {
        val current = _state.value
        if (current is AccountSecurityState.StepUpOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyStepUp() {
        val current = _state.value
        if (current !is AccountSecurityState.StepUpOtp || current.code.length != 6) return

        viewModelScope.launch {
            accountRepository.verifyStepUpOtp(current.challenge.challengeId, current.code)
                .onSuccess { proof ->
                    executeProtectedAction(proof.token, current.pendingAction)
                }
                .onFailure { error ->
                    _state.value = current.copy(error = error.message ?: "Invalid code")
                }
        }
    }

    fun updateAddContactValue(value: String) {
        val current = _state.value
        if (current is AccountSecurityState.AddContactOtp) {
            _state.value = current.copy(contactValue = value, error = null)
        }
    }

    fun updateAddContactOtpCode(code: String) {
        val current = _state.value
        if (current is AccountSecurityState.AddContactOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyAddContactOtp() {
        val current = _state.value
        if (current !is AccountSecurityState.AddContactOtp || current.code.length != 6) return

        viewModelScope.launch {
            accountRepository.verifyContactOtp(current.challengeId, current.code)
                .onSuccess {
                    _state.value = AccountSecurityState.Result("Contact added successfully")
                    loadContacts()
                }
                .onFailure { error ->
                    _state.value = current.copy(error = error.message ?: "Verification failed")
                }
        }
    }

    fun updateCurrentPassword(value: String) {
        val current = _state.value
        if (current is AccountSecurityState.ChangePassword) {
            _state.value = current.copy(currentPassword = value, errors = current.errors - "current")
        }
    }

    fun updateNewPassword(value: String) {
        val current = _state.value
        if (current is AccountSecurityState.ChangePassword) {
            _state.value = current.copy(newPassword = value, errors = current.errors - "new")
        }
    }

    fun updateConfirmPassword(value: String) {
        val current = _state.value
        if (current is AccountSecurityState.ChangePassword) {
            _state.value = current.copy(confirmPassword = value, errors = current.errors - "confirm")
        }
    }

    fun submitPasswordChange() {
        val current = _state.value
        if (current !is AccountSecurityState.ChangePassword) return

        val errors = mutableMapOf<String, String>()
        if (current.currentPassword.isBlank()) errors["current"] = "Current password is required"
        if (current.newPassword.length < 12) errors["new"] = "Password must be at least 12 characters"
        if (current.newPassword != current.confirmPassword) errors["confirm"] = "Passwords do not match"
        if (errors.isNotEmpty()) {
            _state.value = current.copy(errors = errors)
            return
        }

        viewModelScope.launch {
            accountRepository.changePassword(
                stepUpToken = current.stepUpToken,
                currentPassword = current.currentPassword,
                password = current.newPassword,
                passwordConfirmation = current.confirmPassword,
            ).onSuccess { message ->
                _state.value = AccountSecurityState.Result(message)
                loadContacts()
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                _state.value = current.copy(errors = mapOf("_" to (apiError?.message ?: "Password change failed")))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logoutCurrent()
            _state.value = AccountSecurityState.SignedOut
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            authRepository.logoutAll()
            _state.value = AccountSecurityState.SignedOut
        }
    }

    fun back() {
        loadContacts()
    }

    private fun executeProtectedAction(stepUpToken: String, action: StepUpAction) {
        when (action) {
            is StepUpAction.AddContact -> {
                viewModelScope.launch {
                    val contactType = if (action.contactType == ContactType.EMAIL) "email" else "phone"
                    accountRepository.requestContactOtp(stepUpToken, contactType, action.contactValue)
                        .onSuccess { challenge ->
                            _state.value = AccountSecurityState.AddContactOtp(
                                stepUpToken = stepUpToken,
                                contactType = action.contactType,
                                contactValue = action.contactValue,
                                challengeId = challenge.challengeId,
                                expiresAt = challenge.expiresAt,
                            )
                        }
                        .onFailure { error ->
                            _state.value = AccountSecurityState.Overview(error = error.message ?: "Failed to send code")
                        }
                }
            }
            is StepUpAction.MakePrimary -> {
                viewModelScope.launch {
                    accountRepository.makeContactPrimary(stepUpToken, action.contactId)
                        .onSuccess { contacts ->
                            _state.value = AccountSecurityState.Result("Primary contact updated", contacts)
                            loadContacts()
                        }
                        .onFailure { error ->
                            _state.value = AccountSecurityState.Overview(error = error.message ?: "Failed to update")
                        }
                }
            }
            is StepUpAction.RemoveContact -> {
                viewModelScope.launch {
                    accountRepository.removeContact(stepUpToken, action.contactId)
                        .onSuccess {
                            _state.value = AccountSecurityState.Result("Contact removed")
                            loadContacts()
                        }
                        .onFailure { error ->
                            _state.value = AccountSecurityState.Overview(error = error.message ?: "Failed to remove")
                        }
                }
            }
            is StepUpAction.ChangePassword -> {
                _state.value = AccountSecurityState.ChangePassword(stepUpToken = stepUpToken)
            }
        }
    }
}
