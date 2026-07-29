package com.eyecare.app.presentation.messaging.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.Message
import com.eyecare.app.domain.model.MessageContext
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class MessageBubbleTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appointmentContext_rendersAsCardAndOpensAppointment() {
        var openedAppointmentId: Int? = null

        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message(MessageContext.Appointment(7)),
                    isOwn = true,
                    onAppointmentClick = { openedAppointmentId = it },
                )
            }
        }

        composeRule.onNodeWithText("Appointment").assertIsDisplayed()
        composeRule.onNodeWithText("Reference #7").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open appointment 7").performClick()
        composeRule.runOnIdle { check(openedAppointmentId == 7) }
    }

    @Test
    fun appointmentContext_secondCard_rendersAndOpens() {
        var openedAppointmentId: Int? = null

        composeRule.setContent {
            EyecareTheme {
                MessageBubble(
                    message = message(MessageContext.Appointment(12)),
                    isOwn = false,
                    onAppointmentClick = { openedAppointmentId = it },
                )
            }
        }

        composeRule.onNodeWithText("Appointment").assertIsDisplayed()
        composeRule.onNodeWithText("Reference #12").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open appointment 12").performClick()
        composeRule.runOnIdle { check(openedAppointmentId == 12) }
    }

    private fun message(context: MessageContext) = Message(
        id = 10,
        conversationId = 1,
        senderId = 2,
        body = "Linked record",
        readAt = null,
        createdAt = "2026-07-23T10:00:00Z",
        attachments = emptyList(),
        contexts = listOf(context),
    )
}
