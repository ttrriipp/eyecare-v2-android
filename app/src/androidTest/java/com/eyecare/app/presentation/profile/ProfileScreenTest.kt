package com.eyecare.app.presentation.profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
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
                    account = testAccount(),
                    unreadMessageCount = 0,
                    onEditProfile = { destination = "edit" },
                    onNavigateToMessages = { destination = "messages" },
                    onNavigateToPrescriptions = { destination = "prescriptions" },
                    onNavigateToEyewear = { destination = "eyewear" },
                    onLogoutClick = { destination = "logout" },
                )
            }
        }

        composeRule.onNodeWithText("Alex Rivera").assertIsDisplayed()
        composeRule.onNodeWithText("alex@example.com").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Profile initials AR").assertIsDisplayed()

        listOf(
            "Edit profile" to "edit",
            "Messages" to "messages",
            "Prescriptions" to "prescriptions",
            "Eyewear" to "eyewear",
            "Log out" to "logout",
        ).forEach { (label, expectedDestination) ->
            composeRule.onNodeWithText(label).performClick()
            composeRule.runOnIdle { check(destination == expectedDestination) }
        }
    }

    @Test
    fun profileContent_handlesMissingPhone() {
        composeRule.setContent {
            EyecareTheme {
                ProfileContent(
                    account = testAccount(phone = null),
                    unreadMessageCount = 12,
                )
            }
        }

        composeRule.onNodeWithText("09171234567").assertDoesNotExist()
    }

    @Test
    fun editProfileContent_showsFirstAndLastNameFields() {
        composeRule.setContent {
            EyecareTheme {
                EditProfileContent(
                    state = ProfileUiState.Success(
                        account = testAccount(),
                        isEditing = true,
                        editFirstName = "Alex",
                        editLastName = "Rivera",
                    ),
                )
            }
        }

        composeRule.onNodeWithText("Edit Profile").assertIsDisplayed()
        composeRule.onNodeWithText("First name").assertIsDisplayed()
        composeRule.onNodeWithText("Last name").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsDisplayed()
    }

    private fun testAccount(phone: String? = "09171234567") = PatientAccount(
        id = 1,
        name = "Alex Rivera",
        firstName = "Alex",
        middleName = null,
        lastName = "Rivera",
        email = "alex@example.com",
        phone = phone,
        role = "patient",
        dateOfBirth = "1990-05-15",
        linkStatus = PatientLinkStatus.LINKED,
        privacyPolicyVersion = null,
        privacyAcceptedAt = null,
        linkedPatient = null,
    )
}
