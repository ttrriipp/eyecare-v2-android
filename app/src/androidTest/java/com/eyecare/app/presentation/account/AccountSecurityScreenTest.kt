package com.eyecare.app.presentation.account

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class AccountSecurityScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun accountOverview_showsFullEmailAndFixedPhoneWithoutPrimaryControls() {
        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount(),
                        contacts = testContacts(),
                    ),
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
        composeRule.onNodeWithText("Contact information").assertIsDisplayed()
        composeRule.onNodeWithText("alex@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("a***@example.com").assertDoesNotExist()
        composeRule.onNodeWithText("+63 917 123 4567").assertIsDisplayed()
        composeRule.onNodeWithText("0917***4567").assertDoesNotExist()
        composeRule.onNodeWithText("Primary").assertDoesNotExist()
        composeRule.onNodeWithText("Make primary").assertDoesNotExist()
    }

    @Test
    fun accountOverview_contactErrorOffersRetry() {
        var retryClicked = false

        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount(),
                        contactsError = "Contacts unavailable",
                    ),
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
                    onRetryContacts = { retryClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Contact information is unavailable.").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { check(retryClicked) }
    }

    @Test
    fun accountOverview_missingContactOffersAddAction() {
        var addEmailClicked = false

        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount().copy(email = null),
                        contacts = listOf(testContacts().last()),
                    ),
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
                    onAddEmail = { addEmailClicked = true },
                )
            }
        }

        composeRule.onNodeWithText("Add email").performClick()
        composeRule.runOnIdle { check(addEmailClicked) }
    }

    @Test
    fun addEmail_isAnEmailOnlyFlow() {
        composeRule.setContent {
            EyecareTheme {
                EnterNewEmailContent(
                    state = AccountSecurityState.EnterNewContact(),
                    onBack = {},
                    onValueChange = {},
                    onContinue = {},
                )
            }
        }

        composeRule.onNodeWithText("Add email").assertIsDisplayed()
        composeRule.onNodeWithText("Email address").assertIsDisplayed()
        composeRule.onNodeWithText("Add contact").assertDoesNotExist()
        composeRule.onNodeWithText("Phone").assertDoesNotExist()
        composeRule.onNodeWithText("Phone number").assertDoesNotExist()
    }

    @Test
    fun accountOverview_neverOffersAddPhone() {
        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount().copy(phone = null),
                        contacts = listOf(testContacts().first()),
                    ),
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

        composeRule.onNodeWithText("Add phone").assertDoesNotExist()
    }

    @Test
    fun accountOverview_emailCanBeRemovedButPrimaryControlsAreHidden() {
        var removedId: Int? = null

        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount(),
                        contacts = listOf(testContacts().first().copy(isPrimary = false)),
                    ),
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
                    onRemoveContact = { removedId = it },
                )
            }
        }

        composeRule.onNodeWithText("Make primary").assertDoesNotExist()
        composeRule.onNodeWithText("Primary").assertDoesNotExist()
        composeRule.onNodeWithText("Remove").performClick()
        composeRule.runOnIdle {
            check(removedId == 1)
        }
    }

    @Test
    fun accountOverview_phoneNeverOffersPrimaryOrRemoveActions() {
        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount().copy(email = null),
                        contacts = listOf(
                            testContacts().last().copy(
                                isPrimary = false,
                                verifiedAt = "2026-08-01T10:00:00+08:00",
                            ),
                        ),
                    ),
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

        composeRule.onNodeWithText("Primary").assertDoesNotExist()
        composeRule.onNodeWithText("Make primary").assertDoesNotExist()
        composeRule.onNodeWithText("Remove").assertDoesNotExist()
        composeRule.onNodeWithText("+63 917 123 4567").assertIsDisplayed()
        composeRule.onNodeWithText("Add email").assertIsDisplayed()
    }

    @Test
    fun accountOverview_editingDisablesContactActions() {
        composeRule.setContent {
            EyecareTheme {
                AccountSecurityOverviewContent(
                    state = AccountSecurityState.Overview(
                        account = testAccount().copy(email = null),
                        contacts = listOf(testContacts().last()),
                        isEditingAccount = true,
                        editFirstName = "Alex",
                        editLastName = "Rivera",
                        editDateOfBirth = "1990-05-15",
                    ),
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

        composeRule.onNodeWithText("Add email").assertIsNotEnabled()
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
            "+63 917 123 4567",
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
            .assertIsEnabled()
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

    private fun testContacts() = listOf(
        AccountContact(
            id = 1,
            type = ContactType.EMAIL,
            maskedValue = "a***@example.com",
            isPrimary = false,
            verifiedAt = "2026-08-01T10:00:00+08:00",
        ),
        AccountContact(
            id = 2,
            type = ContactType.PHONE,
            maskedValue = "0917***4567",
            isPrimary = true,
            verifiedAt = "2026-08-01T10:00:00+08:00",
        ),
    )
}
