package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

private const val reasonSoftLimit = 1000

/**
 * The reason step. Split out from identity so the two never share a screen: this one is short
 * enough that a patient with a symptom can finish it in a single thought, and it is the only
 * part of the request the clinic genuinely cannot proceed without.
 */
@Composable
fun RequestReasonContent(
    state: RequestStep.Reason,
    onReasonChange: (String) -> Unit,
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

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = state.reason,
                    onValueChange = onReasonChange,
                    label = { Text("Reason for visit") },
                    placeholder = { Text("e.g. Blurred vision in my left eye for two weeks") },
                    // Grows with the content instead of clipping it at a fixed height, which
                    // matters most at large system font sizes.
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                    isError = state.reasonError != null,
                    supportingText = state.reasonError?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                    ),
                    maxLines = 6,
                )
                // Only worth showing once the limit is actually in reach.
                if (state.reason.length > reasonSoftLimit * 4 / 5) {
                    Text(
                        text = "${state.reason.length} of $reasonSoftLimit characters",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
