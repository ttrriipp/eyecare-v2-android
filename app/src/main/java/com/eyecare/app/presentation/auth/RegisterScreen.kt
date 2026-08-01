package com.eyecare.app.presentation.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
import com.eyecare.app.presentation.auth.components.ContactMethodSelector
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField
import com.eyecare.app.presentation.auth.components.PolicyConsentRow

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is RegistrationState.ChooseMethod -> RegisterMethodStep(viewModel)
        is RegistrationState.EnterContact -> RegisterContactStep(viewModel, s)
        is RegistrationState.VerifyContactOtp -> RegisterOtpStep(viewModel, s)
        is RegistrationState.EnterDetails -> RegisterDetailsStep(viewModel, s)
        is RegistrationState.OptionalSecondary -> RegisterSecondaryStep(viewModel, s)
        is RegistrationState.VerifySecondaryOtp -> RegisterSecondaryOtpStep(viewModel, s)
        is RegistrationState.Success -> {
            onRegisterSuccess()
        }
        is RegistrationState.Error -> {
            AuthStepScaffold(title = "Error") {
                Text(s.message)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.back() }) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun RegisterMethodStep(viewModel: RegistrationViewModel) {
    AuthStepScaffold(title = "Create account") {
        ContactMethodSelector(
            selected = ContactMethod.EMAIL,
            onSelect = { method ->
                viewModel.chooseMethod(if (method == ContactMethod.EMAIL) ContactType.EMAIL else ContactType.PHONE)
            },
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { /* handled by NavGraph back */ }) {
            Text("Already have an account? Sign in")
        }
    }
}

@Composable
private fun RegisterContactStep(viewModel: RegistrationViewModel, state: RegistrationState.EnterContact) {
    AuthStepScaffold(
        title = "Create account",
        onBack = { viewModel.back() },
    ) {
        val method = if (state.method == ContactType.EMAIL) ContactMethod.EMAIL else ContactMethod.PHONE
        ContactField(
            value = state.contactValue,
            onValueChange = { viewModel.updateContactValue(it) },
            method = method,
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.requestContactOtp() },
            enabled = state.contactValue.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun RegisterOtpStep(viewModel: RegistrationViewModel, state: RegistrationState.VerifyContactOtp) {
    AuthStepScaffold(
        title = "Verify your contact",
        onBack = { viewModel.back() },
    ) {
        Text(
            text = "Enter the 6-digit code sent to ${state.contactValue}",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateOtpCode(it) },
            error = state.error,
        )
        OtpExpiryRow(
            expiresAt = state.expiresAt,
            canResend = !state.isResending,
            onResend = { viewModel.resendOtp() },
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verifyContactOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isResending) CircularProgressIndicator() else Text("Verify")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDetailsStep(viewModel: RegistrationViewModel, state: RegistrationState.EnterDetails) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    AuthStepScaffold(title = "Your details") {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = state.firstName,
                onValueChange = { viewModel.updateDetails(firstName = it) },
                label = { Text("First name *") },
                singleLine = true,
                isError = state.errors.containsKey("firstName"),
                modifier = Modifier.fillMaxWidth(),
            )
            FieldError(state.errors["firstName"])

            OutlinedTextField(
                value = state.middleName,
                onValueChange = { viewModel.updateDetails(middleName = it) },
                label = { Text("Middle name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.lastName,
                onValueChange = { viewModel.updateDetails(lastName = it) },
                label = { Text("Last name *") },
                singleLine = true,
                isError = state.errors.containsKey("lastName"),
                modifier = Modifier.fillMaxWidth(),
            )
            FieldError(state.errors["lastName"])

            OutlinedTextField(
                value = state.dateOfBirth,
                onValueChange = {},
                label = { Text("Date of birth *") },
                readOnly = true,
                isError = state.errors.containsKey("dateOfBirth"),
                modifier = Modifier.fillMaxWidth(),
            )
            FieldError(state.errors["dateOfBirth"])
            TextButton(onClick = { showDatePicker = true }) {
                Text(if (state.dateOfBirth.isBlank()) "Select date" else "Change date")
            }

            PasswordField(
                value = state.password,
                onValueChange = { viewModel.updateDetails(password = it) },
                label = "Password *",
                error = state.errors["password"],
            )

            PasswordField(
                value = state.passwordConfirmation,
                onValueChange = { viewModel.updateDetails(passwordConfirmation = it) },
                label = "Confirm password *",
                error = state.errors["passwordConfirmation"],
            )

            OutlinedTextField(
                value = state.invitationCode,
                onValueChange = { viewModel.updateDetails(invitationCode = it) },
                label = { Text("Invitation code (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.policies != null) {
                PolicyConsentRow(
                    label = "I accept the",
                    linkText = "Terms of Service",
                    checked = state.termsAccepted,
                    onCheckedChange = { viewModel.updateDetails(termsAccepted = it) },
                    onLinkClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.policies.termsUrl)))
                    },
                )
                FieldError(state.errors["terms"])

                PolicyConsentRow(
                    label = "I accept the",
                    linkText = "Privacy Policy",
                    checked = state.privacyAccepted,
                    onCheckedChange = { viewModel.updateDetails(privacyAccepted = it) },
                    onLinkClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(state.policies.privacyPolicyUrl)))
                    },
                )
                FieldError(state.errors["privacy"])
            } else if (state.isLoadingPolicies) {
                CircularProgressIndicator()
            } else {
                Text("Could not load policy information. Please try again.")
            }

            FieldError(state.errors["_"])

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.submitRegistration() },
                enabled = state.policies != null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Create account")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val instant = java.time.Instant.ofEpochMilli(millis)
                        val date = instant.atZone(java.time.ZoneId.of("Asia/Manila")).toLocalDate()
                        viewModel.updateDetails(dateOfBirth = date.toString())
                    }
                    showDatePicker = false
                }) { Text("OK") }
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
private fun RegisterSecondaryStep(viewModel: RegistrationViewModel, state: RegistrationState.OptionalSecondary) {
    AuthStepScaffold(title = "Add another sign-in method?") {
        Text(
            text = "Verify your current contact, then verify the new contact.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        val method = if (state.secondaryType == ContactType.EMAIL) ContactMethod.EMAIL else ContactMethod.PHONE
        ContactField(
            value = state.secondaryValue,
            onValueChange = { viewModel.updateSecondaryValue(it) },
            method = method,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { viewModel.startSecondaryVerification() },
                enabled = state.secondaryValue.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) { Text("Verify now") }
            OutlinedButton(
                onClick = { viewModel.skipSecondary() },
                modifier = Modifier.weight(1f),
            ) { Text("Skip for now") }
        }
    }
}

@Composable
private fun RegisterSecondaryOtpStep(viewModel: RegistrationViewModel, state: RegistrationState.VerifySecondaryOtp) {
    AuthStepScaffold(
        title = "Verify new contact",
        onBack = { viewModel.skipSecondary() },
    ) {
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateSecondaryOtp(it) },
            error = state.error,
        )
        OtpExpiryRow(expiresAt = state.expiresAt, canResend = false, onResend = {})
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verifySecondaryOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Verify") }
    }
}
