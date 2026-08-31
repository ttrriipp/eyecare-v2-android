package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold

private const val reasonSoftLimit = maxReasonForVisitLength

/**
 * The reason step. Split out from identity so the two never share a screen: this one is short
 * enough that a patient with a symptom can finish it in a single thought, and it is the only
 * part of the request the clinic genuinely cannot proceed without.
 */
@Composable
fun RequestReasonContent(
    state: RequestStep.Reason,
    onReasonChange: (String) -> Unit,
    onReasonChoiceChange: (VisitReasonChoice) -> Unit,
    onReferringSourceChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    RequestStepScaffold(
        title = "Reason for visit",
        stepLabels = requestStepLabels(state.identityRequired),
        currentStep = requestStepIndex(RequestStepId.REASON, state.identityRequired),
        onBack = onBack,
        bottomBar = {
            AppointmentPrimaryButton(text = "Continue", onClick = onConfirm)
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = RequestStepMargin),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "What would you like to be seen for?",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "A short description helps the clinic prepare for your visit " +
                    "and decide how much time you need.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val presets = state.selectedType.visitReasonPresets
            val hasPresets = presets.isNotEmpty()
            val choiceError = state.reasonErrorCode == VisitReasonCompositionError.CHOICE_REQUIRED ||
                state.reasonErrorCode == VisitReasonCompositionError.PRESET_UNAVAILABLE

            if (hasPresets) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Common reasons",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = if (state.reasonChoice == VisitReasonChoice.None) {
                            "Choose a common reason, or Other if none fit."
                        } else {
                            "Choose the closest match, then add details if helpful."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = state.reasonChoice == VisitReasonChoice.Preset(preset.id),
                                onClick = {
                                    onReasonChoiceChange(VisitReasonChoice.Preset(preset.id))
                                },
                                label = { Text(preset.label) },
                                modifier = Modifier.heightIn(min = 48.dp),
                            )
                        }
                        FilterChip(
                            selected = state.reasonChoice == VisitReasonChoice.Other,
                            onClick = { onReasonChoiceChange(VisitReasonChoice.Other) },
                            label = { Text("Other") },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                    if (choiceError) {
                        state.reasonError?.let { error ->
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            val showReasonField = !hasPresets || state.reasonChoice != VisitReasonChoice.None
            if (showReasonField) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val fieldLabel = when {
                        !hasPresets -> "Reason for visit"
                        state.reasonChoice is VisitReasonChoice.Preset -> "Add details (optional)"
                        else -> "Describe your reason for visit"
                    }
                    val fieldPlaceholder = when {
                        !hasPresets -> "e.g. Blurred vision in my left eye for two weeks"
                        state.reasonChoice is VisitReasonChoice.Preset ->
                            "e.g. Mostly in my left eye for two weeks"
                        else -> "e.g. Headaches when reading"
                    }
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = onReasonChange,
                        label = { Text(fieldLabel) },
                        placeholder = { Text(fieldPlaceholder) },
                        // Grows with the content instead of clipping it at a fixed height, which
                        // matters most at large system font sizes.
                        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                        isError = state.reasonError != null && !choiceError,
                        supportingText = if (!choiceError) {
                            state.reasonError?.let { error -> { Text(error) } }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        maxLines = 6,
                    )
                    // Count the value that Review and the request body will actually use. For an
                    // invalid selection, retain the best available length while the patient fixes
                    // the choice so the feedback never hides their draft.
                    val reasonLength = composedReasonLength(state)
                    if (reasonLength > reasonSoftLimit * 4 / 5) {
                        Text(
                            text = "$reasonLength of $reasonSoftLimit characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (!showReasonField && !choiceError) {
                Text(
                    text = "Select a common reason or Other to continue.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.selectedType.requiresReferral) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${state.selectedType.name} needs a referral",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Tell the clinic who sent you, so they can confirm your referral.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = state.referringSource,
                        onValueChange = onReferringSourceChange,
                        label = { Text("Who referred you?") },
                        placeholder = { Text("e.g. Dr. Santos, Manila General") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = state.referringSourceError != null,
                        supportingText = state.referringSourceError?.let { error -> { Text(error) } },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                        ),
                        singleLine = true,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

private fun composedReasonLength(state: RequestStep.Reason): Int {
    val composition = composeReasonForVisit(
        choice = state.reasonChoice,
        presets = state.selectedType.visitReasonPresets,
        details = state.reason,
    )
    if (composition is VisitReasonComposition.Valid) return composition.value.length

    val details = state.reason.trim()
    val label = (state.reasonChoice as? VisitReasonChoice.Preset)?.let { choice ->
        state.selectedType.visitReasonPresets
            .firstOrNull { it.id == choice.presetId }
            ?.label
            ?.trim()
    }.orEmpty()
    return if (label.isBlank()) details.length else {
        label.length + if (details.isBlank()) 0 else 2 + details.length
    }
}
