package com.eyecare.app.presentation.eyewear

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FrameRatingDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun publicDisplayConsentIsUncheckedUntilThePatientSelectsIt() {
        var submittedConsent: Boolean? = null

        composeRule.setContent {
            EyecareTheme {
                FrameRatingDialog(
                    currentRating = 5,
                    currentComment = "Comfortable and sturdy.",
                    onSubmit = { _, _, consent -> submittedConsent = consent },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithTag("public-review-consent").assertIsOff()
        composeRule.onNodeWithText("Update").performClick()
        composeRule.runOnIdle { assertEquals(false, submittedConsent) }

        composeRule.onNodeWithTag("public-review-consent").performClick()
        composeRule.onNodeWithTag("public-review-consent").assertIsOn()
        composeRule.onNodeWithText("Update").performClick()
        composeRule.runOnIdle { assertEquals(true, submittedConsent) }
    }

    @Test
    fun existingOwnerAttachmentIsPreviewedWhenUpdatingRating() {
        composeRule.setContent {
            EyecareTheme {
                FrameRatingDialog(
                    currentRating = 5,
                    currentAttachmentUrl = "/api/v1/optical-order-items/20/rating/attachment",
                    onSubmit = { _, _, _ -> },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Current review photo").assertIsDisplayed()
    }
}
