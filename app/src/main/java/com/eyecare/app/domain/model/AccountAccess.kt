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
    PatientLinkStatus.LINKED,
    PatientLinkStatus.UNLINKED,
    PatientLinkStatus.PENDING_REVIEW,
    PatientLinkStatus.UNKNOWN -> RouteDestination.MainGraph
}

/**
 * Clinical records are protected by the active patient-link boundary. Account-only screens
 * remain available to limited sessions, so callers must use this policy only for patient data.
 */
fun canAccessPatientFeatures(sessionState: SessionState): Boolean =
    sessionState is SessionState.Linked

enum class RouteDestination {
    AuthGraph,
    AccountAccessGraph,
    MainGraph,
}
