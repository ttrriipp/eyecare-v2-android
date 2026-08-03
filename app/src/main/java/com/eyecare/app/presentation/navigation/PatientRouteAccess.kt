package com.eyecare.app.presentation.navigation

import com.eyecare.app.domain.model.PatientLinkStatus

sealed interface PatientRouteAccess {
    data object AccountOnly : PatientRouteAccess
    data object ActiveLinkRequired : PatientRouteAccess
}

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
