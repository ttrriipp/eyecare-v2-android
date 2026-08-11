package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestReviewContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewShowsPreferredAndOrderedAlternativesWithReasonAndReferral() {
        composeRule.setContent {
            EyecareTheme {
                RequestReviewContent(
                    state = RequestStep.Review(
                        selectedType = AppointmentType(
                            id = 1,
                            name = "Referral examination",
                            description = null,
                            durationMinutes = 45,
                            requiresReferral = true,
                        ),
                        date = "2026-08-10",
                        primarySlot = slot("2026-08-10T09:00:00+08:00", "2026-08-10T09:45:00+08:00"),
                        alternativeSlots = listOf(
                            slot("2026-08-11T10:00:00+08:00", "2026-08-11T10:45:00+08:00"),
                            slot("2026-08-12T11:00:00+08:00", "2026-08-12T11:45:00+08:00"),
                        ),
                        reason = "Blurred vision",
                        referringSource = "Dr. Santos",
                    ),
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Preferred time").assertIsDisplayed()
        composeRule.onNodeWithText("Alternative times").assertIsDisplayed()
        composeRule.onNodeWithText("Alternative 1").assertIsDisplayed()
        composeRule.onNodeWithText("Alternative 2").assertIsDisplayed()
        composeRule.onNodeWithText("Reason for visit").assertIsDisplayed()
        composeRule.onNodeWithText("Blurred vision").assertIsDisplayed()
        composeRule.onNodeWithText("Referral source").assertIsDisplayed()
        composeRule.onNodeWithText("Dr. Santos").assertIsDisplayed()
    }

    private fun slot(startsAt: String, endsAt: String) = AvailabilitySlot(
        startsAt = startsAt,
        endsAt = endsAt,
        available = true,
        reason = null,
    )
}
