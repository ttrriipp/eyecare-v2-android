package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.AppointmentBookingBlockingReason
import com.eyecare.app.domain.model.AppointmentBookingEligibility
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestAppointmentAccessContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun limitReachedExplainsWhyAnotherRequestCannotBeStarted() {
        composeRule.setContent {
            EyecareTheme {
                RequestLimitReachedContent(
                    activeRequestCount = 1,
                    onViewRequests = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("You have 1 pending appointment request").assertIsDisplayed()
        composeRule.onNodeWithText("View my requests").assertIsDisplayed()
    }

    @Test
    fun scheduledAppointmentExplainsHowToContinue() {
        composeRule.setContent {
            EyecareTheme {
                RequestBookingBlockedContent(
                    eligibility = AppointmentBookingEligibility(
                        canSubmitNewRequest = false,
                        blockingReason = AppointmentBookingBlockingReason.SCHEDULED_APPOINTMENT,
                        appointmentId = 42,
                        canRequestRebooking = true,
                    ),
                    onViewAppointments = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Active appointment already exists").assertIsDisplayed()
        composeRule.onNodeWithText("View my appointments").assertIsDisplayed()
    }

    @Test
    fun checkedInAppointmentDoesNotOfferRescheduling() {
        composeRule.setContent {
            EyecareTheme {
                RequestBookingBlockedContent(
                    eligibility = AppointmentBookingEligibility(
                        canSubmitNewRequest = false,
                        blockingReason = AppointmentBookingBlockingReason.CHECKED_IN_APPOINTMENT,
                    ),
                    onViewAppointments = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText(
            "You already have a checked-in appointment. You may cancel it before requesting another.",
        ).assertIsDisplayed()
    }
}
