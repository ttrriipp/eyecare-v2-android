package com.eyecare.app.presentation.appointments

import com.eyecare.app.domain.model.PatientLinkStatus

enum class AppointmentsTab {
    REQUESTS,
    CONFIRMED,
}

data class AppointmentsCoordinatorState(
    val selectedTab: AppointmentsTab,
    val isLinked: Boolean,
    val showLinkRequired: Boolean,
)

fun resolveAppointmentsTab(linkStatus: PatientLinkStatus): AppointmentsTab = when (linkStatus) {
    PatientLinkStatus.LINKED -> AppointmentsTab.CONFIRMED
    PatientLinkStatus.UNLINKED,
    PatientLinkStatus.PENDING_REVIEW,
    PatientLinkStatus.UNKNOWN -> AppointmentsTab.REQUESTS
}

fun resolveAppointmentsCoordinator(linkStatus: PatientLinkStatus): AppointmentsCoordinatorState {
    val isLinked = linkStatus == PatientLinkStatus.LINKED
    return AppointmentsCoordinatorState(
        selectedTab = resolveAppointmentsTab(linkStatus),
        isLinked = isLinked,
        showLinkRequired = !isLinked,
    )
}
