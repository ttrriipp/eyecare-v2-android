package com.eyecare.app.presentation.auth

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.toPhilippineE164
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField
import com.eyecare.app.presentation.auth.components.PolicyConsentRow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is RegistrationState.EnterPhone -> RegisterPhoneStep(viewModel, s, onNavigateToLogin)
        is RegistrationState.VerifyPhoneOtp -> RegisterOtpStep(viewModel, s)
        is RegistrationState.EnterDetails -> RegisterDetailsStep(viewModel, s)
        is RegistrationState.Success -> onRegisterSuccess()
    }
}
@Composable
private fun RegisterPhoneStep(
    viewModel: RegistrationViewModel,
    state: RegistrationState.EnterPhone,
    onNavigateToLogin: () -> Unit,
) {
    AuthStepScaffold(
        title = "Create account",
        onBack = onNavigateToLogin,
    ) {
        Text(
            text = "Start with your phone number. We'll send a verification code before you enter your details.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        ContactField(
            value = state.phoneNumber,
            onValueChange = viewModel::updatePhone,
            method = ContactMethod.PHONE,
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = viewModel::requestPhoneOtp,
            enabled = state.phoneNumber.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send code")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Sign in")
        }
    }
}

@Composable
private fun RegisterOtpStep(
    viewModel: RegistrationViewModel,
    state: RegistrationState.VerifyPhoneOtp,
) {
    AuthStepScaffold(
        title = "Verify your phone",
        onBack = viewModel::back,
    ) {
        Text(
            text = "Enter the 6-digit code sent to ${toPhilippineE164(state.phoneNumber)}.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = viewModel::updateOtpCode,
            error = state.error,
            enabled = !state.isResending,
        )
        OtpExpiryRow(
            expiresAt = state.expiresAt,
            canResend = !state.isResending,
            onResend = viewModel::resendOtp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = viewModel::verifyPhoneOtp,
            enabled = state.code.length == 6 && !state.isResending,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isResending) CircularProgressIndicator() else Text("Verify phone")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegisterDetailsStep(
    viewModel: RegistrationViewModel,
    state: RegistrationState.EnterDetails,
) {
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val registrationZone = remember { ZoneId.of("Asia/Manila") }
    val today = remember { LocalDate.now(registrationZone) }
    val datePickerState = rememberDatePickerState(
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = Instant.ofEpochMilli(utcTimeMillis)
                    .atZone(registrationZone)
                    .toLocalDate()
                return date.isBefore(today)
            }
        },
    )

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

            OutlinedTextField(
                value = state.email,
                onValueChange = { viewModel.updateDetails(email = it) },
                label = { Text("Email (optional)") },
                singleLine = true,
                isError = state.errors.containsKey("email"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            FieldError(state.errors["email"])

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
                onClick = viewModel::submitRegistration,
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
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.ofEpochMilli(millis)
                                .atZone(registrationZone)
                                .toLocalDate()
                            viewModel.updateDetails(dateOfBirth = date.toString())
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
