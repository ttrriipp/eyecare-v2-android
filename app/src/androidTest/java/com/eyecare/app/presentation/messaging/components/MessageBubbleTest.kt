package com.eyecare.app.presentation.messaging.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.eyecare.app.domain.model.ConversationAccessLevel
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.SenderType
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class MessageBubbleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun plainTextMessage_isDisplayed() {
        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message("Hello from the clinic"),
                    isOwn = false,
                )
            }
        }

        composeRule.onNodeWithText("Hello from the clinic").assertIsDisplayed()
    }

    @Test
    fun ownMessage_isDisplayed() {
        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message("My question"),
                    isOwn = true,
                )
            }
        }

        composeRule.onNodeWithText("My question").assertIsDisplayed()
    }

    @Test
    fun ownUnreadMessage_showsSentReceipt() {
        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message("My question", readAt = null),
                    isOwn = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Sent").assertIsDisplayed()
    }

    @Test
    fun ownReadMessage_showsReadReceipt() {
        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message("My question", readAt = "2026-07-23T10:05:00Z"),
                    isOwn = true,
                )
            }
        }

        composeRule.onNodeWithContentDescription("Read").assertIsDisplayed()
    }

    private fun message(body: String, readAt: String? = null) = Message(
        id = 10,
        senderId = 2,
        senderType = SenderType.STAFF,
        body = body,
        readAt = readAt,
        createdAt = "2026-07-23T10:00:00Z",
        attachments = emptyList(),
    )
}
