package com.eyecare.app.presentation.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.ThemePreference
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class AppearanceSettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appearanceSettings_showsAllThemeChoicesAndSelectsDark() {
        var selectedPreference = ThemePreference.LIGHT

        composeRule.setContent {
            EyecareTheme {
                AppearanceSettingsContent(
                    selectedPreference = selectedPreference,
                    onBack = {},
                    onPreferenceSelected = { selectedPreference = it },
                )
            }
        }

        composeRule.onNodeWithText("Appearance").assertIsDisplayed()
        composeRule.onNodeWithText("Light").assertIsDisplayed()
        composeRule.onNodeWithText("Dark").assertIsDisplayed()
        composeRule.onNodeWithText("Use device setting").assertIsDisplayed()

        composeRule
            .onNodeWithContentDescription("Dark theme option")
            .performClick()
        composeRule.runOnIdle {
            check(selectedPreference == ThemePreference.DARK)
        }
    }
}
