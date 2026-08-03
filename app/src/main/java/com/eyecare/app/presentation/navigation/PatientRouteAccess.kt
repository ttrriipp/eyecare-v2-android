package com.eyecare.app.presentation.navigation

import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.domain.model.SessionState

sealed interface PatientRouteAccess {
    data object AccountOnly : PatientRouteAccess
    data object ActiveLinkRequired : PatientRouteAccess
}

private val accountSafeDestinationNames = setOf(
    "MainGraph",
    "Home",
    "Profile",
    "EditProfile",
    "AccountSecurity",
    "LimitedAccount",
    "SessionGate",
    "Welcome",
    "Login",
    "Register",
    "CreateAccount",
    "RecoverPassword",
    "AuthGraph",
    "AccountAccessGraph",
)

fun classifyRouteAccess(route: String): PatientRouteAccess = when {
    // Account-only: browse catalog and request creation/list/detail
    route.contains("Frames") ||
        route.contains("FrameDetail") ||
        route.contains("ArTryOn") -> PatientRouteAccess.AccountOnly
    route.contains("Appointments") -> PatientRouteAccess.AccountOnly
    route.contains("RequestAppointment") -> PatientRouteAccess.AccountOnly
    route.contains("AppointmentRequest") -> PatientRouteAccess.AccountOnly
    // Active-link required: confirmed appointments, clinical resources
    route.contains("AppointmentDetail") -> PatientRouteAccess.ActiveLinkRequired
    route.contains("PatientProfile") -> PatientRouteAccess.ActiveLinkRequired
    route.contains("Prescription") -> PatientRouteAccess.ActiveLinkRequired
    route.contains("Eyewear") -> PatientRouteAccess.ActiveLinkRequired
    route.contains("FrameReservation") -> PatientRouteAccess.ActiveLinkRequired
    route.contains("Chat") -> PatientRouteAccess.ActiveLinkRequired
    // Default: fail closed
    else -> PatientRouteAccess.ActiveLinkRequired
}

fun canAccessRoute(route: String, linkStatus: PatientLinkStatus): Boolean {
    val access = classifyRouteAccess(route)
    return when (access) {
        PatientRouteAccess.AccountOnly -> true
        PatientRouteAccess.ActiveLinkRequired -> linkStatus == PatientLinkStatus.LINKED
    }
}

fun canAccessRoute(route: Any, linkStatus: PatientLinkStatus): Boolean =
    canAccessRoute(route.toString(), linkStatus)

/**
 * Returns whether a limited session has landed on an active-link destination that must be
 * replaced by the account-link hub before the destination can load patient data.
 */
fun shouldRedirectToLimitedAccount(route: String?, sessionState: SessionState): Boolean {
    if (route == null || sessionState !is SessionState.Limited) return false

    val destinationName = route.substringAfterLast('.').substringBefore('/')
    if (destinationName in accountSafeDestinationNames) return false

    return classifyRouteAccess(route) == PatientRouteAccess.ActiveLinkRequired
}
