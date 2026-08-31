package com.eyecare.app.presentation.appointments.requests

import com.eyecare.app.domain.model.VisitReasonPreset

internal const val maxReasonForVisitLength = 1000

sealed interface VisitReasonChoice {
    data object None : VisitReasonChoice
    data class Preset(val presetId: Int) : VisitReasonChoice
    data object Other : VisitReasonChoice
}

enum class VisitReasonCompositionError {
    CHOICE_REQUIRED,
    PRESET_UNAVAILABLE,
    REASON_REQUIRED,
    TOO_LONG,
}

sealed interface VisitReasonComposition {
    data class Valid(val value: String) : VisitReasonComposition
    data class Invalid(val error: VisitReasonCompositionError) : VisitReasonComposition
}

fun composeReasonForVisit(
    choice: VisitReasonChoice,
    presets: List<VisitReasonPreset>,
    details: String,
): VisitReasonComposition {
    val trimmedDetails = details.trim()
    val composed = when (choice) {
        VisitReasonChoice.None -> {
            if (presets.isNotEmpty()) {
                return VisitReasonComposition.Invalid(VisitReasonCompositionError.CHOICE_REQUIRED)
            }
            trimmedDetails
        }

        VisitReasonChoice.Other -> trimmedDetails

        is VisitReasonChoice.Preset -> {
            val label = presets.firstOrNull { it.id == choice.presetId }?.label?.trim()
                ?: return VisitReasonComposition.Invalid(
                    VisitReasonCompositionError.PRESET_UNAVAILABLE,
                )
            if (label.isBlank()) {
                return VisitReasonComposition.Invalid(
                    VisitReasonCompositionError.PRESET_UNAVAILABLE,
                )
            }
            if (trimmedDetails.isBlank()) label else "$label: $trimmedDetails"
        }
    }

    return when {
        composed.isBlank() -> VisitReasonComposition.Invalid(VisitReasonCompositionError.REASON_REQUIRED)
        composed.length > maxReasonForVisitLength ->
            VisitReasonComposition.Invalid(VisitReasonCompositionError.TOO_LONG)
        else -> VisitReasonComposition.Valid(composed)
    }
}
