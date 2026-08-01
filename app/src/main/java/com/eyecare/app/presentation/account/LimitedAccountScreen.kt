package com.eyecare.app.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField

@Composable
fun LimitedAccountScreen(
    account: PatientAccount,
    onAccountSecurity: () -> Unit,
    onSignOut: () -> Unit,
    onNavigateToMain: () -> Unit,
    viewModel: LimitedAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(account) {
        viewModel.load(account)
    }

    when (val s = state) {
        is LimitedAccountState.Overview -> LimitedOverviewContent(
            account = s.account,
            onEnterInvite = { viewModel.startInvitationEntry() },
            onAccountSecurity = onAccountSecurity,
            onSignOut = onSignOut,
        )
        is LimitedAccountState.EnterInvitationCode -> LimitedInviteCodeStep(s, viewModel)
        is LimitedAccountState.VerifyInvitationOtp -> LimitedInviteOtpStep(s, viewModel)
        is LimitedAccountState.Linked -> {
            onNavigateToMain()
        }
        is LimitedAccountState.Error -> {
            Scaffold { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(s.message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.load(account) }) { Text("Retry") }
                }
            }
        }
    }
}

@Composable
private fun LimitedOverviewContent(
    account: PatientAccount,
    onEnterInvite: () -> Unit,
    onAccountSecurity: () -> Unit,
    onSignOut: () -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = account.name.ifBlank { "Account" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = statusCopy(account.linkStatus),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onEnterInvite,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enter invitation code")
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAccountSecurity,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Account & Security")
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text("Sign out")
            }
        }
    }
}

@Composable
private fun LimitedInviteCodeStep(
    state: LimitedAccountState.EnterInvitationCode,
    viewModel: LimitedAccountViewModel,
) {
    AuthStepScaffold(title = "Invitation code", onBack = { viewModel.back() }) {
        Text(
            text = "Enter your invitation code to link your account.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.code,
            onValueChange = { viewModel.updateInvitationCode(it) },
            label = { Text("Invitation code") },
            singleLine = true,
            isError = state.error != null,
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(state.error)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.requestInvitationOtp() },
            enabled = state.code.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Continue")
        }
    }
}

@Composable
private fun LimitedInviteOtpStep(
    state: LimitedAccountState.VerifyInvitationOtp,
    viewModel: LimitedAccountViewModel,
) {
    AuthStepScaffold(title = "Verify code", onBack = { viewModel.back() }) {
        Text(
            text = "Enter the verification code sent to your contact.",
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
            onClick = { viewModel.verifyInvitationOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Verify")
        }
    }
}

private fun statusCopy(status: PatientLinkStatus): String = when (status) {
    PatientLinkStatus.UNLINKED -> "Your account is not linked to a clinic record yet."
    PatientLinkStatus.PENDING_REVIEW -> "Clinic review pending. You can still enter an invitation code."
    PatientLinkStatus.UNKNOWN -> "Account status unknown."
    PatientLinkStatus.LINKED -> ""
}
