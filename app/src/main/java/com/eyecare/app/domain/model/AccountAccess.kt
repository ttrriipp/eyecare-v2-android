package com.eyecare.app.domain.model

sealed interface SessionState {
    data object Checking : SessionState
    data object Unauthenticated : SessionState
    data class Linked(val account: PatientAccount) : SessionState
    data class Limited(val account: PatientAccount) : SessionState
    data class TransientFailure(val message: String) : SessionState
}

fun resolveSessionState(account: PatientAccount?): SessionState = when {
    account == null -> SessionState.Unauthenticated
    account.linkStatus == PatientLinkStatus.LINKED -> SessionState.Linked(account)
    else -> SessionState.Limited(account)
}

fun routeFromLinkStatus(linkStatus: PatientLinkStatus): RouteDestination = when (linkStatus) {
    PatientLinkStatus.LINKED -> RouteDestination.MainGraph
    PatientLinkStatus.UNLINKED,
    PatientLinkStatus.PENDING_REVIEW,
    PatientLinkStatus.UNKNOWN -> RouteDestination.AccountAccessGraph
}

enum class RouteDestination {
    AuthGraph,
    AccountAccessGraph,
    MainGraph,
}
