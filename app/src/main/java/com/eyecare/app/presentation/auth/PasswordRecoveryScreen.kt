package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField

@Composable
fun PasswordRecoveryScreen(
    onRecoverySuccess: () -> Unit,
    onBack: () -> Unit,
    viewModel: PasswordRecoveryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is RecoveryState.EnterPhone -> RecoveryPhoneStep(s, viewModel, onBack)
        is RecoveryState.EnterOtp -> RecoveryOtpStep(s, viewModel)
        is RecoveryState.EnterNewPassword -> RecoveryPasswordStep(s, viewModel)
        is RecoveryState.Success -> {
            onRecoverySuccess()
        }
    }
}

@Composable
private fun RecoveryPhoneStep(
    state: RecoveryState.EnterPhone,
    viewModel: PasswordRecoveryViewModel,
    onBack: () -> Unit,
) {
    AuthStepScaffold(title = "Reset password", onBack = onBack) {
        Text(
            text = "Enter your phone number. If it matches an account, we'll send a verification code.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = "+63",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                modifier = Modifier.width(72.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = state.phoneNumber,
                onValueChange = { raw ->
                    val filtered = raw.filter { it.isDigit() }.take(10)
                    viewModel.updatePhone(filtered)
                },
                label = { Text("Phone number") },
                placeholder = { Text("9171234567") },
                singleLine = true,
                isError = state.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.weight(1f),
            )
        }
        FieldError(state.error)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.requestOtp() },
            enabled = state.phoneNumber.length >= 10,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Send code")
        }
    }
}

@Composable
private fun RecoveryOtpStep(
    state: RecoveryState.EnterOtp,
    viewModel: PasswordRecoveryViewModel,
) {
    AuthStepScaffold(title = "Enter code", onBack = { viewModel.back() }) {
        Text(
            text = "Enter the 6-digit verification code.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateOtpCode(it) },
            error = state.error,
        )
        OtpExpiryRow(expiresAt = state.expiresAt, canResend = false, onResend = {})
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verifyOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun RecoveryPasswordStep(
    state: RecoveryState.EnterNewPassword,
    viewModel: PasswordRecoveryViewModel,
) {
    AuthStepScaffold(title = "New password", onBack = { viewModel.back() }) {
        Text(
            text = "Your new password must be at least 12 characters. Other devices will be signed out.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = state.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = "New password",
            error = state.errors["password"],
        )
        PasswordField(
            value = state.passwordConfirmation,
            onValueChange = { viewModel.updatePasswordConfirmation(it) },
            label = "Confirm new password",
            error = state.errors["passwordConfirmation"],
        )
        FieldError(state.errors["_"])
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.resetPassword() },
            enabled = state.password.isNotBlank() && state.passwordConfirmation.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Reset password")
        }
    }
}
