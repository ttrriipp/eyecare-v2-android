package com.eyecare.app.presentation.appointments.requests

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eyecare.app.domain.model.AppointmentRequestGender
import com.eyecare.app.presentation.appointments.CLINIC_TIME_ZONE
import com.eyecare.app.presentation.appointments.components.AppointmentPrimaryButton
import com.eyecare.app.presentation.appointments.components.RequestStepMargin
import com.eyecare.app.presentation.appointments.components.RequestStepScaffold
import com.eyecare.app.ui.theme.EyecareColors
import java.time.Instant
import java.time.LocalDate

/**
 * Identity details for accounts with no linked clinic record.
 *
 * These nine fields exist for the clinic's benefit — they are how staff match a request to a
 * patient record — so the screen says that plainly rather than presenting them as a toll. When
 * validation fails it names every problem at once in a summary at the top, because the previous
 * behaviour (errors attached to fields that may be below the fold) made `Continue` look broken.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestIdentityContent(
    state: RequestStep.Identity,
    onEmailChange: (String) -> Unit,
    onFirstNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onGenderChange: (AppointmentRequestGender) -> Unit,
    onOccupationChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onFocusHandled: () -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // A failed submit scrolls back to the summary so the outcome is never off-screen.
    LaunchedEffect(state.focusField) {
        if (state.focusField != null) {
            scrollState.animateScrollTo(0)
            onFocusHandled()
        }
    }

    RequestStepScaffold(
        title = "Your details",
        stepLabels = requestStepLabels(identityRequired = true),
        currentStep = requestStepIndex(RequestStepId.IDENTITY, identityRequired = true),
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
                text = "The clinic uses these to match your request to your patient record.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.errors.isNotEmpty()) {
                ErrorSummary(count = state.errors.size)
            }

            IdentitySection("Contact") {
                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {},
                    label = { Text("Phone number") },
                    readOnly = true,
                    enabled = false,
                    isError = state.errors.containsKey("phone"),
                    supportingText = {
                        Text(state.errors["phone"] ?: "From your verified account. Can't be changed here.")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.email,
                    onValueChange = onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    isError = state.errors.containsKey("email"),
                    supportingText = state.errors["email"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IdentitySection("Your name") {
                OutlinedTextField(
                    value = state.firstName,
                    onValueChange = onFirstNameChange,
                    label = { Text("First name") },
                    singleLine = true,
                    isError = state.errors.containsKey("firstName"),
                    supportingText = state.errors["firstName"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.middleName,
                    onValueChange = onMiddleNameChange,
                    label = { Text("Middle name (optional)") },
                    singleLine = true,
                    isError = state.errors.containsKey("middleName"),
                    supportingText = state.errors["middleName"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.lastName,
                    onValueChange = onLastNameChange,
                    label = { Text("Last name") },
                    singleLine = true,
                    isError = state.errors.containsKey("lastName"),
                    supportingText = state.errors["lastName"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IdentitySection("About you") {
                DateOfBirthField(
                    value = state.dateOfBirth,
                    error = state.errors["dateOfBirth"],
                    onClick = { showDatePicker = true },
                )
                GenderField(
                    selectedGender = state.gender,
                    error = state.errors["gender"],
                    onGenderChange = onGenderChange,
                )
                OutlinedTextField(
                    value = state.occupation,
                    onValueChange = onOccupationChange,
                    label = { Text("Occupation") },
                    singleLine = true,
                    isError = state.errors.containsKey("occupation"),
                    supportingText = state.errors["occupation"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            IdentitySection("Address") {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = onAddressChange,
                    label = { Text("Home address") },
                    minLines = 3,
                    maxLines = 5,
                    isError = state.errors.containsKey("address"),
                    supportingText = state.errors["address"]?.let { error -> { Text(error) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val today = remember { LocalDate.now(CLINIC_TIME_ZONE) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.dateOfBirth.toDatePickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(CLINIC_TIME_ZONE)
                        .toLocalDate()
                        .isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateOfBirthChange(
                            Instant.ofEpochMilli(millis)
                                .atZone(CLINIC_TIME_ZONE)
                                .toLocalDate()
                                .toString(),
                        )
                    }
                    showDatePicker = false
                }) { Text("Set date") }
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
private fun ErrorSummary(count: Int) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Assertive },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = if (count == 1) {
                    "One field needs your attention. It's marked below."
                } else {
                    "$count fields need your attention. They're marked below."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun IdentitySection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp,
        )
        content()
    }
}

/**
 * The field itself is the tap target. Previously the read-only field ignored taps and a
 * caption-weight text button beneath it was the only way in, which read as broken.
 */
@Composable
private fun DateOfBirthField(
    value: String,
    error: String?,
    onClick: () -> Unit,
) {
    val display = value.takeIf { it.isNotBlank() }?.let { formatRequestDate(it) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append("Date of birth, ")
                    append(display ?: "not set")
                    if (error != null) append(", $error")
                    append(". Double tap to choose a date.")
                }
            },
    ) {
        OutlinedTextField(
            value = display.orEmpty(),
            onValueChange = {},
            label = { Text("Date of birth") },
            placeholder = { Text("Choose a date") },
            readOnly = true,
            enabled = false,
            isError = error != null,
            supportingText = error?.let { message -> { Text(message) } },
            modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
        )
    }
}

/** Same shape and tap behaviour as [DateOfBirthField], so both "choose a value" fields match. */
@Composable
private fun GenderField(
    selectedGender: AppointmentRequestGender?,
    error: String?,
    onGenderChange: (AppointmentRequestGender) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = true }
                .semantics(mergeDescendants = true) {
                    role = Role.DropdownList
                    contentDescription = buildString {
                        append("Gender, ")
                        append(selectedGender?.label ?: "not set")
                        if (error != null) append(", $error")
                        append(". Double tap to choose an option.")
                    }
                },
        ) {
            OutlinedTextField(
                value = selectedGender?.label.orEmpty(),
                onValueChange = {},
                label = { Text("Gender") },
                placeholder = { Text("Choose an option") },
                readOnly = true,
                enabled = false,
                isError = error != null,
                supportingText = error?.let { message -> { Text(message) } },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
            )
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
}
