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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.domain.model.PatientLinkStatus

@Composable
fun LimitedAccountScreen(
    account: PatientAccount,
    onAccountSecurity: () -> Unit,
    onSignOut: () -> Unit,
    onInvitationCode: (String) -> Unit,
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = account.name,
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

private fun statusCopy(status: PatientLinkStatus): String = when (status) {
    PatientLinkStatus.UNLINKED -> "Your account is not linked to a clinic record yet."
    PatientLinkStatus.PENDING_REVIEW -> "Clinic review pending"
    PatientLinkStatus.UNKNOWN -> "Account status unknown"
    PatientLinkStatus.LINKED -> ""
}
