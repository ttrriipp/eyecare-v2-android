package com.eyecare.app.presentation.appointments.requests

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class RequestIdentityContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun genderField_opensFromFullFieldAndOptionsMatchFieldWidth() {
        var selectedGender: AppointmentRequestGender? = null
        composeRule.setContent {
            EyecareTheme {
                RequestIdentityContent(
                    state = identityState(selectedGender = selectedGender),
                    onEmailChange = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onGenderChange = { selectedGender = it },
                    onOccupationChange = {},
                    onAddressChange = {},
                    onFocusHandled = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        val genderField = composeRule.onNodeWithContentDescription(
            "Gender, not set. Double tap to choose an option.",
        )
        genderField.performScrollTo()
        composeRule.waitForIdle()
        val fieldWidth = genderField.fetchSemanticsNode().boundsInRoot.width

        genderField.performTouchInput {
            click(Offset(width - 8f, height / 2f))
        }
        composeRule.waitForIdle()

        val femaleOption = composeRule.onNodeWithText("Female")
        femaleOption.assertIsDisplayed()
        assertEquals(fieldWidth, femaleOption.fetchSemanticsNode().boundsInRoot.width, 1f)

        femaleOption.performClick()
        composeRule.runOnIdle {
            assertEquals(AppointmentRequestGender.FEMALE, selectedGender)
        }
    }

    private fun identityState(selectedGender: AppointmentRequestGender?) = RequestStep.Identity(
        selectedType = AppointmentType(
            id = 1,
            name = "First eye examination",
            description = null,
            durationMinutes = 45,
            requiresReferral = false,
        ),
        date = "2026-09-07",
        primarySlot = AvailabilitySlot(
            startsAt = "2026-09-07T09:00:00+08:00",
            endsAt = "2026-09-07T09:45:00+08:00",
            available = true,
            reason = null,
        ),
        alternativeSlots = emptyList(),
        reason = "Routine check",
        referringSource = "",
        phone = "09171234567",
        firstName = "Maria",
        lastName = "Reyes",
        dateOfBirth = "1994-09-03",
        gender = selectedGender,
        occupation = "Designer",
        address = "123 Main Street",
    )
}
