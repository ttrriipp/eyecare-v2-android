package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.WizardStepIndicator
import java.time.Instant
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileAndReasonContent(
    state: RequestStep.Details,
    onReasonChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (AppointmentRequestGender) -> Unit,
    onOccupationChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.dateOfBirth.toDatePickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(CLINIC_TIME_ZONE)
                    .toLocalDate()
                return date.isBefore(today)
            }
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.identityRequired) "Profile & reason" else "Reason for visit") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            WizardStepIndicator(
                currentStep = 1,
                steps = listOf("Schedule", "Details", "Review"),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionLabel("Reason for visit")
                    OutlinedTextField(
                        value = state.reason,
                        onValueChange = onReasonChange,
                        label = { Text("Reason for visit") },
                        placeholder = { Text("e.g., Blurred vision in left eye") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        isError = state.reasonError != null,
                        supportingText = state.reasonError?.let { { Text(it) } },
                        maxLines = 5,
                    )
                    Text(
                        text = "${state.reason.length}/1000",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.identityRequired) {
                    Text(
                        text = "Add your details so the clinic can match this request to your record.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Contact")
                        OutlinedTextField(
                            value = state.phone,
                            onValueChange = {},
                            label = { Text("Phone number *") },
                            readOnly = true,
                            isError = state.errors.containsKey("phone"),
                            supportingText = {
                                Text(state.errors["phone"] ?: "Verified account contact")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.email,
                            onValueChange = onEmailChange,
                            label = { Text("Email (optional)") },
                            singleLine = true,
                            isError = state.errors.containsKey("email"),
                            supportingText = state.errors["email"]?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Personal details")
                        OutlinedTextField(
                            value = state.firstName,
                            onValueChange = onFirstNameChange,
                            label = { Text("First name *") },
                            singleLine = true,
                            isError = state.errors.containsKey("firstName"),
                            supportingText = state.errors["firstName"]?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.middleName,
                            onValueChange = onMiddleNameChange,
                            label = { Text("Middle name (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = state.lastName,
                            onValueChange = onLastNameChange,
                            label = { Text("Last name *") },
                            singleLine = true,
                            isError = state.errors.containsKey("lastName"),
                            supportingText = state.errors["lastName"]?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("About you")
                        OutlinedTextField(
                            value = state.dateOfBirth,
                            onValueChange = {},
                            label = { Text("Date of birth *") },
                            readOnly = true,
                            isError = state.errors.containsKey("dateOfBirth"),
                            supportingText = state.errors["dateOfBirth"]?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(if (state.dateOfBirth.isBlank()) "Select date" else "Change date")
                        }
                        GenderSelector(
                            selectedGender = state.gender,
                            error = state.errors["gender"],
                            onGenderChange = onGenderChange,
                        )
                        OutlinedTextField(
                            value = state.occupation,
                            onValueChange = onOccupationChange,
                            label = { Text("Occupation *") },
                            singleLine = true,
                            isError = state.errors.containsKey("occupation"),
                            supportingText = state.errors["occupation"]?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionLabel("Address")
                        OutlinedTextField(
                            value = state.address,
                            onValueChange = onAddressChange,
                            label = { Text("Home address *") },
                            minLines = 3,
                            maxLines = 4,
                            isError = state.errors.containsKey("address"),
                            supportingText = state.errors["address"]?.let { { Text(it) } },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                AppointmentPrimaryButton(text = "Continue", onClick = onConfirm)
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(CLINIC_TIME_ZONE)
                                .toLocalDate()
                            onDateOfBirthChange(date.toString())
                        }
                        showDatePicker = false
                    },
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp,
    )
}

@Composable
private fun GenderSelector(
    selectedGender: AppointmentRequestGender?,
    error: String?,
    onGenderChange: (AppointmentRequestGender) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.labelLarge,
            color = if (error != null) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (error != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.outline,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(selectedGender?.label ?: "Select gender")
                    Icon(
                        Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                AppointmentRequestGender.entries.forEach { gender ->
                    DropdownMenuItem(
                        text = { Text(gender.label) },
                        onClick = {
                            onGenderChange(gender)
                            expanded = false
                        },
                    )
                }
            }
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
    }
}

private fun String.toDatePickerMillis(): Long? = runCatching {
    LocalDate.parse(this)
        .atStartOfDay(CLINIC_TIME_ZONE)
        .toInstant()
        .toEpochMilli()
}.getOrNull()
