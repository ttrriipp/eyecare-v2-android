package com.eyecare.app.presentation.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun AccountSecurityScreen(
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadContacts()
    }

    when (val s = state) {
        is AccountSecurityState.Loading -> {
            Scaffold { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        is AccountSecurityState.Overview -> ContactOverviewContent(s, viewModel, onSignedOut, onBack)
        is AccountSecurityState.EnterNewContact -> EnterNewContactContent(s, viewModel)
        is AccountSecurityState.StepUpOtp -> StepUpOtpContent(s, viewModel)
        is AccountSecurityState.AddContactOtp -> AddContactOtpContent(s, viewModel)
        is AccountSecurityState.ChangePassword -> ChangePasswordContent(s, viewModel)
        is AccountSecurityState.Result -> {
            LaunchedEffect(s.message) { viewModel.loadContacts() }
            ContactOverviewContent(
                AccountSecurityState.Overview(contacts = s.contacts),
                viewModel, onSignedOut, onBack,
            )
        }
        is AccountSecurityState.SignedOut -> {
            LaunchedEffect(Unit) { onSignedOut() }
        }
    }
}

@Composable
private fun ContactOverviewContent(
    state: AccountSecurityState.Overview,
    viewModel: AccountSecurityViewModel,
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
) {
    AuthStepScaffold(title = "Account & Security", onBack = onBack) {
        if (state.error != null) {
            Text(state.error, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text("Contacts", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        state.contacts.forEach { contact ->
            ContactRow(
                contact = contact,
                onMakePrimary = { viewModel.startStepUp(StepUpAction.MakePrimary(contact.id)) },
                onRemove = { viewModel.startStepUp(StepUpAction.RemoveContact(contact.id)) },
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(
            onClick = { viewModel.startAddContact() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add contact") }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Security", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.startStepUp(StepUpAction.ChangePassword) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Change password") }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.logout() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
        ) { Text("Sign out this device") }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.logoutAll() },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Sign out all devices") }
    }
}

@Composable
private fun ContactRow(
    contact: AccountContact,
    onMakePrimary: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (contact.type == ContactType.EMAIL) Icons.Default.Email else Icons.Default.Phone,
                contentDescription = null,
                modifier = Modifier.padding(end = 12.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.maskedValue, style = MaterialTheme.typography.bodyLarge)
                Row {
                    if (contact.isPrimary) {
                        Text("Primary", style = MaterialTheme.typography.labelSmall, color = EyecareColors.current.accentText)
                    }
                    if (contact.verifiedAt == null) {
                        Text("Pending", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (!contact.isPrimary && contact.verifiedAt != null) {
                TextButton(onClick = onMakePrimary) { Text("Make primary") }
            }
            if (!contact.isPrimary) {
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
    }
}

@Composable
private fun EnterNewContactContent(
    state: AccountSecurityState.EnterNewContact,
    viewModel: AccountSecurityViewModel,
) {
    AuthStepScaffold(title = "Add contact", onBack = { viewModel.back() }) {
        Text("Choose a contact method and enter the value you'd like to add.")
        Spacer(modifier = Modifier.height(16.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ContactType.entries.forEachIndexed { index, type ->
                SegmentedButton(
                    selected = state.contactType == type,
                    onClick = { viewModel.updateNewContactType(type) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = ContactType.entries.size),
                    label = { Text(if (type == ContactType.EMAIL) "Email" else "Phone") },
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        ContactField(
            value = state.contactValue,
            onValueChange = { viewModel.updateNewContactValue(it) },
            method = if (state.contactType == ContactType.EMAIL) ContactMethod.EMAIL else ContactMethod.PHONE,
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.submitNewContact() },
            enabled = state.contactValue.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}

@Composable
private fun StepUpOtpContent(
    state: AccountSecurityState.StepUpOtp,
    viewModel: AccountSecurityViewModel,
) {
    AuthStepScaffold(title = "Verify it's you", onBack = { viewModel.back() }) {
        Text("A code was sent to ${state.challenge.maskedContact}.")
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateStepUpCode(it) },
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verifyStepUp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Verify") }
    }
}

@Composable
private fun AddContactOtpContent(
    state: AccountSecurityState.AddContactOtp,
    viewModel: AccountSecurityViewModel,
) {
    AuthStepScaffold(title = "Verify new contact", onBack = { viewModel.back() }) {
        Text("Enter the code sent to ${state.contactValue}.")
        Spacer(modifier = Modifier.height(16.dp))
        OtpField(
            value = state.code,
            onValueChange = { viewModel.updateAddContactOtpCode(it) },
            error = state.error,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.verifyAddContactOtp() },
            enabled = state.code.length == 6,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Verify") }
    }
}

@Composable
private fun ChangePasswordContent(
    state: AccountSecurityState.ChangePassword,
    viewModel: AccountSecurityViewModel,
) {
    AuthStepScaffold(title = "Change password", onBack = { viewModel.back() }) {
        Text("Other devices will be signed out after password change.")
        Spacer(modifier = Modifier.height(16.dp))
        PasswordField(
            value = state.currentPassword,
            onValueChange = { viewModel.updateCurrentPassword(it) },
            label = "Current password",
            error = state.errors["current"],
        )
        PasswordField(
            value = state.newPassword,
            onValueChange = { viewModel.updateNewPassword(it) },
            label = "New password",
            error = state.errors["new"],
        )
        PasswordField(
            value = state.confirmPassword,
            onValueChange = { viewModel.updateConfirmPassword(it) },
            label = "Confirm new password",
            error = state.errors["confirm"],
        )
        FieldError(state.errors["_"])
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { viewModel.submitPasswordChange() },
            enabled = state.currentPassword.isNotBlank() && state.newPassword.isNotBlank() && state.confirmPassword.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Change password") }
    }
}
