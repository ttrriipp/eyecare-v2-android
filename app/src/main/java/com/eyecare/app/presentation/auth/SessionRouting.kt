package com.eyecare.app.presentation.auth

import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState

enum class AuthDestination {
    SessionGate,
    Welcome,
    SignIn,
    CreateAccount,
    RecoverPassword,
    LimitedAccount,
    AccountSecurity,
    Main,
}

fun resolveDestination(sessionState: SessionState): AuthDestination = when (sessionState) {
    SessionState.Checking -> AuthDestination.SessionGate
    SessionState.Unauthenticated -> AuthDestination.Welcome
    is SessionState.Linked -> AuthDestination.Main
    is SessionState.Limited -> AuthDestination.LimitedAccount
    is SessionState.TransientFailure -> AuthDestination.SessionGate
}

fun shouldHideBottomNav(destination: AuthDestination): Boolean = when (destination) {
    AuthDestination.Main -> false
    else -> true
}
