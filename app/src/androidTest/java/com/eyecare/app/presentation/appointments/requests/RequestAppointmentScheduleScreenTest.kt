package com.eyecare.app.presentation.appointments.requests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.AppointmentRequestAvailability
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestAppointmentScheduleScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun alternatives_canBeAddedInSubmittedOrderAndRemoved() {
        var alternatives by mutableStateOf(emptyList<AvailabilitySlot>())

        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(alternatives),
                    onDateSelected = {},
                    onSelectSlot = {},
                    onAddAlternative = { slot -> alternatives = alternatives + slot },
                    onRemoveAlternative = { slot ->
                        alternatives = alternatives.filterNot { it.startsAt == slot.startsAt }
                    },
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Add as alternative").assertCountEquals(2)
        composeRule.onAllNodesWithText("Add as alternative")[0].performClick()
        composeRule.onAllNodesWithText("Add as alternative")[0].performClick()

        composeRule.onNodeWithText("Alternative 1").assertIsDisplayed()
        composeRule.onNodeWithText("Alternative 2").assertIsDisplayed()
        composeRule.onNodeWithText("Remove alternative 1").performClick()
        composeRule.onNodeWithText("Alternative 1").assertIsDisplayed()
        composeRule.runOnIdle { check(alternatives == listOf(slotTwo)) }
    }

    @Test
    fun availabilityError_showsRetryAction() {
        var retryCount by mutableStateOf(0)

        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(emptyList()).copy(
                        availability = null,
                        availabilityError = "We couldn't load availability. Please try again.",
                    ),
                    onDateSelected = {},
                    onSelectSlot = {},
                    onAddAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = { retryCount++ },
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("We couldn't load availability. Please try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { check(retryCount == 1) }
    }

    @Test
    fun closedDay_showsClosedState() {
        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(emptyList()).copy(
                        availability = scheduleState(emptyList()).availability?.copy(
                            dayStatus = "closed",
                            slots = emptyList(),
                        ),
                    ),
                    onDateSelected = {},
                    onSelectSlot = {},
                    onAddAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("The clinic is closed on this date.")
            .assertIsDisplayed()
    }

    @Test
    fun openDayWithNoAvailableSlots_showsEmptyState() {
        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(emptyList()).copy(
                        availability = scheduleState(emptyList()).availability?.copy(
                            slots = emptyList(),
                        ),
                    ),
                    onDateSelected = {},
                    onSelectSlot = {},
                    onAddAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("No appointment times are available on this date.")
            .assertIsDisplayed()
    }

    private fun scheduleState(alternatives: List<AvailabilitySlot>) = RequestStep.Schedule(
        selectedType = appointmentType,
        date = "2026-08-10",
        primaryDate = "2026-08-10",
        availability = AppointmentRequestAvailability(
            date = "2026-08-10",
            timezone = "Asia/Manila",
            intervalMinutes = 15,
            slotDurationMinutes = 45,
            visitDurationMinutes = 45,
            appointmentTypeId = appointmentType.id,
            dayStatus = "open",
            generatedAt = "2026-08-01T00:00:00+08:00",
            slots = listOf(primarySlot, slotOne, slotTwo),
        ),
        primarySlot = primarySlot,
        alternativeSlots = alternatives,
    )

    companion object {
        private val appointmentType = AppointmentType(
            id = 1,
            name = "First eye examination",
            description = null,
            durationMinutes = 45,
            requiresReferral = false,
        )

        private val primarySlot = AvailabilitySlot(
            startsAt = "2026-08-10T09:00:00+08:00",
            endsAt = "2026-08-10T09:45:00+08:00",
            available = true,
            reason = null,
        )

        private val slotOne = AvailabilitySlot(
            startsAt = "2026-08-10T10:00:00+08:00",
            endsAt = "2026-08-10T10:45:00+08:00",
            available = true,
            reason = null,
        )

        private val slotTwo = AvailabilitySlot(
            startsAt = "2026-08-10T11:00:00+08:00",
            endsAt = "2026-08-10T11:45:00+08:00",
            available = true,
            reason = null,
        )
    }
}
