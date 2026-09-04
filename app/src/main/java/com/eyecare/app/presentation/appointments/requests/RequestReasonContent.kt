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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold
import com.eyecare.app.ui.theme.EyecareColors

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
    onFocusHandled: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    val presets = state.selectedType.visitReasonPresets
    val hasPresets = presets.isNotEmpty()
    val choiceError = state.reasonErrorCode == VisitReasonCompositionError.CHOICE_REQUIRED ||
        state.reasonErrorCode == VisitReasonCompositionError.PRESET_UNAVAILABLE

    val scrollState = rememberScrollState()
    val chipsAnchor = remember { BringIntoViewRequester() }
    val reasonFieldAnchor = remember { BringIntoViewRequester() }
    val referralAnchor = remember { BringIntoViewRequester() }

    // A failed Continue scrolls to whichever error actually applies, since (unlike Identity's
    // single top-of-screen summary) this step's errors sit next to their own field.
    LaunchedEffect(state.focusOnError) {
        if (!state.focusOnError) return@LaunchedEffect
        when {
            choiceError -> chipsAnchor.bringIntoView()
            state.reasonError != null -> reasonFieldAnchor.bringIntoView()
            state.referringSourceError != null -> referralAnchor.bringIntoView()
        }
        onFocusHandled()
    }

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
                .verticalScroll(scrollState)
                .padding(horizontal = RequestStepMargin),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "What would you like to be seen for?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() },
            )

            if (hasPresets) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(chipsAnchor)
                            .semantics { selectableGroup() },
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
                                colors = reasonChipColors(),
                            )
                        }
                        FilterChip(
                            selected = state.reasonChoice == VisitReasonChoice.Other,
                            onClick = { onReasonChoiceChange(VisitReasonChoice.Other) },
                            label = { Text("Other") },
                            modifier = Modifier.heightIn(min = 48.dp),
                            colors = reasonChipColors(),
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
                    // Count the value that Review and the request body will actually use. For an
                    // invalid selection, retain the best available length while the patient fixes
                    // the choice so the feedback never hides their draft.
                    val reasonLength = composedReasonLength(state)
                    // Live feedback the moment typing crosses the limit, rather than only after a
                    // failed Continue — the field never silently clamps what the patient types.
                    val overLimit = reasonLength > reasonSoftLimit
                    val liveOverLimitMessage = "Please shorten this to $reasonSoftLimit characters or fewer."
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = onReasonChange,
                        label = { Text(fieldLabel) },
                        placeholder = { Text(fieldPlaceholder) },
                        // Grows with the content instead of clipping it at a fixed height, which
                        // matters most at large system font sizes.
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp)
                            .bringIntoViewRequester(reasonFieldAnchor),
                        isError = (state.reasonError != null && !choiceError) || overLimit,
                        supportingText = if (!choiceError) {
                            (state.reasonError ?: liveOverLimitMessage.takeIf { overLimit })
                                ?.let { error -> { Text(error) } }
                        } else {
                            null
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                        ),
                        maxLines = 6,
                    )
                    if (reasonLength > reasonSoftLimit * 4 / 5) {
                        Text(
                            text = "$reasonLength of $reasonSoftLimit characters",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (overLimit) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }

            if (state.selectedType.requiresReferral) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${state.selectedType.name} needs a referral",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.semantics { heading() },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .bringIntoViewRequester(referralAnchor),
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

@Composable
private fun reasonChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = EyecareColors.current.accentText,
    selectedLeadingIconColor = EyecareColors.current.accentText,
)

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
