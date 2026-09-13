package com.eyecare.app.presentation.appointments.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selectableGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.eyecare.app.presentation.appointments.PATIENT_CANCELLATION_REASON_MAX_LENGTH
import com.eyecare.app.ui.theme.EyecareColors

internal sealed interface CancellationReasonChoice {
    data class Preset(val reason: String) : CancellationReasonChoice

    data object Other : CancellationReasonChoice
}

internal val PATIENT_CANCELLATION_REASON_PRESETS = listOf(
    "I need to change my schedule.",
    "I am feeling unwell.",
    "I have another commitment.",
    "I no longer need this appointment.",
)

@Composable
internal fun CancellationReasonPicker(
    choice: CancellationReasonChoice,
    reason: String,
    onChoiceChange: (CancellationReasonChoice) -> Unit,
    onReasonChange: (String) -> Unit,
    enabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Why are you cancelling?",
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = "Choose a reason or select Other to write your own.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { selectableGroup() },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PATIENT_CANCELLATION_REASON_PRESETS.forEach { preset ->
                FilterChip(
                    selected = choice == CancellationReasonChoice.Preset(preset),
                    onClick = {
                        onChoiceChange(CancellationReasonChoice.Preset(preset))
                        onReasonChange(preset)
                    },
                    enabled = enabled,
                    label = { Text(preset.removeSuffix(".")) },
                    modifier = Modifier.heightIn(min = 44.dp),
                    colors = cancellationReasonChipColors(),
                )
            }
            FilterChip(
                selected = choice == CancellationReasonChoice.Other,
                onClick = {
                    onChoiceChange(CancellationReasonChoice.Other)
                    onReasonChange("")
                },
                enabled = enabled,
                label = { Text("Other") },
                modifier = Modifier.heightIn(min = 44.dp),
                colors = cancellationReasonChipColors(),
            )
        }

        if (choice == CancellationReasonChoice.Other) {
            OutlinedTextField(
                value = reason,
                onValueChange = { value ->
                    if (value.length <= PATIENT_CANCELLATION_REASON_MAX_LENGTH) {
                        onReasonChange(value)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                enabled = enabled,
                label = { Text("Cancellation reason") },
                placeholder = { Text("Tell the clinic why you need to cancel") },
                minLines = 3,
                maxLines = 5,
                supportingText = {
                    Text("${reason.length}/$PATIENT_CANCELLATION_REASON_MAX_LENGTH")
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                ),
            )
        }
    }
}

@Composable
private fun cancellationReasonChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    selectedLabelColor = EyecareColors.current.accentText,
    selectedLeadingIconColor = EyecareColors.current.accentText,
)
