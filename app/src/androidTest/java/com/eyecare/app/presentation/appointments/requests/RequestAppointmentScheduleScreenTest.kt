package com.eyecare.app.presentation.appointments.requests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
    fun chosenTimesCard_showsPreferredAndRankedBackupsWithTheirDays() {
        setSchedule(
            state = scheduleState(alternatives = listOf(slotOne, slotTwo)),
        )

        composeRule.onNodeWithText("Your times").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Preferred: Aug 10, 2026 · 9:00 AM – 9:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Backup 1: Aug 11, 2026 · 10:00 AM – 10:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Backup 2: Aug 12, 2026 · 11:00 AM – 11:45 AM")
            .assertIsDisplayed()
    }

    /**
     * A backup on another day is unreachable from the slot list, so removal has to live on the
     * card. Previously the card disappeared once both backups were chosen, stranding the patient.
     */
    @Test
    fun bothBackupsChosen_canStillBeRemovedFromTheCard() {
        var alternatives by mutableStateOf(listOf(slotOne, slotTwo))

        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(alternatives = alternatives),
                    onShowWeek = {},
                    onDateSelected = {},
                    onSelectSlot = {},
                    onStartAddingAlternatives = {},
                    onFinishAddingAlternatives = {},
                    onToggleAlternative = {},
                    onRemoveAlternative = { slot ->
                        alternatives = alternatives.filterNot { it.startsAt == slot.startsAt }
                    },
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Remove one to swap in a different time.").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Remove Backup 1, Aug 11, 2026 · 10:00 AM – 10:45 AM")
            .performClick()

        composeRule.runOnIdle { check(alternatives == listOf(slotTwo)) }
        // The remaining backup is re-ranked, and adding another becomes possible again.
        composeRule.onNodeWithContentDescription("Backup 1: Aug 12, 2026 · 11:00 AM – 11:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Add another").assertIsDisplayed()
    }

    @Test
    fun noBackupsYet_explainsWhyTheyHelpAndOffersTheAction() {
        var startedAdding = false

        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(alternatives = emptyList()),
                    onShowWeek = {},
                    onDateSelected = {},
                    onSelectSlot = {},
                    onStartAddingAlternatives = { startedAdding = true },
                    onFinishAddingAlternatives = {},
                    onToggleAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule
            .onNodeWithText(
                "Backup times are optional — offering more than one gives the clinic more " +
                    "ways to say yes.",
            )
            .assertIsDisplayed()
        composeRule.onNodeWithText("Add backup times").performClick()
        composeRule.runOnIdle { check(startedAdding) }
    }

    @Test
    fun alternativesPhase_countsProgressAndKeepsThePreferredTimeVisible() {
        setSchedule(
            state = scheduleState(alternatives = listOf(slotOne))
                .copy(phase = SchedulePhase.ALTERNATIVES),
        )

        composeRule.onNodeWithText("Add backup times").assertIsDisplayed()
        composeRule.onNodeWithText("1 of 2 backups").assertIsDisplayed()
        composeRule.onNodeWithText("Tick any other times that would also work for you.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Preferred: Aug 10, 2026 · 9:00 AM – 9:45 AM")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Done · 1 backup").assertIsDisplayed()
    }

    @Test
    fun continueIsBlockedUntilAPreferredTimeIsPicked() {
        setSchedule(state = scheduleState(alternatives = emptyList()).copy(primarySlot = null))

        composeRule.onNodeWithText("Continue").assertIsNotEnabled()
    }

    @Test
    fun availabilityError_showsRetryAction() {
        var retryCount = 0

        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = scheduleState(alternatives = emptyList()).copy(
                        availability = null,
                        availabilityError = "We couldn't load times for this day. Please try again.",
                    ),
                    onShowWeek = {},
                    onDateSelected = {},
                    onSelectSlot = {},
                    onStartAddingAlternatives = {},
                    onFinishAddingAlternatives = {},
                    onToggleAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = { retryCount++ },
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("We couldn't load times for this day. Please try again.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { check(retryCount == 1) }
    }

    @Test
    fun closedDay_pointsBackToTheDateStrip() {
        setSchedule(
            state = scheduleState(alternatives = emptyList()).copy(
                availability = availability(dayStatus = "closed", slots = emptyList()),
            ),
        )

        composeRule.onNodeWithText("The clinic is closed on Monday. Pick another day above.")
            .assertIsDisplayed()
    }

    @Test
    fun openDayWithNoAvailableSlots_showsFullyBookedState() {
        setSchedule(
            state = scheduleState(alternatives = emptyList()).copy(
                availability = availability(slots = emptyList()),
            ),
        )

        composeRule.onNodeWithText("Monday is fully booked. Pick another day above.")
            .assertIsDisplayed()
    }

    private fun setSchedule(state: RequestStep.Schedule) {
        composeRule.setContent {
            EyecareTheme {
                ScheduleContent(
                    state = state,
                    onShowWeek = {},
                    onDateSelected = {},
                    onSelectSlot = {},
                    onStartAddingAlternatives = {},
                    onFinishAddingAlternatives = {},
                    onToggleAlternative = {},
                    onRemoveAlternative = {},
                    onRetry = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }
    }

    private fun scheduleState(alternatives: List<AvailabilitySlot>) = RequestStep.Schedule(
        selectedType = appointmentType,
        identityRequired = false,
        weekStart = "2026-08-10",
        date = "2026-08-10",
        primaryDate = "2026-08-10",
        availability = availability(),
        primarySlot = primarySlot,
        alternativeSlots = alternatives,
    )

    private fun availability(
        dayStatus: String = "open",
        slots: List<AvailabilitySlot> = listOf(primarySlot, slotOne, slotTwo),
    ) = AppointmentRequestAvailability(
        date = "2026-08-10",
        timezone = "Asia/Manila",
        intervalMinutes = 15,
        slotDurationMinutes = 45,
        visitDurationMinutes = 45,
        appointmentTypeId = appointmentType.id,
        dayStatus = dayStatus,
        generatedAt = "2026-08-01T00:00:00+08:00",
        slots = slots,
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

        // Deliberately on later days: a backup routinely lives on a day the slot list is not
        // showing, which is the case the chosen-times card exists to cover.
        private val slotOne = AvailabilitySlot(
            startsAt = "2026-08-11T10:00:00+08:00",
            endsAt = "2026-08-11T10:45:00+08:00",
            available = true,
            reason = null,
        )

        private val slotTwo = AvailabilitySlot(
            startsAt = "2026-08-12T11:00:00+08:00",
            endsAt = "2026-08-12T11:45:00+08:00",
            available = true,
            reason = null,
        )
    }
}
