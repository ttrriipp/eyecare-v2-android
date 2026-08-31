package com.eyecare.app.presentation.appointments.requests

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.eyecare.app.domain.model.AppointmentType
import com.eyecare.app.domain.model.AvailabilitySlot
import com.eyecare.app.domain.model.VisitReasonPreset
import com.eyecare.app.ui.theme.EyecareTheme
import org.junit.Rule
import org.junit.Test

class RequestReasonContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun presetCatalog_rendersInOrderWithOtherAndOptionalDetails() {
        var state by mutableStateOf(reasonState(type = presetType))
        val choices = mutableListOf<VisitReasonChoice>()

        composeRule.setContent {
            EyecareTheme {
                RequestReasonContent(
                    state = state,
                    onReasonChange = { state = state.copy(reason = it) },
                    onReasonChoiceChange = {
                        choices += it
                        state = state.copy(reasonChoice = it)
                    },
                    onReferringSourceChange = { state = state.copy(referringSource = it) },
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Common reasons").assertIsDisplayed()
        composeRule.onNodeWithText("Blurred or reduced vision").assertIsDisplayed()
        composeRule.onNodeWithText("Eye pain or discomfort").assertIsDisplayed()
        composeRule.onNodeWithText("Other").assertIsDisplayed()
        composeRule.onNodeWithText("Add details (optional)").assertDoesNotExist()

        composeRule.onNodeWithText("Blurred or reduced vision").performClick()

        composeRule.runOnIdle {
            check(choices == listOf(VisitReasonChoice.Preset(21)))
        }
        composeRule.onNodeWithText("Blurred or reduced vision").assertIsSelected()
        composeRule.onNodeWithText("Eye pain or discomfort").assertIsNotSelected()
        composeRule.onNodeWithText("Add details (optional)").assertIsDisplayed()
    }

    @Test
    fun otherChoice_showsRequiredDescription() {
        var state by mutableStateOf(reasonState(type = presetType))

        composeRule.setContent {
            EyecareTheme {
                RequestReasonContent(
                    state = state,
                    onReasonChange = { state = state.copy(reason = it) },
                    onReasonChoiceChange = { state = state.copy(reasonChoice = it) },
                    onReferringSourceChange = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithText("Other").performClick()

        composeRule.onNodeWithText("Describe your reason for visit").assertIsDisplayed()
    }

    @Test
    fun noPresets_keepsDirectTextFieldWithoutChoiceGroup() {
        setReason(reasonState(type = noPresetType))

        composeRule.onNodeWithText("Common reasons").assertDoesNotExist()
        composeRule.onNodeWithText("Reason for visit").assertIsDisplayed()
        composeRule.onNodeWithText("Add details (optional)").assertDoesNotExist()
    }

    @Test
    fun reasonErrors_areShownInlineAndReferralRemainsVisible() {
        setReason(
            reasonState(type = presetReferralType).copy(
                reasonChoice = VisitReasonChoice.Other,
                reasonError = "Tell the clinic what you'd like to be seen for.",
                reasonErrorCode = VisitReasonCompositionError.REASON_REQUIRED,
                referringSourceError = "Add who referred you.",
            ),
        )

        composeRule.onNodeWithText("Tell the clinic what you'd like to be seen for.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Who referred you?").assertIsDisplayed()
        composeRule.onNodeWithText("Add who referred you.").assertIsDisplayed()
    }

    @Test
    fun composedCharacterCount_usesPresetLabelAndDetails() {
        val details = "x".repeat(810)
        setReason(
            reasonState(type = presetType).copy(
                reasonChoice = VisitReasonChoice.Preset(21),
                reason = details,
            ),
        )

        composeRule.onNodeWithText("${"Blurred or reduced vision: $details".length} of 1000 characters")
            .assertIsDisplayed()
    }

    @Test
    fun longPresetLabel_remainsVisibleWithoutTruncation() {
        val label = "Blurred or reduced vision " + "with additional context ".repeat(10)
        setReason(
            reasonState(
                type = noPresetType.copy(
                    visitReasonPresets = listOf(VisitReasonPreset(id = 21, label = label)),
                ),
            ),
        )

        composeRule.onNodeWithText(label).assertIsDisplayed()
    }

    private fun setReason(state: RequestStep.Reason) {
        composeRule.setContent {
            EyecareTheme {
                RequestReasonContent(
                    state = state,
                    onReasonChange = {},
                    onReasonChoiceChange = {},
                    onReferringSourceChange = {},
                    onConfirm = {},
                    onBack = {},
                )
            }
        }
    }

    private fun reasonState(type: AppointmentType) = RequestStep.Reason(
        selectedType = type,
        identityRequired = false,
        date = "2026-08-10",
        primarySlot = slot,
        alternativeSlots = emptyList(),
    )

    companion object {
        private val noPresetType = AppointmentType(
            id = 1,
            name = "First eye examination",
            description = null,
            durationMinutes = 45,
            requiresReferral = false,
        )

        private val presetType = noPresetType.copy(
            visitReasonPresets = listOf(
                VisitReasonPreset(id = 21, label = "Blurred or reduced vision"),
                VisitReasonPreset(id = 22, label = "Eye pain or discomfort"),
            ),
        )

        private val presetReferralType = presetType.copy(requiresReferral = true)

        private val slot = AvailabilitySlot(
            startsAt = "2026-08-10T09:00:00+08:00",
            endsAt = "2026-08-10T09:45:00+08:00",
            available = true,
            reason = null,
        )
    }
}
