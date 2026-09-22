package com.eyecare.app.presentation.appointments

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.AppointmentAvailability
import com.eyecare.app.domain.model.AppointmentSlot
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RescheduleBottomSheetTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noSelection_explainsWhatIsNeededAndOffersExplicitDismissal() {
        composeRule.setContent {
            EyecareTheme {
                RescheduleBottomSheet(
                    currentScheduledAt = CURRENT_APPOINTMENT,
                    weekStart = "2030-09-23",
                    dayAvailability = mapOf("2030-09-24" to DayAvailability.OPEN),
                    availabilityState = RescheduleAvailabilityState.Success(availability()),
                    isSubmitting = false,
                    errorMessage = null,
                    onShowWeek = {},
                    onDateChanged = {},
                    onRetryAvailability = {},
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Choose an available time to continue.").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Keep current time").assertIsDisplayed()
    }

    @Test
    fun changingDate_clearsPreviouslySelectedTime() {
        composeRule.setContent {
            EyecareTheme {
                RescheduleBottomSheet(
                    currentScheduledAt = CURRENT_APPOINTMENT,
                    weekStart = "2030-09-23",
                    dayAvailability = mapOf(
                        "2030-09-24" to DayAvailability.OPEN,
                        "2030-09-25" to DayAvailability.OPEN,
                    ),
                    availabilityState = RescheduleAvailabilityState.Success(availability()),
                    isSubmitting = false,
                    errorMessage = null,
                    onShowWeek = {},
                    onDateChanged = {},
                    onRetryAvailability = {},
                    onDismiss = {},
                    onConfirm = { _, _, _ -> },
                )
            }
        }

        composeRule.onNodeWithContentDescription("9:00 AM – 9:30 AM").performClick()
        composeRule.onNodeWithText("Your selected times").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Wednesday 25 September, Times available")
            .performClick()

        composeRule.onAllNodesWithText("Your selected times").assertCountEquals(0)
        composeRule.onNodeWithText("No appointment times are available on this date. Try another date.")
            .assertIsDisplayed()
    }

    private fun availability() = AppointmentAvailability(
        date = "2030-09-24",
        timezone = "Asia/Manila",
        intervalMinutes = 30,
        visitReasonId = 1,
        visitDurationMinutes = 30,
        optometristId = null,
        appointmentId = 42,
        dayStatus = "open",
        generatedAt = "2030-09-20T09:00:00Z",
        slots = listOf(
            AppointmentSlot(
                startsAt = "2030-09-24T01:00:00Z",
                endsAt = "2030-09-24T01:30:00Z",
                available = true,
                reason = null,
            ),
        ),
    )

    private companion object {
        const val CURRENT_APPOINTMENT = "2030-09-24T02:00:00Z"
    }
}
