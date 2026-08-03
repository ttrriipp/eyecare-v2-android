package com.eyecare.app.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
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
                    account = testAccount(),
                    linkState = LinkState.Unlinked,
                    currentLinkRequest = null,
                    isSubmittingLinkRequest = false,
                    requestError = null,
                    requestedFeatureLabel = "Prescriptions",
                    onBack = {},
                    onEnterInvite = {},
                    onRequestClinicLink = {},
                    onAccountSecurity = {},
                    onSignOut = {},
                )
            }
        }

        composeRule.onNodeWithText("Link your care record").assertIsDisplayed()
        composeRule.onNodeWithText("Enter invitation code").assertIsDisplayed()
    }

    private fun testAccount() = PatientAccount(
        id = 1,
        name = "Test Account",
        firstName = "Test",
        middleName = null,
        lastName = "Account",
        email = "test@example.com",
        phone = null,
        role = "patient",
        dateOfBirth = null,
        linkStatus = PatientLinkStatus.UNLINKED,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
