package com.eyecare.app.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyecare.app.domain.model.ApiDomainError
import com.eyecare.app.domain.model.AuthApiCodes
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.repository.AccountRepository
import com.eyecare.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface LimitedAccountState {
    data class Overview(
        val account: PatientAccount,
        val linkState: LinkState? = null,
        val currentLinkRequest: PatientLinkRequest? = null,
        val isSubmittingLinkRequest: Boolean = false,
        val requestError: String? = null,
    ) : LimitedAccountState
    data class EnterInvitationCode(
        val account: PatientAccount,
        val code: String = "",
        val error: String? = null,
        val isRequesting: Boolean = false,
    ) : LimitedAccountState
    data class VerifyInvitationOtp(
        val account: PatientAccount,
        val invitationCode: String,
        val challengeId: String,
        val expiresAt: String,
        val code: String = "",
        val error: String? = null,
        val isResending: Boolean = false,
        val isVerifying: Boolean = false,
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

    private var linkStateRefreshJob: Job? = null
    private var linkRequestRefreshJob: Job? = null
    private var linkedAccountRefreshJob: Job? = null

    fun load(account: PatientAccount) {
        // The navigation handoff updates the parent session to LINKED before the
        // LimitedAccount destination finishes popping. Its account-keyed load effect
        // can therefore call back with the linked snapshot; never replace the terminal
        // state that is waiting to dispatch onLinkComplete.
        if (account.linkStatus == PatientLinkStatus.LINKED && _state.value is LimitedAccountState.Linked) {
            return
        }

        cancelRefreshJobs()
        _state.value = LimitedAccountState.Overview(account = account)
        refreshLinkState()
        refreshCurrentLinkRequest()
    }

    fun startInvitationEntry() {
        val current = _state.value
        val account = when (current) {
            is LimitedAccountState.Overview -> current.account
            else -> return
        }
        _state.value = LimitedAccountState.EnterInvitationCode(account = account)
    }

    fun submitClinicLinkRequest() {
        val current = _state.value
        if (current !is LimitedAccountState.Overview ||
            current.isSubmittingLinkRequest ||
            current.currentLinkRequest != null ||
            current.linkState is LinkState.PendingReview
        ) {
            return
        }

        viewModelScope.launch {
            _state.value = current.copy(isSubmittingLinkRequest = true, requestError = null)
            accountRepository.submitPatientLinkRequest()
                .onSuccess { request ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.Overview) {
                        _state.value = latest.copy(
                            linkState = LinkState.PendingReview,
                            currentLinkRequest = request,
                            isSubmittingLinkRequest = false,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.Overview) {
                        _state.value = latest.copy(
                            isSubmittingLinkRequest = false,
                            requestError = linkingErrorMessage(
                                error,
                                fallback = "Could not send the clinic link request.",
                            ),
                        )
                    }
                }
        }
    }

    fun updateInvitationCode(code: String) {
        val current = _state.value
        if (current is LimitedAccountState.EnterInvitationCode && !current.isRequesting) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun requestInvitationOtp() {
        val current = _state.value
        if (
            current !is LimitedAccountState.EnterInvitationCode ||
            current.code.isBlank() ||
            current.isRequesting
        ) return

        viewModelScope.launch {
            _state.value = current.copy(error = null, isRequesting = true)
            val invitationCode = current.code.trim()
            accountRepository.requestInvitationOtp(invitationCode)
                .onSuccess { challenge ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.EnterInvitationCode && latest.isRequesting) {
                        _state.value = LimitedAccountState.VerifyInvitationOtp(
                            account = latest.account,
                            invitationCode = invitationCode,
                            challengeId = challenge.challengeId,
                            expiresAt = challenge.expiresAt,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.EnterInvitationCode && latest.isRequesting) {
                        _state.value = latest.copy(
                            isRequesting = false,
                            error = linkingErrorMessage(
                                error,
                                fallback = "Could not check that invitation code.",
                            ),
                        )
                    }
                }
        }
    }

    fun resendInvitationOtp() {
        val current = _state.value
        if (
            current !is LimitedAccountState.VerifyInvitationOtp ||
            current.isResending ||
            current.isVerifying
        ) return

        viewModelScope.launch {
            _state.value = current.copy(isResending = true, error = null, code = "")
            accountRepository.requestInvitationOtp(current.invitationCode)
                .onSuccess { challenge ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.VerifyInvitationOtp && latest.isResending) {
                        _state.value = latest.copy(
                            challengeId = challenge.challengeId,
                            expiresAt = challenge.expiresAt,
                            code = "",
                            error = null,
                            isResending = false,
                        )
                    }
                }
                .onFailure { error ->
                    val latest = _state.value
                    if (latest is LimitedAccountState.VerifyInvitationOtp && latest.isResending) {
                        _state.value = latest.copy(
                            isResending = false,
                            error = linkingErrorMessage(
                                error,
                                fallback = "Could not send a new verification code.",
                            ),
                        )
                    }
                }
        }
    }

    fun updateOtpCode(code: String) {
        val current = _state.value
        if (
            current is LimitedAccountState.VerifyInvitationOtp &&
            !current.isResending &&
            !current.isVerifying
        ) {
            _state.value = current.copy(code = code, error = null)
        }
    }

    fun verifyInvitationOtp() {
        val current = _state.value
        if (
            current !is LimitedAccountState.VerifyInvitationOtp ||
            current.isResending ||
            current.isVerifying ||
            current.code.length != 6
        ) return

        viewModelScope.launch {
            cancelRefreshJobs()
            _state.value = current.copy(error = null, isVerifying = true)
            accountRepository.acceptInvitation(
                invitationCode = current.invitationCode,
                challengeId = current.challengeId,
                code = current.code,
            ).onSuccess { linkState ->
                val latest = _state.value
                if (latest !is LimitedAccountState.VerifyInvitationOtp || !latest.isVerifying) return@onSuccess

                if (linkState is LinkState.Linked) {
                    refreshMeForLinked()
                } else {
                    _state.value = LimitedAccountState.Overview(
                        account = latest.account,
                        linkState = linkState,
                    )
                }
            }.onFailure { error ->
                val latest = _state.value
                if (latest is LimitedAccountState.VerifyInvitationOtp && latest.isVerifying) {
                    _state.value = latest.copy(
                        isVerifying = false,
                        error = linkingErrorMessage(
                            error,
                            fallback = "Could not verify the invitation code.",
                        ),
                    )
                }
            }
        }
    }

    fun back() {
        when (val current = _state.value) {
            is LimitedAccountState.EnterInvitationCode -> {
                if (current.isRequesting) return
                _state.value = LimitedAccountState.Overview(current.account)
                refreshLinkState()
                refreshCurrentLinkRequest()
            }
            is LimitedAccountState.VerifyInvitationOtp -> {
                if (current.isResending || current.isVerifying) return
                _state.value = LimitedAccountState.EnterInvitationCode(current.account, current.invitationCode)
            }
            else -> Unit
        }
    }

    private fun refreshMeForLinked() {
        linkedAccountRefreshJob?.cancel()
        linkedAccountRefreshJob = viewModelScope.launch {
            authRepository.getMe()
                .onSuccess { account ->
                    if (account.linkStatus == PatientLinkStatus.LINKED) {
                        _state.value = LimitedAccountState.Linked(account)
                    } else {
                        _state.value = LimitedAccountState.Overview(account = account)
                    }
                }
                .onFailure { error ->
                    _state.value = LimitedAccountState.Error(
                        linkingErrorMessage(
                            error,
                            fallback = "Could not verify link status. Please try again.",
                        ),
                    )
                }
        }
    }

    private fun refreshLinkState() {
        linkStateRefreshJob?.cancel()
        linkStateRefreshJob = viewModelScope.launch {
            accountRepository.getLinkState()
                .onSuccess { linkState ->
                    if (linkState is LinkState.Linked) {
                        refreshMeForLinked()
                    } else {
                        val current = _state.value
                        if (current is LimitedAccountState.Overview) {
                            _state.value = current.copy(linkState = linkState)
                        }
                    }
                }
        }
    }

    private fun refreshCurrentLinkRequest() {
        linkRequestRefreshJob?.cancel()
        linkRequestRefreshJob = viewModelScope.launch {
            accountRepository.getCurrentPatientLinkRequest()
                .onSuccess { request ->
                    val current = _state.value
                    if (current is LimitedAccountState.Overview) {
                        _state.value = current.copy(
                            currentLinkRequest = request,
                            linkState = if (request != null && current.linkState !is LinkState.Linked) {
                                LinkState.PendingReview
                            } else {
                                current.linkState
                            },
                        )
                    }
                }
        }
    }

    private fun linkingErrorMessage(error: Throwable, fallback: String): String {
        val apiError = error as? ApiDomainError
        if (apiError?.httpStatus == 429 || apiError?.code == AuthApiCodes.OTP_RATE_LIMIT_REACHED) {
            return "Too many requests. Please wait before trying again."
        }
        return when (apiError?.code) {
            AuthApiCodes.INVITATION_INVALID ->
                "That invitation code is invalid or expired. Check it and try again."
            AuthApiCodes.ACCOUNT_ALREADY_LINKED ->
                "This account is already linked to a clinic record."
            AuthApiCodes.PATIENT_ALREADY_LINKED ->
                "That invitation is already linked to another account."
            AuthApiCodes.INVALID_OTP ->
                "That verification code is incorrect. Check the 6 digits and try again."
            AuthApiCodes.OTP_ATTEMPT_LIMIT_REACHED ->
                "Too many incorrect codes. Request a new code and try again."
            AuthApiCodes.OTP_RATE_LIMIT_REACHED ->
                "Too many code requests. Please wait before requesting another code."
            AuthApiCodes.LINK_REQUEST_PENDING ->
                "A clinic link request is already pending."
            AuthApiCodes.LINK_REQUEST_ALREADY_PENDING ->
                "A clinic link request is already pending."
            AuthApiCodes.ACTIVE_REQUEST_LIMIT_REACHED ->
                "You already have the maximum number of clinic link requests."
            else -> apiError?.message?.takeIf(String::isNotBlank)
                ?: error.message?.takeIf(String::isNotBlank)
                ?: fallback
        }
    }

    private fun cancelRefreshJobs() {
        linkStateRefreshJob?.cancel()
        linkRequestRefreshJob?.cancel()
        linkedAccountRefreshJob?.cancel()
    }
}
