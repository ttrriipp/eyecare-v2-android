package com.eyecare.app.presentation.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.toPhilippineLocalDigits
import com.eyecare.app.presentation.auth.components.AuthIntro
import com.eyecare.app.presentation.auth.components.AuthPrimaryButton
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
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
        AuthIntro("Enter your phone number. If it matches an account, we'll send a verification code.")
        Spacer(modifier = Modifier.height(20.dp))
        ContactField(
            value = state.phoneNumber,
            onValueChange = viewModel::updatePhone,
            method = ContactMethod.PHONE,
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Send code",
            onClick = viewModel::requestOtp,
            enabled = toPhilippineLocalDigits(state.phoneNumber).length >= 10,
        )
    }
}

@Composable
private fun RecoveryOtpStep(
    state: RecoveryState.EnterOtp,
    viewModel: PasswordRecoveryViewModel,
) {
    AuthStepScaffold(title = "Enter code", onBack = { viewModel.back() }) {
        AuthIntro("Enter the 6-digit verification code.")
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateOtpCode(it) },
            error = state.error,
            enabled = !state.isResending,
        )
        OtpExpiryRow(
            expiresAt = state.expiresAt,
            canResend = !state.isResending,
            onResend = viewModel::resendOtp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Continue",
            onClick = viewModel::verifyOtp,
            enabled = state.code.length == 6 && !state.isResending,
            loading = state.isResending,
        )
    }
}

@Composable
private fun RecoveryPasswordStep(
    state: RecoveryState.EnterNewPassword,
    viewModel: PasswordRecoveryViewModel,
) {
    AuthStepScaffold(title = "New password", onBack = { viewModel.back() }) {
        AuthIntro("Your new password must be at least 12 characters. Other devices will be signed out.")
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
        AuthPrimaryButton(
            text = "Reset password",
            onClick = viewModel::resetPassword,
            enabled = state.password.isNotBlank() && state.passwordConfirmation.isNotBlank(),
        )
    }
}
