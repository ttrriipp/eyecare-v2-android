package com.eyecare.app.presentation.appointments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.AppointmentRequest
import com.eyecare.app.domain.model.AppointmentRequestStatus
import com.eyecare.app.domain.model.AppointmentRequestType
import com.eyecare.app.domain.model.AppointmentStatus
import com.eyecare.app.domain.model.AppointmentV1
import com.eyecare.app.domain.model.CurrentAppointmentJourney
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class MyAppointmentScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun linkedAppointment_showsFullDetailsHistoryAndDetailAction() {
        composeRule.setContent {
            EyecareTheme {
                MyAppointmentScreen(
                    uiState = appointmentState(contactNotes = "Please call before arrival."),
                    onRetry = {},
                    onRefresh = {},
                    onRequestAppointment = {},
                    onNavigateToHistory = {},
                    onNavigateToRequestDetail = {},
                    onNavigateToAppointmentDetail = {},
                    onCancelRequest = { _, _ -> },
                    onCancelAppointment = {},
                    onClearMutationError = {},
                    onClearMutationSuccess = {},
                    hasActivePatientLink = true,
                )
            }
        }

        composeRule.onNodeWithText("Your booking note").assertIsDisplayed()
        composeRule.onNodeWithText("Please call before arrival.").assertIsDisplayed()
        composeRule.onNodeWithText("Request a different time").assertIsDisplayed()
        composeRule.onNodeWithText("View appointment details").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Appointment history").assertIsDisplayed()
    }

    @Test
    fun unlinkedAppointment_hidesProtectedActionsAndExplainsLinkRequirement() {
        composeRule.setContent {
            EyecareTheme {
                MyAppointmentScreen(
                    uiState = appointmentState(),
                    onRetry = {},
                    onRefresh = {},
                    onRequestAppointment = {},
                    onNavigateToHistory = {},
                    onNavigateToRequestDetail = {},
                    onNavigateToAppointmentDetail = {},
                    onCancelRequest = { _, _ -> },
                    onCancelAppointment = {},
                    onClearMutationError = {},
                    onClearMutationSuccess = {},
                    hasActivePatientLink = false,
                )
            }
        }

        composeRule.onNodeWithText("Connect your clinic record to manage this appointment.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Link your account").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel appointment").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Appointment history").assertDoesNotExist()
    }

    @Test
    fun unlinkedAppointment_keepsPendingRescheduleRequestDetailsAvailable() {
        val pendingReschedule = AppointmentRequest(
            id = 7,
            requestNumber = "APR-2030-000007",
            status = AppointmentRequestStatus.PENDING,
            requestType = AppointmentRequestType.RESCHEDULE,
            patientId = null,
            appointmentType = null,
            scheduledAt = "2030-09-26T09:00:00+08:00",
            alternativeScheduledTimes = emptyList(),
            provisionalDurationMinutes = null,
            reasonForVisit = null,
            referringSource = null,
            timePreferencesAreReserved = false,
            expiresAt = null,
            cancelledAt = null,
            rejectionReason = null,
            createdAt = "2030-09-01T09:00:00+08:00",
            appointmentId = 42,
        )
        composeRule.setContent {
            EyecareTheme {
                MyAppointmentScreen(
                    uiState = appointmentState(pendingReschedule = pendingReschedule),
                    onRetry = {},
                    onRefresh = {},
                    onRequestAppointment = {},
                    onNavigateToHistory = {},
                    onNavigateToRequestDetail = {},
                    onNavigateToAppointmentDetail = {},
                    onCancelRequest = { _, _ -> },
                    onCancelAppointment = {},
                    onClearMutationError = {},
                    onClearMutationSuccess = {},
                    hasActivePatientLink = false,
                )
            }
        }

        composeRule.onNodeWithText("Time-change request pending").assertIsDisplayed()
        composeRule.onNodeWithText("View request details").assertIsDisplayed()
        composeRule.onNodeWithText("Request a different time").assertDoesNotExist()
    }

    private fun appointmentState(
        contactNotes: String? = null,
        pendingReschedule: AppointmentRequest? = null,
    ) =
        MyAppointmentUiState.Content(
            journey = CurrentAppointmentJourney.Appointment(
                appointment = AppointmentV1(
                    id = 42,
                    appointmentNumber = "APT-2026-000042",
                    appointmentType = "First eye examination",
                    durationMinutes = 45,
                    referringSource = null,
                    status = AppointmentStatus.SCHEDULED,
                    scheduledAt = "2030-09-25T09:00:00+08:00",
                    contactNotes = contactNotes,
                    reasonForVisit = "Annual checkup",
                    lastRescheduleReason = null,
                    source = "mobile",
                    assignedOptometrist = null,
                ),
                originalRequest = null,
                pendingReschedule = pendingReschedule,
            ),
        )
}
