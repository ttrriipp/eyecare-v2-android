package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestAppointmentTypeScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun typeCatalog_canScrollToLaterTypes() {
        val types = (1..8).map { id ->
            AppointmentType(
                id = id,
                name = "Appointment type $id",
                description = "Description $id",
                durationMinutes = 45,
                requiresReferral = false,
            )
        }

        composeRule.setContent {
            EyecareTheme {
                TypePlaceholderContent(
                    state = RequestStep.Type(
                        types = types,
                        isLoading = false,
                    ),
                    onSelectType = {},
                    onConfirm = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Appointment type 8")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun typeCatalog_showsRefreshNotice() {
        composeRule.setContent {
            EyecareTheme {
                TypePlaceholderContent(
                    state = RequestStep.Type(
                        types = listOf(
                            AppointmentType(
                                id = 1,
                                name = "First eye examination",
                                description = null,
                                durationMinutes = 45,
                                requiresReferral = false,
                            ),
                        ),
                        isLoading = false,
                        notice = "That appointment type is no longer available. Please choose another.",
                    ),
                    onSelectType = {},
                    onConfirm = {},
                    onRetry = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("That appointment type is no longer available. Please choose another.")
            .assertIsDisplayed()
    }
}
