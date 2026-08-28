package com.eyecare.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AccountProfilePatch
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.StepUpChallenge
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AccountSecurityState {
    data object Loading : AccountSecurityState
    data class Overview(
        val account: PatientAccount? = null,
        val error: String? = null,
        val isEditingAccount: Boolean = false,
        val isSavingAccount: Boolean = false,
        val isRequestingStepUp: Boolean = false,
        val editFirstName: String = "",
        val editMiddleName: String = "",
        val editLastName: String = "",
        val editDateOfBirth: String = "",
        val accountSaveError: String? = null,
        val fieldErrors: Map<String, String> = emptyMap(),
    ) : AccountSecurityState
    data class EnterNewContact(
        val contactType: ContactType = ContactType.EMAIL,
        val contactValue: String = "",
        val error: String? = null,
    ) : AccountSecurityState
    data class StepUpOtp(
        val challenge: StepUpChallenge,
        val code: String = "",
        val error: String? = null,
        val isVerifying: Boolean = false,
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
    data class Result(
        val message: String,
        val account: PatientAccount? = null,
    ) : AccountSecurityState
    data object SignedOut : AccountSecurityState
}

sealed interface StepUpAction {
    data class AddContact(val contactType: ContactType, val contactValue: String) : StepUpAction
    data class MakePrimary(val contactId: Int) : StepUpAction
    data class RemoveContact(val contactId: Int) : StepUpAction
    data object ChangePassword : StepUpAction
    data class UpdateProfile(val draft: ProfileDraft) : StepUpAction
}

@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<AccountSecurityState>(AccountSecurityState.Loading)
    val state: StateFlow<AccountSecurityState> = _state.asStateFlow()
    private var latestAccount: PatientAccount? = null
    private var stepUpRequestJob: Job? = null

    private companion object {
        val PROFILE_FIELD_KEYS = setOf("first_name", "middle_name", "last_name", "date_of_birth")
    }

    fun loadAccount() {
        viewModelScope.launch {
            _state.value = AccountSecurityState.Loading
            val accountResult = authRepository.getMe()
            val loadedAccount = accountResult.getOrNull()
            if (loadedAccount != null) latestAccount = loadedAccount
            _state.value = AccountSecurityState.Overview(
                account = loadedAccount ?: latestAccount,
                error = accountResult.exceptionOrNull()?.message,
            )
        }
    }

    fun startAccountEditing() {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        val account = current.account ?: return
        if (current.isSavingAccount || current.isRequestingStepUp) return
        _state.value = current.copy(
            isEditingAccount = true,
            isSavingAccount = false,
            isRequestingStepUp = false,
            editFirstName = account.firstName.orEmpty(),
            editMiddleName = account.middleName.orEmpty(),
            editLastName = account.lastName.orEmpty(),
            editDateOfBirth = account.dateOfBirth.orEmpty(),
            accountSaveError = null,
            fieldErrors = emptyMap(),
        )
    }

    fun cancelAccountEditing() {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (current.isSavingAccount) return
        if (current.isRequestingStepUp) stepUpRequestJob?.cancel()
        _state.value = current.copy(
            isEditingAccount = false,
            isSavingAccount = false,
            isRequestingStepUp = false,
            accountSaveError = null,
            fieldErrors = emptyMap(),
        )
    }

    fun updateAccountFirstName(value: String) {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (!canEditAccount(current)) return
        _state.value = current.copy(editFirstName = value, accountSaveError = null, fieldErrors = current.fieldErrors - "first_name")
    }

    fun updateAccountMiddleName(value: String) {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (!canEditAccount(current)) return
        _state.value = current.copy(editMiddleName = value, accountSaveError = null, fieldErrors = current.fieldErrors - "middle_name")
    }

    fun updateAccountLastName(value: String) {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (!canEditAccount(current)) return
        _state.value = current.copy(editLastName = value, accountSaveError = null, fieldErrors = current.fieldErrors - "last_name")
    }

    fun updateAccountDateOfBirth(value: String) {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (!canEditAccount(current)) return
        _state.value = current.copy(editDateOfBirth = value, accountSaveError = null, fieldErrors = current.fieldErrors - "date_of_birth")
    }

    fun saveAccountDetails() {
        val current = _state.value as? AccountSecurityState.Overview ?: return
        if (!canEditAccount(current)) return
        val account = current.account ?: return

        val draft = ProfileDraft(
            firstName = current.editFirstName,
            middleName = current.editMiddleName,
            lastName = current.editLastName,
            dateOfBirth = current.editDateOfBirth,
        )
        val validation = AccountProfileEditor.validate(draft)
        if (!validation.isValid) {
            _state.value = current.copy(
                fieldErrors = buildMap {
                    validation.firstNameError?.let { put("first_name", it) }
                    validation.middleNameError?.let { put("middle_name", it) }
                    validation.lastNameError?.let { put("last_name", it) }
                    validation.dateOfBirthError?.let { put("date_of_birth", it) }
                },
            )
            return
        }

        val patch = AccountProfileEditor.computePatch(draft, account)
        if (patch.isEmpty()) {
            _state.value = current.copy(isEditingAccount = false)
            return
        }

        if (patch.hasDateOfBirthChange()) {
            startStepUp(StepUpAction.UpdateProfile(draft))
            return
        }

        executeProfilePatch(patch, null, draft)
    }

    private fun executeProfilePatch(patch: AccountProfilePatch, stepUpToken: String?, draft: ProfileDraft) {
        val current = _state.value as? AccountSecurityState.Overview
        _state.value = (current ?: AccountSecurityState.Overview(account = latestAccount)).copy(
            account = latestAccount ?: current?.account,
            isEditingAccount = true,
            isSavingAccount = true,
            isRequestingStepUp = false,
            editFirstName = draft.firstName,
            editMiddleName = draft.middleName,
            editLastName = draft.lastName,
            editDateOfBirth = draft.dateOfBirth,
            accountSaveError = null,
            fieldErrors = emptyMap(),
        )
        viewModelScope.launch {
            authRepository.updateAccountProfile(patch, stepUpToken)
                .onSuccess { updatedAccount ->
                    latestAccount = updatedAccount
                    _state.value = AccountSecurityState.Overview(
                        account = updatedAccount,
                    )
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    val rawFieldErrors = apiError?.fieldErrors.orEmpty()
                    val fieldErrors = rawFieldErrors
                        .filterKeys { it in PROFILE_FIELD_KEYS }
                        .mapValues { it.value.firstOrNull().orEmpty() }
                    val hasFormError = rawFieldErrors.keys.any { it !in PROFILE_FIELD_KEYS }
                    if (fieldErrors.isNotEmpty() || hasFormError) {
                        _state.value = AccountSecurityState.Overview(
                            account = latestAccount,
                            isEditingAccount = true,
                            editFirstName = draft.firstName,
                            editMiddleName = draft.middleName,
                            editLastName = draft.lastName,
                            editDateOfBirth = draft.dateOfBirth,
                            accountSaveError = if (hasFormError) {
                                apiError?.message ?: "We couldn't save your changes. Please try again."
                            } else {
                                null
                            },
                            fieldErrors = fieldErrors,
                        )
                    } else {
                        _state.value = AccountSecurityState.Overview(
                            account = latestAccount,
                            isEditingAccount = true,
                            editFirstName = draft.firstName,
                            editMiddleName = draft.middleName,
                            editLastName = draft.lastName,
                            editDateOfBirth = draft.dateOfBirth,
                            accountSaveError = apiError?.message ?: "We couldn't save your changes. Please try again.",
                        )
                    }
                }
        }
    }

    fun startAddContact() {
        _state.value = AccountSecurityState.EnterNewContact()
    }

    fun updateNewContactType(type: ContactType) {
        val current = _state.value
        if (current is AccountSecurityState.EnterNewContact) {
            _state.value = current.copy(contactType = type, contactValue = "", error = null)
        }
    }

    fun updateNewContactValue(value: String) {
        val current = _state.value
        if (current is AccountSecurityState.EnterNewContact) {
            _state.value = current.copy(contactValue = value, error = null)
        }
    }

    fun submitNewContact() {
        val current = _state.value
        if (current !is AccountSecurityState.EnterNewContact) return
        if (current.contactValue.isBlank()) {
            _state.value = current.copy(error = "Enter a value")
            return
        }
        startStepUp(StepUpAction.AddContact(current.contactType, current.contactValue.trim()))
    }

    fun startStepUp(action: StepUpAction) {
        if (action is StepUpAction.UpdateProfile) {
            val current = _state.value as? AccountSecurityState.Overview ?: return
            if (!canEditAccount(current)) return
            _state.value = current.copy(
                isRequestingStepUp = true,
                accountSaveError = null,
                fieldErrors = emptyMap(),
            )
        }

        stepUpRequestJob?.cancel()
        stepUpRequestJob = viewModelScope.launch {
            accountRepository.requestStepUpOtp()
                .onSuccess { challenge ->
                    if (action is StepUpAction.UpdateProfile) {
                        val current = _state.value as? AccountSecurityState.Overview
                        if (current?.isRequestingStepUp == true && current.isEditingAccount) {
                            _state.value = AccountSecurityState.StepUpOtp(
                                challenge = challenge,
                                pendingAction = action,
                            )
                        }
                    } else {
                        _state.value = AccountSecurityState.StepUpOtp(
                            challenge = challenge,
                            pendingAction = action,
                        )
                    }
                }
                .onFailure { error ->
                    when (action) {
                        is StepUpAction.UpdateProfile -> {
                            val current = _state.value as? AccountSecurityState.Overview
                            if (current?.isRequestingStepUp == true) {
                                _state.value = current.copy(
                                    isRequestingStepUp = false,
                                    accountSaveError = error.message ?: "Failed to send verification code",
                                )
                            }
                        }
                        else -> {
                            _state.value = AccountSecurityState.Overview(
                                account = latestAccount,
                                error = error.message ?: "Failed to send code",
                            )
                        }
                    }
                }
        }
    }

    fun updateStepUpCode(code: String) {
        val current = _state.value
        if (current is AccountSecurityState.StepUpOtp && !current.isVerifying) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyStepUp() {
        val current = _state.value
        if (current !is AccountSecurityState.StepUpOtp || current.code.length != 6 || current.isVerifying) return

        val challengeId = current.challenge.challengeId
        val pendingAction = current.pendingAction
        _state.value = current.copy(isVerifying = true, error = null)

        viewModelScope.launch {
            accountRepository.verifyStepUpOtp(challengeId, current.code)
                .onSuccess { proof ->
                    val latest = _state.value
                    if (latest is AccountSecurityState.StepUpOtp &&
                        latest.challenge.challengeId == challengeId &&
                        latest.pendingAction == pendingAction &&
                        latest.isVerifying
                    ) {
                        executeProtectedAction(proof.token, pendingAction)
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is AccountSecurityState.StepUpOtp &&
                        latest.challenge.challengeId == challengeId &&
                        latest.pendingAction == pendingAction &&
                        latest.isVerifying
                    ) {
                        _state.value = latest.copy(
                            isVerifying = false,
                            error = error.message ?: "Invalid code",
                        )
                    }
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
                    _state.value = AccountSecurityState.Result(
                        message = "Contact added successfully",
                        account = latestAccount,
                    )
                    loadAccount()
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
                _state.value = AccountSecurityState.Result(message = message, account = latestAccount)
                loadAccount()
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
        val current = _state.value
        if (current is AccountSecurityState.Overview && (current.isSavingAccount || current.isRequestingStepUp)) {
            return
        }
        if (current is AccountSecurityState.StepUpOtp && current.pendingAction is StepUpAction.UpdateProfile) {
            cancelStepUp()
        } else {
            loadAccount()
        }
    }

    fun cancelStepUp() {
        val current = _state.value as? AccountSecurityState.StepUpOtp ?: return
        when (val action = current.pendingAction) {
            is StepUpAction.UpdateProfile -> {
                _state.value = AccountSecurityState.Overview(
                    account = latestAccount,
                    isEditingAccount = true,
                    editFirstName = action.draft.firstName,
                    editMiddleName = action.draft.middleName,
                    editLastName = action.draft.lastName,
                    editDateOfBirth = action.draft.dateOfBirth,
                )
            }
            else -> loadAccount()
        }
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
                            _state.value = AccountSecurityState.Overview(
                                account = latestAccount,
                                error = error.message ?: "Failed to send code",
                            )
                        }
                }
            }
            is StepUpAction.MakePrimary -> {
                viewModelScope.launch {
                    accountRepository.makeContactPrimary(stepUpToken, action.contactId)
                        .onSuccess {
                            _state.value = AccountSecurityState.Result(
                                message = "Primary contact updated",
                                account = latestAccount,
                            )
                            loadAccount()
                        }
                        .onFailure { error ->
                            _state.value = AccountSecurityState.Overview(
                                account = latestAccount,
                                error = error.message ?: "Failed to update",
                            )
                        }
                }
            }
            is StepUpAction.RemoveContact -> {
                viewModelScope.launch {
                    accountRepository.removeContact(stepUpToken, action.contactId)
                        .onSuccess {
                            _state.value = AccountSecurityState.Result(
                                message = "Contact removed",
                                account = latestAccount,
                            )
                            loadAccount()
                        }
                        .onFailure { error ->
                            _state.value = AccountSecurityState.Overview(
                                account = latestAccount,
                                error = error.message ?: "Failed to remove",
                            )
                        }
                }
            }
            is StepUpAction.ChangePassword -> {
                _state.value = AccountSecurityState.ChangePassword(stepUpToken = stepUpToken)
            }
            is StepUpAction.UpdateProfile -> {
                val account = latestAccount ?: return
                val patch = AccountProfileEditor.computePatch(action.draft, account)
                executeProfilePatch(patch, stepUpToken, action.draft)
            }
        }
    }

    private fun canEditAccount(state: AccountSecurityState.Overview): Boolean =
        state.isEditingAccount && !state.isSavingAccount && !state.isRequestingStepUp
}
