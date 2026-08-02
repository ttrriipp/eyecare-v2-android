package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {},
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        is SignInState.EnterCredentials -> LoginContactStep(
            state = s,
            viewModel = viewModel,
            onNavigateToRegister = onNavigateToRegister,
            onForgotPassword = onForgotPassword,
        )
        is SignInState.VerifyOtp -> LoginOtpStep(state = s, viewModel = viewModel)
        is SignInState.Success -> {
            onLoginSuccess()
        }
    }
}

@Composable
private fun LoginContactStep(
    state: SignInState.EnterCredentials,
    viewModel: SignInViewModel,
    onNavigateToRegister: () -> Unit,
    onForgotPassword: () -> Unit,
) {
    AuthStepScaffold(title = "Sign in") {
        ContactField(
            value = state.phoneNumber,
            onValueChange = { viewModel.updatePhone(it) },
            method = ContactMethod.PHONE,
        )
        Spacer(modifier = Modifier.height(12.dp))
        PasswordField(
            value = state.password,
            onValueChange = { viewModel.updatePassword(it) },
            label = "Password",
        )
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onForgotPassword) {
            Text("Forgot password?")
        }
        FieldError(state.error)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.signIn() },
            enabled = state.phoneNumber.isNotBlank() && state.password.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Sign in")
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Create one")
        }
    }
}

@Composable
private fun LoginOtpStep(
    state: SignInState.VerifyOtp,
    viewModel: SignInViewModel,
) {
    AuthStepScaffold(
        title = "Verification",
        onBack = { viewModel.back() },
    ) {
        Text(
            text = "If the details match an account, a code was sent.",
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
            onClick = { viewModel.verifyOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isResending) CircularProgressIndicator() else Text("Verify")
        }
    }
}
