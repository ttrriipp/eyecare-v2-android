package com.eyecare.app.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
                    onLastNameChange = {},
                    onSave = {},
                    onChangePassword = {},
                    onSignOut = {},
                    onSignOutAll = {},
                )
            }
        }

        composeRule.onNodeWithText("Account details").assertIsDisplayed()
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
                    lastName = "",
                    saveError = null,
                    onEdit = { editClicked = true },
                    onCancel = {},
                    onFirstNameChange = {},
                    onLastNameChange = {},
                    onSave = {},
                )
            }
        }

        listOf(
            "Account details",
            "Alex",
            "Rivera",
            "M.",
            "alex@example.com",
            "09171234567",
            "May 15, 1990",
            "Patient",
            "Linked",
        ).forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }

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
                    lastName = "Rivera",
                    saveError = null,
                    onEdit = {},
                    onCancel = {},
                    onFirstNameChange = {},
                    onLastNameChange = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithText("Only first and last name can be edited here. Phone and clinical details are read-only for now.").assertIsDisplayed()
        composeRule.onNodeWithText("First name").assertIsDisplayed()
        composeRule.onNodeWithText("Last name").assertIsDisplayed()
        composeRule.onNodeWithText("alex@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
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
