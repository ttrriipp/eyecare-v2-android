package com.eyecare.app.presentation.appointments.requests

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.eyecare.app.domain.model.AppointmentRequestGender
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val identityDateZone = ZoneId.of("Asia/Manila")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestIdentityContent(
    state: RequestStep.EnterIdentity,
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
    val today = remember { LocalDate.now(identityDateZone) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = state.dateOfBirth.toDatePickerMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(identityDateZone)
                    .toLocalDate()
                return date.isBefore(today)
            }
        },
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Requester details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Add the details of the person requesting this appointment.",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "These details help the clinic match the request to the right patient record.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))

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

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
            Spacer(modifier = Modifier.height(16.dp))
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
                                .atZone(identityDateZone)
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
                    Text("▾")
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
        .atStartOfDay(identityDateZone)
        .toInstant()
        .toEpochMilli()
}.getOrNull()
