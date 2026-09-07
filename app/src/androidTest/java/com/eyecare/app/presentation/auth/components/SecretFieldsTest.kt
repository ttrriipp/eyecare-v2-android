package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class SecretFieldsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun passwordGuidanceShowsRequirementAndMismatchInline() {
        composeRule.setContent {
            EyecareTheme {
                Column {
                    PasswordRequirements(password = "short")
                    PasswordMatchGuidance(
                        password = "short",
                        confirmation = "different",
                    )
                }
            }
        }

        composeRule.onNodeWithText("Password requirements").assertIsDisplayed()
        listOf(
            "At least 8 characters",
            "One uppercase letter (A-Z)",
            "One lowercase letter (a-z)",
            "One number (0-9)",
            "One special character (e.g. !@#$%^&*)",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        composeRule.onNodeWithContentDescription(
            "Password requirement not met: One uppercase letter (A-Z)",
        ).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Passwords do not match").assertIsDisplayed()
    }

    @Test
    fun passwordGuidanceConfirmsAValidPasswordAndMatchingConfirmation() {
        composeRule.setContent {
            EyecareTheme {
                Column {
                    PasswordRequirements(password = "Abcdef1!")
                    PasswordMatchGuidance(
                        password = "Abcdef1!",
                        confirmation = "Abcdef1!",
                    )
                }
            }
        }

        listOf(
            "At least 8 characters",
            "One uppercase letter (A-Z)",
            "One lowercase letter (a-z)",
            "One number (0-9)",
            "One special character (e.g. !@#$%^&*)",
        ).forEach { requirement ->
            composeRule.onNodeWithContentDescription(
                "Password requirement met: $requirement",
            ).assertIsDisplayed()
        }
        composeRule.onNodeWithContentDescription("Passwords match").assertIsDisplayed()
    }
}
