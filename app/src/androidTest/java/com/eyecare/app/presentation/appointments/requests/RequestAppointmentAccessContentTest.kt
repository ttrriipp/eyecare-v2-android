package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
                    activeRequestCount = 2,
                    onViewRequests = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("You have 2 pending appointment requests").assertIsDisplayed()
        composeRule.onNodeWithText("View my requests").assertIsDisplayed()
    }
}
