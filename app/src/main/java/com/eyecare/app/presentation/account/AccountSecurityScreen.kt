package com.eyecare.app.presentation.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
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
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
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
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }

    AuthStepScaffold(title = "Account & Security", onBack = onBack, showGradientBar = false) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Contacts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        state.contacts.forEachIndexed { index, contact ->
                            if (index > 0) AccountSecurityDivider()
                            ContactRow(
                                contact = contact,
                                onMakePrimary = { viewModel.startStepUp(StepUpAction.MakePrimary(contact.id)) },
                                onRemove = { viewModel.startStepUp(StepUpAction.RemoveContact(contact.id)) },
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.startAddContact() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                    ),
                ) { Text("Add contact", fontWeight = FontWeight.SemiBold) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Security",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column {
                        SettingsNavRow(
                            icon = Icons.Outlined.Lock,
                            label = "Change password",
                            onClick = { viewModel.startStepUp(StepUpAction.ChangePassword) },
                        )
                        AccountSecurityDivider()
                        SettingsNavRow(
                            icon = Icons.AutoMirrored.Outlined.Logout,
                            label = "Sign out this device",
                            onClick = { showLogoutDialog = true },
                            isDestructive = true,
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showLogoutAllDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Sign out all devices", fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (showLogoutDialog) {
        AppConfirmationDialog(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Sign out?",
            message = "You'll be signed out from this device. You can sign in again anytime.",
            confirmLabel = "Sign out",
            dismissLabel = "Cancel",
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismissRequest = { showLogoutDialog = false },
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
        )
    }

    if (showLogoutAllDialog) {
        AppConfirmationDialog(
            icon = Icons.AutoMirrored.Outlined.Logout,
            title = "Sign out all devices?",
            message = "You'll be signed out from all devices where you're currently logged in.",
            confirmLabel = "Sign out all",
            dismissLabel = "Cancel",
            onConfirm = {
                showLogoutAllDialog = false
                viewModel.logoutAll()
            },
            onDismissRequest = { showLogoutAllDialog = false },
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
        )
    }
}

@Composable
private fun ContactRow(
    contact: AccountContact,
    onMakePrimary: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (contact.type == ContactType.EMAIL) Icons.Outlined.Email else Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = EyecareColors.current.accentText,
                    modifier = Modifier.size(21.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.maskedValue,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contact.isPrimary) {
                    Text(
                        text = "Primary",
                        style = MaterialTheme.typography.labelSmall,
                        color = EyecareColors.current.accentText,
                    )
                }
                if (contact.verifiedAt == null) {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        if (!contact.isPrimary && contact.verifiedAt != null) {
            TextButton(onClick = onMakePrimary) {
                Text("Make primary", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (!contact.isPrimary) {
            TextButton(onClick = onRemove) {
                Text(
                    "Remove",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun EnterNewContactContent(
    state: AccountSecurityState.EnterNewContact,
    viewModel: AccountSecurityViewModel,
) {
    AuthStepScaffold(title = "Add contact", onBack = { viewModel.back() }, showGradientBar = false) {
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
    AuthStepScaffold(title = "Verify it's you", onBack = { viewModel.back() }, showGradientBar = false) {
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
    AuthStepScaffold(title = "Verify new contact", onBack = { viewModel.back() }, showGradientBar = false) {
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
    AuthStepScaffold(title = "Change password", onBack = { viewModel.back() }, showGradientBar = false) {
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

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isDestructive) MaterialTheme.colorScheme.errorContainer
                else MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isDestructive) MaterialTheme.colorScheme.onErrorContainer
                        else EyecareColors.current.accentText,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }

            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (isDestructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AccountSecurityDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 66.dp, end = 14.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
