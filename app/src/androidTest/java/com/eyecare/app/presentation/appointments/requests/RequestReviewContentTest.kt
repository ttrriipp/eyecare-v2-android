package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentRequestIdentity
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestReviewContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun reviewShowsPreferredTimeRankedAlternativesReasonAndReferral() {
        setReview(state = reviewState())

        composeRule.onNodeWithText("Preferred time").assertIsDisplayed()
        composeRule.onNodeWithText("Monday, August 10, 2026").assertIsDisplayed()
        composeRule.onNodeWithText("9:00 AM – 9:45 AM").assertIsDisplayed()
        composeRule.onNodeWithText("Alternative times").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Alternative 1: Aug 11, 2026 · 10:00 AM – 10:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Alternative 2: Aug 12, 2026 · 11:00 AM – 11:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Reason for visit").assertIsDisplayed()
        composeRule.onNodeWithText("Blurred vision").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Referred by: Dr. Santos").assertIsDisplayed()
    }

    @Test
    fun reviewShowsTheExactComposedPresetReason() {
        setReview(state = reviewState().copy(
            reason = "Blurred or reduced vision: mostly in my left eye for two weeks",
        ))

        composeRule
            .onNodeWithText("Blurred or reduced vision: mostly in my left eye for two weeks")
            .assertIsDisplayed()
    }

    @Test
    fun noAlternatives_saysSoRatherThanShowingAnEmptySection() {
        setReview(state = reviewState().copy(alternativeSlots = emptyList()))

        composeRule
            .onNodeWithText("No alternative times selected. The clinic will confirm your preferred time or contact you.")
            .assertIsDisplayed()
    }

    /** Every card's action reads "Edit"; only the description tells them apart. */
    @Test
    fun eachSectionHasItsOwnDistinguishableEditAction() {
        val edited = mutableListOf<RequestStepId>()

        composeRule.setContent {
            EyecareTheme {
                RequestReviewContent(
                    state = reviewState(identity = identity),
                    onEdit = { edited += it },
                    onSubmit = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Change appointment time")
            .performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Change reason for visit")
            .performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Change your details")
            .performScrollTo().performClick()

        composeRule.runOnIdle {
            check(
                edited == listOf(
                    RequestStepId.SCHEDULE,
                    RequestStepId.REASON,
                    RequestStepId.IDENTITY,
                ),
            )
        }
    }

    @Test
    fun identityDetailsRenderAsLabelledPairs() {
        setReview(state = reviewState(identity = identity))

        composeRule.onNodeWithText("Your details").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Name: Ana Maria Reyes")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Date of birth: March 4, 1990")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Phone: +639171234567")
            .performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("More details")
            .performScrollTo().performClick()
        composeRule.onNodeWithContentDescription("Home address: 12 Mabini Street, Quezon City")
            .performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theReviewShowsOneNonBindingNoticeAndAConciseSubmitAction() {
        setReview(state = reviewState())

        composeRule.onAllNodesWithText(REQUEST_NON_BINDING_NOTE).assertCountEquals(1)
        composeRule.onNodeWithText("Send request").assertIsDisplayed()
        composeRule.onNodeWithText("Submit request to clinic").assertDoesNotExist()
    }

    private fun setReview(state: RequestStep.Review) {
        composeRule.setContent {
            EyecareTheme {
                RequestReviewContent(
                    state = state,
                    onEdit = {},
                    onSubmit = {},
                    onBack = {},
                )
            }
        }
    }

    private fun reviewState(identity: AppointmentRequestIdentity? = null) = RequestStep.Review(
        selectedType = AppointmentType(
            id = 1,
            name = "Referral examination",
            description = null,
            durationMinutes = 45,
            requiresReferral = true,
        ),
        identityRequired = identity != null,
        date = "2026-08-10",
        primarySlot = slot("2026-08-10T09:00:00+08:00", "2026-08-10T09:45:00+08:00"),
        alternativeSlots = listOf(
            slot("2026-08-11T10:00:00+08:00", "2026-08-11T10:45:00+08:00"),
            slot("2026-08-12T11:00:00+08:00", "2026-08-12T11:45:00+08:00"),
        ),
        reason = "Blurred vision",
        referringSource = "Dr. Santos",
        identity = identity,
    )

    private fun slot(startsAt: String, endsAt: String) = AvailabilitySlot(
        startsAt = startsAt,
        endsAt = endsAt,
        available = true,
        reason = null,
    )

    private val identity = AppointmentRequestIdentity(
        phone = "+639171234567",
        email = "ana@example.com",
        firstName = "Ana",
        middleName = "Maria",
        lastName = "Reyes",
        dateOfBirth = "1990-03-04",
        gender = AppointmentRequestGender.FEMALE,
        occupation = "Teacher",
        address = "12 Mabini Street, Quezon City",
    )
}
