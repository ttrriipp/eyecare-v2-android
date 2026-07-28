package com.eyecare.app.presentation.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.User
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun profileContent_leadsWithIdentityAndPreservesAccountDestinations() {
        var destination = ""

        composeRule.setContent {
            EyecareTheme {
                ProfileContent(
                    user = patient(),
                    unreadMessageCount = 0,
                    onEditProfile = { destination = "edit" },
                    onNavigateToMessages = { destination = "messages" },
                    onNavigateToPrescriptions = { destination = "prescriptions" },
                    onLogoutClick = { destination = "logout" },
                )
            }
        }

        composeRule.onNodeWithText("Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Your care, in one place").assertIsDisplayed()
        composeRule.onNodeWithText("Alex Rivera").assertIsDisplayed()
        composeRule.onNodeWithText("alex@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("09171234567").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile initials AR").assertIsDisplayed()
        composeRule.onNodeWithText("Care & activity").assertIsDisplayed()

        listOf(
            "Edit profile" to "edit",
            "Messages" to "messages",
            "Prescriptions" to "prescriptions",
            "Log out" to "logout",
        ).forEach { (label, expectedDestination) ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle { check(destination == expectedDestination) }
        }
    }

    @Test
    fun profileContent_handlesMissingPhoneAndAnnouncesUnreadMessages() {
        composeRule.setContent {
            EyecareTheme {
                ProfileContent(
                    user = patient(phone = null),
                    unreadMessageCount = 12,
                )
            }
        }

        composeRule.onNodeWithText("09171234567").assertDoesNotExist()
        composeRule.onNodeWithText("9+").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("12 unread messages").assertIsDisplayed()
    }

    @Test
    fun profileLoadingContent_hasAccessibleContentShape() {
        composeRule.setContent {
            EyecareTheme {
                ProfileLoadingContent()
            }
        }

        composeRule.onNodeWithContentDescription("Loading profile").assertIsDisplayed()
    }

    @Test
    fun editProfileContent_keepsSupportedFieldsAndSavesDirectly() {
        var saved = false

        composeRule.setContent {
            EyecareTheme {
                EditProfileContent(
                    state = editState(),
                    onSave = { saved = true },
                )
            }
        }

        composeRule.onNodeWithText("Edit Profile").assertIsDisplayed()
        composeRule.onNodeWithText("Personal details").assertIsDisplayed()
        composeRule.onNodeWithText("Name").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Phone").assertIsDisplayed()

        composeRule.onNodeWithText("Save changes").performClick()
        composeRule.runOnIdle { check(saved) }
        composeRule.onNodeWithText("Save changes?").assertDoesNotExist()
    }

    @Test
    fun editProfileContent_exposesSavingAndFailureStates() {
        composeRule.setContent {
            EyecareTheme {
                EditProfileContent(
                    state = editState(
                        isSaving = true,
                        saveError = "We couldn't save your changes. Please try again.",
                    ),
                )
            }
        }

        composeRule.onNodeWithContentDescription("Saving profile").assertIsDisplayed()
        composeRule.onNodeWithText("We couldn't save your changes. Please try again.")
            .assertIsDisplayed()
    }

    private fun patient(phone: String? = "09171234567") = User(
        id = 1,
        name = "Alex Rivera",
        email = "alex@example.com",
        phone = phone,
        role = "customer",
        patientNumber = "PAT-001",
        fullName = "Alex Rivera",
        dateOfBirth = "1990-05-15",
        occupation = "Engineer",
        address = "123 Main St",
        gender = "Male",
        contactEmail = "alex@example.com",
    )

    private fun editState(
        isSaving: Boolean = false,
        saveError: String? = null,
    ) = ProfileUiState.Success(
        user = patient(),
        isEditing = true,
        isSaving = isSaving,
        editName = "Alex Rivera",
        editEmail = "alex@example.com",
        editPhone = "09171234567",
        editFullName = "Alex Rivera",
        editDateOfBirth = "1990-05-15",
        editOccupation = "Engineer",
        editAddress = "123 Main St",
        editGender = "Male",
        editContactEmail = "alex@example.com",
        saveError = saveError,
    )
}
