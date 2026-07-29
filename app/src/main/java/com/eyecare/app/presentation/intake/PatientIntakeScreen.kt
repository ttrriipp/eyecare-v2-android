package com.eyecare.app.presentation.intake

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eyecare.app.domain.model.IntakeStatus
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.common.components.ErrorContent

@Composable
fun PatientIntakeScreen(
    onBack: () -> Unit,
    viewModel: PatientIntakeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSubmitDialog by remember { mutableStateOf(false) }

    when (val state = uiState) {
        PatientIntakeUiState.Loading -> {
            Scaffold(topBar = { IntakeTopBar(onBack = onBack) }) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                }
            }
        }
        is PatientIntakeUiState.Error -> {
            Scaffold(topBar = { IntakeTopBar(onBack = onBack) }) { padding ->
                ErrorContent(
                    message = state.message,
                    onRetry = viewModel::retry,
                    modifier = Modifier.fillMaxSize().padding(padding),
                )
            }
        }
        is PatientIntakeUiState.Success -> {
            val isDraft = state.intake?.status == IntakeStatus.DRAFT || state.intake == null
            val isSubmitted = state.intake?.status == IntakeStatus.SUBMITTED
            val isVerified = state.intake?.status == IntakeStatus.VERIFIED

            Scaffold(
                topBar = {
                    IntakeTopBar(
                        onBack = onBack,
                        title = when {
                            isVerified -> "Intake (Verified)"
                            isSubmitted -> "Intake (Submitted)"
                            else -> "Intake"
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (isDraft) {
                        IntakeFormFields(
                            draft = state.draft,
                            fieldErrors = state.fieldErrors,
                            enabled = !state.isSaving,
                            onChiefComplaintChange = { viewModel.updateDraft { d -> d.copy(chiefComplaint = it) } },
                            onPastOcularHistoryChange = { viewModel.updateDraft { d -> d.copy(pastOcularHistory = it) } },
                            onPastSurgicalHistoryChange = { viewModel.updateDraft { d -> d.copy(pastSurgicalHistory = it) } },
                            onPastMedicalHistoryChange = { viewModel.updateDraft { d -> d.copy(pastMedicalHistory = it) } },
                            onAllergiesChange = { viewModel.updateDraft { d -> d.copy(allergies = it) } },
                            onMedicationsChange = { viewModel.updateDraft { d -> d.copy(medications = it) } },
                        )

                        state.saveError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Button(
                            onClick = { viewModel.saveDraft() },
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            if (state.isSaving) CircularProgressIndicator(Modifier.height(18.dp))
                            else Text("Save Draft", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = { showSubmitDialog = true },
                            enabled = !state.isSaving && !state.isSubmitting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            if (state.isSubmitting) CircularProgressIndicator(Modifier.height(18.dp))
                            else Text("Submit Intake", fontWeight = FontWeight.SemiBold)
                        }

                        state.submitError?.let { error ->
                            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        IntakeReadOnlyFields(intake = state.intake!!)
                    }
                }
            }

            if (showSubmitDialog) {
                AppConfirmationDialog(
                    icon = Icons.Outlined.Send,
                    title = "Submit intake?",
                    message = "Once submitted, you cannot edit this intake. Please review your information before submitting.",
                    confirmLabel = "Submit",
                    dismissLabel = "Review",
                    onConfirm = {
                        showSubmitDialog = false
                        viewModel.submitIntake()
                    },
                    onDismissRequest = { showSubmitDialog = false },
                )
            }
        }
    }
}

@Composable
private fun IntakeFormFields(
    draft: IntakeDraft,
    fieldErrors: Map<String, List<String>>,
    enabled: Boolean,
    onChiefComplaintChange: (String) -> Unit,
    onPastOcularHistoryChange: (String) -> Unit,
    onPastSurgicalHistoryChange: (String) -> Unit,
    onPastMedicalHistoryChange: (String) -> Unit,
    onAllergiesChange: (String) -> Unit,
    onMedicationsChange: (String) -> Unit,
) {
    Text("Clinical History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(
        "Your demographic information from your profile will be included automatically.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(Modifier.height(8.dp))

    IntakeTextField("Chief complaint", draft.chiefComplaint, onChiefComplaintChange, enabled, fieldErrors["chief_complaint"], singleLine = false)
    IntakeTextField("Past ocular history", draft.pastOcularHistory, onPastOcularHistoryChange, enabled, fieldErrors["past_ocular_history"], singleLine = false)
    IntakeTextField("Past surgical history", draft.pastSurgicalHistory, onPastSurgicalHistoryChange, enabled, fieldErrors["past_surgical_history"], singleLine = false)
    IntakeTextField("Past medical history", draft.pastMedicalHistory, onPastMedicalHistoryChange, enabled, fieldErrors["past_medical_history"], singleLine = false)
    IntakeTextField("Allergies", draft.allergies, onAllergiesChange, enabled, fieldErrors["allergies"], singleLine = false)
    IntakeTextField("Medications", draft.medications, onMedicationsChange, enabled, fieldErrors["medications"], singleLine = false)
}

@Composable
private fun IntakeTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    errors: List<String>?,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        isError = errors != null,
        supportingText = errors?.firstOrNull()?.let { { Text(it) } },
        singleLine = singleLine,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
    )
}

@Composable
private fun IntakeReadOnlyFields(
    intake: com.eyecare.app.domain.model.PatientIntake,
) {
    Text("Demographics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    ReadOnlyField("Full name", intake.fullName)
    ReadOnlyField("Date of birth", intake.dateOfBirth)
    ReadOnlyField("Gender", intake.gender)
    ReadOnlyField("Occupation", intake.occupation)
    ReadOnlyField("Address", intake.address)
    ReadOnlyField("Phone", intake.phone)
    ReadOnlyField("Email", intake.email)

    Spacer(Modifier.height(8.dp))
    Text("Clinical History", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    ReadOnlyField("Chief complaint", intake.chiefComplaint)
    ReadOnlyField("Past ocular history", intake.pastOcularHistory)
    ReadOnlyField("Past surgical history", intake.pastSurgicalHistory)
    ReadOnlyField("Past medical history", intake.pastMedicalHistory)
    ReadOnlyField("Allergies", intake.allergies)
    ReadOnlyField("Medications", intake.medications)
}

@Composable
private fun ReadOnlyField(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntakeTopBar(onBack: () -> Unit, title: String = "Intake") {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
    )
}
