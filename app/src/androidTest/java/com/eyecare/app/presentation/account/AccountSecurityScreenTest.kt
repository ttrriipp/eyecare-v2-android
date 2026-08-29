package com.eyecare.app.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class AccountSecurityScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountOverview_omitsContactManagementWhenPhoneIsOnlyContactMethod() {
        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(account = testAccount()),
                    onBack = {},
                    onEdit = {},
                    onCancelEdit = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                    onChangePassword = {},
                    onSignOut = {},
                    onSignOutAll = {},
                )
            }
        }

        composeRule.onNodeWithText("Account details").assertIsDisplayed()
        composeRule.onNodeWithText("Account & Security").assertDoesNotExist()
        composeRule.onNodeWithText("Security").assertIsDisplayed()
        composeRule.onNodeWithText("Contact methods").assertDoesNotExist()
        composeRule.onNodeWithText("Add contact").assertDoesNotExist()
    }

    @Test
    fun accountDetails_showsSnapshotAndEditAction() {
        var editClicked = false

        composeRule.setContent {
            EyecareTheme {
                AccountDetailsContent(
                    account = testAccount(),
                    isEditing = false,
                    isSaving = false,
                    firstName = "",
                    middleName = "",
                    lastName = "",
                    dateOfBirth = "",
                    fieldErrors = emptyMap(),
                    saveError = null,
                    onEdit = { editClicked = true },
                    onCancel = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                )
            }
        }

        listOf(
            "Profile details",
            "Alex",
            "Rivera",
            "M.",
            "alex@example.com",
            "09171234567",
            "May 15, 1990",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

        composeRule.onNodeWithText("Role").assertDoesNotExist()
        composeRule.onNodeWithText("Link status").assertDoesNotExist()

        composeRule.onNodeWithText("Edit").performClick()
        composeRule.runOnIdle { check(editClicked) }
    }

    @Test
    fun accountDetails_editMode_explainsSupportedFieldsAndKeepsReadOnlyDetails() {
        composeRule.setContent {
            EyecareTheme {
                AccountDetailsContent(
                    account = testAccount(),
                    isEditing = true,
                    isSaving = false,
                    firstName = "Alex",
                    middleName = "M.",
                    lastName = "Rivera",
                    dateOfBirth = "1990-05-15",
                    fieldErrors = emptyMap(),
                    saveError = null,
                    onEdit = {},
                    onCancel = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("First name").assertIsDisplayed()
        composeRule.onNodeWithText("Middle name").assertIsDisplayed()
        composeRule.onNodeWithText("Last name").assertIsDisplayed()
        composeRule.onNodeWithText("Date of birth").assertIsDisplayed()
        composeRule.onNodeWithText("Profile details").assertIsDisplayed()
        composeRule.onNodeWithText("alex@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun accountDetails_saveIsDisabledWhenDraftIsUnchanged() {
        composeRule.setContent {
            EyecareTheme {
                AccountDetailsContent(
                    account = testAccount(),
                    isEditing = true,
                    isSaving = false,
                    firstName = "Alex",
                    middleName = "M.",
                    lastName = "Rivera",
                    dateOfBirth = "1990-05-15",
                    fieldErrors = emptyMap(),
                    saveError = null,
                    onEdit = {},
                    onCancel = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun accountDetails_dateOfBirthFieldOpensDatePickerWhenTapped() {
        composeRule.setContent {
            EyecareTheme {
                AccountDetailsContent(
                    account = testAccount(),
                    isEditing = true,
                    isSaving = false,
                    firstName = "Alex",
                    middleName = "M.",
                    lastName = "Rivera",
                    dateOfBirth = "",
                    fieldErrors = emptyMap(),
                    saveError = null,
                    onEdit = {},
                    onCancel = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Date of birth, not set. Double tap to choose a date.")
            .performTouchInput { click() }

        composeRule.onNodeWithText("Set date").assertIsDisplayed()
    }

    @Test
    fun accountDetails_middleNameServerErrorIsVisible() {
        composeRule.setContent {
            EyecareTheme {
                AccountDetailsContent(
                    account = testAccount(),
                    isEditing = true,
                    isSaving = false,
                    firstName = "Alex",
                    middleName = "M.",
                    lastName = "Rivera",
                    dateOfBirth = "1990-05-15",
                    fieldErrors = mapOf("middle_name" to "Middle name is invalid"),
                    saveError = null,
                    onEdit = {},
                    onCancel = {},
                    onFirstNameChange = {},
                    onMiddleNameChange = {},
                    onLastNameChange = {},
                    onDateOfBirthChange = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Middle name is invalid").assertIsDisplayed()
    }

    private fun testAccount() = PatientAccount(
        id = 1,
        name = "Alex Rivera",
        firstName = "Alex",
        middleName = "M.",
        lastName = "Rivera",
        email = "alex@example.com",
        phone = "09171234567",
        role = "patient",
        dateOfBirth = "1990-05-15",
        linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
