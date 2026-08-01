package com.eyecare.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LimitedAccountState {
    data class Overview(
        val account: PatientAccount,
        val linkState: LinkState? = null,
    ) : LimitedAccountState
    data class EnterInvitationCode(
        val account: PatientAccount,
        val code: String = "",
        val error: String? = null,
    ) : LimitedAccountState
    data class VerifyInvitationOtp(
        val account: PatientAccount,
        val invitationCode: String,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
    ) : LimitedAccountState
    data class Linked(val account: PatientAccount) : LimitedAccountState
    data class Error(val message: String) : LimitedAccountState
}

@HiltViewModel
class LimitedAccountViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<LimitedAccountState>(LimitedAccountState.Overview(
        account = PatientAccount(
            id = 0, name = "", firstName = null, middleName = null, lastName = null,
            email = null, phone = null, role = "patient", dateOfBirth = null,
            linkStatus = PatientLinkStatus.UNLINKED, privacyPolicyVersion = null, privacyAcceptedAt = null, linkedPatient = null,
        )
    ))
    val state: StateFlow<LimitedAccountState> = _state.asStateFlow()

    fun load(account: PatientAccount) {
        _state.value = LimitedAccountState.Overview(account = account)
        refreshLinkState()
    }

    fun startInvitationEntry() {
        val current = _state.value
        val account = when (current) {
            is LimitedAccountState.Overview -> current.account
            else -> return
        }
        _state.value = LimitedAccountState.EnterInvitationCode(account = account)
    }

    fun updateInvitationCode(code: String) {
        val current = _state.value
        if (current is LimitedAccountState.EnterInvitationCode) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun requestInvitationOtp() {
        val current = _state.value
        if (current !is LimitedAccountState.EnterInvitationCode || current.code.isBlank()) return

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            accountRepository.requestInvitationOtp(current.code)
                .onSuccess { challenge ->
                    _state.value = LimitedAccountState.VerifyInvitationOtp(
                        account = current.account,
                        invitationCode = current.code,
                        challengeId = challenge.challengeId,
                        expiresAt = challenge.expiresAt,
                    )
                }
                .onFailure { error ->
                    val apiError = error as? ApiDomainError
                    _state.value = current.copy(error = apiError?.message ?: "Invalid invitation code")
                }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (current is LimitedAccountState.VerifyInvitationOtp) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyInvitationOtp() {
        val current = _state.value
        if (current !is LimitedAccountState.VerifyInvitationOtp || current.code.length != 6) return

        viewModelScope.launch {
            _state.value = current.copy(error = null)
            accountRepository.acceptInvitation(
                invitationCode = current.invitationCode,
                challengeId = current.challengeId,
                code = current.code,
            ).onSuccess { linkState ->
                if (linkState is LinkState.Linked) {
                    refreshMeForLinked()
                } else {
                    _state.value = LimitedAccountState.Overview(
                        account = current.account,
                        linkState = linkState,
                    )
                }
            }.onFailure { error ->
                val apiError = error as? ApiDomainError
                _state.value = current.copy(error = apiError?.message ?: "Verification failed")
            }
        }
    }

    fun back() {
        _state.value = when (val current = _state.value) {
            is LimitedAccountState.EnterInvitationCode -> LimitedAccountState.Overview(current.account)
            is LimitedAccountState.VerifyInvitationOtp -> LimitedAccountState.EnterInvitationCode(current.account, current.invitationCode)
            else -> _state.value
        }
    }

    private fun refreshMeForLinked() {
        viewModelScope.launch {
            authRepository.getMe()
                .onSuccess { account ->
                    if (account.linkStatus == PatientLinkStatus.LINKED) {
                        _state.value = LimitedAccountState.Linked(account)
                    } else {
                        _state.value = LimitedAccountState.Overview(account = account)
                    }
                }
                .onFailure {
                    _state.value = LimitedAccountState.Error("Could not verify link status")
                }
        }
    }

    private fun refreshLinkState() {
        viewModelScope.launch {
            accountRepository.getLinkState()
                .onSuccess { linkState ->
                    val current = _state.value
                    if (current is LimitedAccountState.Overview) {
                        _state.value = current.copy(linkState = linkState)
                    }
                }
        }
    }
}
