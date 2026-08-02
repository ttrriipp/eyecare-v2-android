package com.eyecare.app.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.domain.model.PatientLinkStatus
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField

@Composable
fun LimitedAccountScreen(
    account: PatientAccount,
    onBack: () -> Unit,
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
            linkState = s.linkState,
            currentLinkRequest = s.currentLinkRequest,
            isSubmittingLinkRequest = s.isSubmittingLinkRequest,
            requestError = s.requestError,
            onBack = onBack,
            onEnterInvite = { viewModel.startInvitationEntry() },
            onRequestClinicLink = { viewModel.submitClinicLinkRequest() },
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
    linkState: LinkState?,
    currentLinkRequest: PatientLinkRequest?,
    isSubmittingLinkRequest: Boolean,
    requestError: String?,
    onBack: () -> Unit,
    onEnterInvite: () -> Unit,
    onRequestClinicLink: () -> Unit,
    onAccountSecurity: () -> Unit,
    onSignOut: () -> Unit,
) {
    AuthStepScaffold(title = "Link to clinic", onBack = onBack) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = account.name.ifBlank { "Account" },
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = statusCopy(account.linkStatus, linkState),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            if (currentLinkRequest != null || linkState is LinkState.PendingReview) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Clinic link request pending",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "The clinic can review your account and link it to the right patient record. You can still enter an invitation code if the clinic sends one.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        currentLinkRequest?.requestNumber?.let { requestNumber ->
                            Text(
                                text = requestNumber,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onEnterInvite,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Enter invitation code")
            }
            if (currentLinkRequest == null && linkState !is LinkState.PendingReview) {
                OutlinedButton(
                    onClick = onRequestClinicLink,
                    enabled = !isSubmittingLinkRequest,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isSubmittingLinkRequest) {
                        CircularProgressIndicator(modifier = Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Ask clinic to link me")
                    }
                }
                FieldError(requestError)
            }
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

private fun statusCopy(status: PatientLinkStatus, linkState: LinkState?): String = when {
    linkState is LinkState.PendingReview -> "Clinic review pending."
    status == PatientLinkStatus.UNLINKED -> "Your account is not linked to a clinic record yet."
    status == PatientLinkStatus.PENDING_REVIEW -> "Clinic review pending."
    status == PatientLinkStatus.UNKNOWN -> "Account status unknown."
    status == PatientLinkStatus.LINKED -> ""
    else -> "Your account is not linked to a clinic record yet."
}
