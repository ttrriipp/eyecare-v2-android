package com.eyecare.app.presentation.account

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.eyecare.app.domain.model.LinkState
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkRequest
import com.eyecare.app.presentation.auth.components.AuthIntro
import com.eyecare.app.presentation.auth.components.AuthOutlinedButton
import com.eyecare.app.presentation.auth.components.AuthPrimaryButton
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpExpiryRow
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun LimitedAccountScreen(
    account: PatientAccount,
    onBack: () -> Unit,
    onLinkComplete: () -> Unit,
    requestedFeatureLabel: String? = null,
    viewModel: LimitedAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(account) {
        viewModel.load(account)
    }

    when (val s = state) {
        is LimitedAccountState.Overview -> LimitedOverviewContent(
            linkState = s.linkState,
            currentLinkRequest = s.currentLinkRequest,
            isSubmittingLinkRequest = s.isSubmittingLinkRequest,
            requestError = s.requestError,
            requestedFeatureLabel = requestedFeatureLabel,
            onBack = onBack,
            onEnterInvite = { viewModel.startInvitationEntry() },
            onRequestClinicLink = { viewModel.submitClinicLinkRequest() },
        )
        is LimitedAccountState.EnterInvitationCode -> LimitedInviteCodeStep(s, viewModel)
        is LimitedAccountState.VerifyInvitationOtp -> LimitedInviteOtpStep(s, viewModel)
        is LimitedAccountState.Linked -> {
            LaunchedEffect(s.account.id) {
                onLinkComplete()
            }
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
internal fun LimitedOverviewContent(
    linkState: LinkState?,
    currentLinkRequest: PatientLinkRequest?,
    isSubmittingLinkRequest: Boolean,
    requestError: String?,
    requestedFeatureLabel: String?,
    onBack: () -> Unit,
    onEnterInvite: () -> Unit,
    onRequestClinicLink: () -> Unit,
) {
    val hasPendingClinicRequest = currentLinkRequest != null || linkState is LinkState.PendingReview

    AuthStepScaffold(title = "Link your care record", onBack = onBack, showGradientBar = false) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = requestedFeatureLabel?.let { "Link your clinic record to open $it." }
                    ?: "Connect this account to your clinic record to unlock patient features.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            ConnectAccountCard(
                showBothMethods = !hasPendingClinicRequest,
                pendingRequestNumber = currentLinkRequest?.requestNumber,
                isSubmittingLinkRequest = isSubmittingLinkRequest,
                requestError = requestError,
                onEnterInvite = onEnterInvite,
                onRequestClinicLink = onRequestClinicLink,
            )
        }
    }
}

private enum class LinkMethod { INVITATION_CODE, CLINIC_REQUEST }

@Composable
private fun ConnectAccountCard(
    showBothMethods: Boolean,
    pendingRequestNumber: String?,
    isSubmittingLinkRequest: Boolean,
    requestError: String?,
    onEnterInvite: () -> Unit,
    onRequestClinicLink: () -> Unit,
) {
    var selectedMethod by remember(showBothMethods) { mutableStateOf(LinkMethod.INVITATION_CODE) }

    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Connect your account",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            if (showBothMethods) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    LinkMethod.entries.forEachIndexed { index, method ->
                        SegmentedButton(
                            selected = selectedMethod == method,
                            onClick = { selectedMethod = method },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = LinkMethod.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                activeContentColor = EyecareColors.current.accentText,
                                activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            ),
                            label = {
                                Text(
                                    if (method == LinkMethod.INVITATION_CODE) "Invitation code" else "Ask clinic",
                                )
                            },
                        )
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = EyecareColors.current.accentText,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "Clinic review pending" + (pendingRequestNumber?.let { " · $it" } ?: ""),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Crossfade(
                targetState = if (showBothMethods) selectedMethod else LinkMethod.INVITATION_CODE,
                label = "link-method",
            ) { method ->
                when (method) {
                    LinkMethod.INVITATION_CODE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (showBothMethods) {
                                "Enter the code from your clinic to link your account right away."
                            } else {
                                "You can still enter an invitation code while the clinic reviews your request."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AuthPrimaryButton(
                            text = "Enter invitation code",
                            onClick = onEnterInvite,
                        )
                    }
                    LinkMethod.CLINIC_REQUEST -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "The clinic reviews your account and links it to your record. This can take longer than an invitation code.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        AuthOutlinedButton(
                            text = "Request clinic review",
                            onClick = onRequestClinicLink,
                            enabled = !isSubmittingLinkRequest,
                            loading = isSubmittingLinkRequest,
                        )
                        FieldError(requestError)
                    }
                }
            }
        }
    }
}

@Composable
private fun LimitedInviteCodeStep(
    state: LimitedAccountState.EnterInvitationCode,
    viewModel: LimitedAccountViewModel,
) {
    AuthStepScaffold(title = "Invitation code", onBack = { viewModel.back() }, showGradientBar = false) {
        AuthIntro("Enter the invitation code from your clinic. We'll verify it before linking your account.")
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = state.code,
            onValueChange = { viewModel.updateInvitationCode(it) },
            label = { Text("Invitation code") },
            singleLine = true,
            isError = !state.error.isNullOrBlank(),
            modifier = Modifier.fillMaxWidth(),
        )
        FieldError(state.error)
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Continue",
            onClick = viewModel::requestInvitationOtp,
            enabled = state.code.isNotBlank(),
        )
    }
}

@Composable
private fun LimitedInviteOtpStep(
    state: LimitedAccountState.VerifyInvitationOtp,
    viewModel: LimitedAccountViewModel,
) {
    AuthStepScaffold(title = "Verify code", onBack = { viewModel.back() }, showGradientBar = false) {
        AuthIntro("Enter the 6-digit verification code sent to your phone.")
        Spacer(modifier = Modifier.height(20.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateOtpCode(it) },
            error = state.error,
            enabled = !state.isResending,
        )
        OtpExpiryRow(
            expiresAt = state.expiresAt,
            canResend = !state.isResending,
            onResend = viewModel::resendInvitationOtp,
        )
        if (state.isResending) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.height(18.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sending a new code…")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        AuthPrimaryButton(
            text = "Verify code",
            onClick = viewModel::verifyInvitationOtp,
            enabled = state.code.length == 6 && !state.isResending,
            loading = state.isResending,
        )
    }
}
