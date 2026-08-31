package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.VisitReasonPreset
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VisitReasonSelectionTest {

    private val presets = listOf(
        VisitReasonPreset(id = 21, label = "Blurred or reduced vision"),
        VisitReasonPreset(id = 22, label = "Eye pain or discomfort"),
    )

    @Test
    fun `preset without details uses the preset label`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 21),
            presets = presets,
            details = "",
        )

        assertEquals(VisitReasonComposition.Valid("Blurred or reduced vision"), result)
    }

    @Test
    fun `preset details are trimmed and appended with a colon`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 21),
            presets = presets,
            details = "  mostly in my left eye  ",
        )

        assertEquals(
            VisitReasonComposition.Valid("Blurred or reduced vision: mostly in my left eye"),
            result,
        )
    }

    @Test
    fun `other uses the exact trimmed custom description`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Other,
            presets = presets,
            details = "  Headaches after reading  ",
        )

        assertEquals(VisitReasonComposition.Valid("Headaches after reading"), result)
    }

    @Test
    fun `no choice is valid as free text when the type has no presets`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.None,
            presets = emptyList(),
            details = "  Routine eye check  ",
        )

        assertEquals(VisitReasonComposition.Valid("Routine eye check"), result)
    }

    @Test
    fun `a type with presets requires an explicit choice`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.None,
            presets = presets,
            details = "Routine eye check",
        )

        assertEquals(VisitReasonComposition.Invalid(VisitReasonCompositionError.CHOICE_REQUIRED), result)
    }

    @Test
    fun `other rejects a blank custom description`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Other,
            presets = presets,
            details = "   ",
        )

        assertEquals(VisitReasonComposition.Invalid(VisitReasonCompositionError.REASON_REQUIRED), result)
    }

    @Test
    fun `unresolved preset is rejected instead of submitting an empty label`() {
        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 999),
            presets = presets,
            details = "some details",
        )

        assertEquals(
            VisitReasonComposition.Invalid(VisitReasonCompositionError.PRESET_UNAVAILABLE),
            result,
        )
    }

    @Test
    fun `duplicate labels do not change ID based selection`() {
        val duplicateLabels = listOf(
            VisitReasonPreset(id = 21, label = "Eye pain"),
            VisitReasonPreset(id = 22, label = "Eye pain"),
        )

        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 22),
            presets = duplicateLabels,
            details = "right eye",
        )

        assertEquals(VisitReasonComposition.Valid("Eye pain: right eye"), result)
    }

    @Test
    fun `exactly 1000 composed characters are accepted`() {
        val label = "A".repeat(255)
        val details = "B".repeat(743)

        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 1),
            presets = listOf(VisitReasonPreset(id = 1, label = label)),
            details = details,
        )

        assertTrue(result is VisitReasonComposition.Valid)
        assertEquals(1000, (result as VisitReasonComposition.Valid).value.length)
    }

    @Test
    fun `1001 composed characters are rejected`() {
        val label = "A".repeat(255)
        val details = "B".repeat(744)

        val result = composeReasonForVisit(
            choice = VisitReasonChoice.Preset(presetId = 1),
            presets = listOf(VisitReasonPreset(id = 1, label = label)),
            details = details,
        )

        assertEquals(VisitReasonComposition.Invalid(VisitReasonCompositionError.TOO_LONG), result)
    }
}
