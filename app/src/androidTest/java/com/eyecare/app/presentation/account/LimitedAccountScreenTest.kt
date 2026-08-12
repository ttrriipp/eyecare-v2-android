package com.eyecare.app.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class LimitedAccountScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun limitedOverview_rendersWithoutNestedScrollMeasurementCrash() {
        composeRule.setContent {
            EyecareTheme {
                LimitedOverviewContent(
                    linkState = LinkState.Unlinked,
                    currentLinkRequest = null,
                    isSubmittingLinkRequest = false,
                    requestError = null,
                    requestedFeatureLabel = "Prescriptions",
                    onBack = {},
                    onEnterInvite = {},
                    onRequestClinicLink = {},
                )
            }
        }

        composeRule.onNodeWithText("Link your care record").assertIsDisplayed()
        composeRule.onNodeWithText("Enter invitation code").assertIsDisplayed()
    }
}
