package com.eyecare.app.presentation.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.eyecare.app.domain.model.AccountContact
import com.eyecare.app.domain.model.ContactType
import com.eyecare.app.domain.model.PatientAccount
import com.eyecare.app.presentation.auth.components.AuthStepScaffold
import com.eyecare.app.presentation.auth.components.ContactField
import com.eyecare.app.presentation.auth.components.ContactMethod
import com.eyecare.app.presentation.auth.components.FieldError
import com.eyecare.app.presentation.auth.components.OtpField
import com.eyecare.app.presentation.auth.components.PasswordField
import com.eyecare.app.presentation.common.components.AppConfirmationDialog
import com.eyecare.app.presentation.appointments.requests.toDatePickerMillis
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun AccountSecurityScreen(
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
    onAccountUpdated: (PatientAccount) -> Unit = {},
    viewModel: AccountSecurityViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadAccount()
    }

    val overview = state as? AccountSecurityState.Overview
    val account = overview?.account
    LaunchedEffect(account) {
        if (account != null && !overview.isEditingAccount) {
            onAccountUpdated(account)
        }
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
        is AccountSecurityState.Overview -> AccountSecurityOverviewContent(
            state = s,
            onBack = onBack,
            onEdit = viewModel::startAccountEditing,
            onCancelEdit = viewModel::cancelAccountEditing,
            onFirstNameChange = viewModel::updateAccountFirstName,
            onMiddleNameChange = viewModel::updateAccountMiddleName,
            onLastNameChange = viewModel::updateAccountLastName,
            onDateOfBirthChange = viewModel::updateAccountDateOfBirth,
            onSave = viewModel::saveAccountDetails,
            onChangePassword = { viewModel.startStepUp(StepUpAction.ChangePassword) },
            onSignOut = viewModel::logout,
            onSignOutAll = viewModel::logoutAll,
            onAddContact = viewModel::startAddContact,
            onMakePrimary = { contactId -> viewModel.startStepUp(StepUpAction.MakePrimary(contactId)) },
            onRemoveContact = { contactId -> viewModel.startStepUp(StepUpAction.RemoveContact(contactId)) },
            onRetryContacts = viewModel::loadAccount,
        )
        is AccountSecurityState.EnterNewContact -> EnterNewContactContent(s, viewModel)
        is AccountSecurityState.StepUpOtp -> StepUpOtpContent(s, viewModel)
        is AccountSecurityState.AddContactOtp -> AddContactOtpContent(s, viewModel)
        is AccountSecurityState.ChangePassword -> ChangePasswordContent(s, viewModel)
        is AccountSecurityState.Result -> {
            LaunchedEffect(s.message) { viewModel.loadAccount() }
            AccountSecurityOverviewContent(
                state = AccountSecurityState.Overview(account = s.account),
                onBack = onBack,
                onEdit = viewModel::startAccountEditing,
                onCancelEdit = viewModel::cancelAccountEditing,
                onFirstNameChange = viewModel::updateAccountFirstName,
                onMiddleNameChange = viewModel::updateAccountMiddleName,
                onLastNameChange = viewModel::updateAccountLastName,
                onDateOfBirthChange = viewModel::updateAccountDateOfBirth,
                onSave = viewModel::saveAccountDetails,
                onChangePassword = { viewModel.startStepUp(StepUpAction.ChangePassword) },
                onSignOut = viewModel::logout,
                onSignOutAll = viewModel::logoutAll,
                onAddContact = viewModel::startAddContact,
                onMakePrimary = { contactId -> viewModel.startStepUp(StepUpAction.MakePrimary(contactId)) },
                onRemoveContact = { contactId -> viewModel.startStepUp(StepUpAction.RemoveContact(contactId)) },
                onRetryContacts = viewModel::loadAccount,
            )
        }
        is AccountSecurityState.SignedOut -> {
            LaunchedEffect(Unit) { onSignedOut() }
        }
    }
}

@Composable
fun AccountSecurityOverviewContent(
    state: AccountSecurityState.Overview,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onSave: () -> Unit,
    onChangePassword: () -> Unit,
    onSignOut: () -> Unit,
    onSignOutAll: () -> Unit,
    onAddContact: (ContactType) -> Unit = {},
    onMakePrimary: (Int) -> Unit = {},
    onRemoveContact: (Int) -> Unit = {},
    onRetryContacts: () -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLogoutAllDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isDirty = state.account?.let { account ->
        state.isEditingAccount && AccountProfileEditor.isDirty(
            ProfileDraft(
                firstName = state.editFirstName,
                middleName = state.editMiddleName,
                lastName = state.editLastName,
                dateOfBirth = state.editDateOfBirth,
            ),
            account,
        )
    } == true

    val handleCancelOrBack: () -> Unit = handleCancelOrBack@{
        if (state.isSavingAccount || state.isRequestingStepUp) {
            return@handleCancelOrBack
        }
        if (isDirty) {
            showDiscardDialog = true
        } else {
            onCancelEdit()
        }
    }

    if (state.isEditingAccount) {
        BackHandler { handleCancelOrBack() }
    }

    AuthStepScaffold(
        title = "Account details",
        onBack = { if (state.isEditingAccount) handleCancelOrBack() else onBack() },
        showGradientBar = false,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (state.error != null) {
                Text(state.error, color = MaterialTheme.colorScheme.error)
            }

            state.account?.let { account ->
                AccountDetailsContent(
                    account = account,
                    isEditing = state.isEditingAccount,
                    isSaving = state.isSavingAccount,
                    isRequestingStepUp = state.isRequestingStepUp,
                    firstName = state.editFirstName,
                    middleName = state.editMiddleName,
                    lastName = state.editLastName,
                    dateOfBirth = state.editDateOfBirth,
                    contacts = state.contacts,
                    contactsError = state.contactsError,
                    fieldErrors = state.fieldErrors,
                    saveError = state.accountSaveError,
                    onEdit = onEdit,
                    onCancel = handleCancelOrBack,
                    onFirstNameChange = onFirstNameChange,
                    onMiddleNameChange = onMiddleNameChange,
                    onLastNameChange = onLastNameChange,
                    onDateOfBirthChange = onDateOfBirthChange,
                    onSave = onSave,
                    onAddContact = onAddContact,
                    onMakePrimary = onMakePrimary,
                    onRemoveContact = onRemoveContact,
                    onRetryContacts = onRetryContacts,
                )
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
                            onClick = onChangePassword,
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
                onSignOut()
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
                onSignOutAll()
            },
            onDismissRequest = { showLogoutAllDialog = false },
            iconTint = MaterialTheme.colorScheme.error,
            isDestructive = true,
        )
    }

    if (showDiscardDialog) {
        AppConfirmationDialog(
            icon = Icons.Outlined.Edit,
            title = "Discard changes?",
            message = "You have unsaved changes. Are you sure you want to discard them?",
            confirmLabel = "Discard",
            dismissLabel = "Keep editing",
            onConfirm = {
                showDiscardDialog = false
                onCancelEdit()
            },
            onDismissRequest = { showDiscardDialog = false },
        )
    }
}

@Composable
fun AccountDetailsContent(
    account: PatientAccount,
    isEditing: Boolean,
    isSaving: Boolean,
    isRequestingStepUp: Boolean = false,
    firstName: String,
    middleName: String,
    lastName: String,
    dateOfBirth: String,
    contacts: List<AccountContact> = emptyList(),
    contactsError: String? = null,
    fieldErrors: Map<String, String>,
    saveError: String?,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
    onFirstNameChange: (String) -> Unit,
    onMiddleNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onDateOfBirthChange: (String) -> Unit,
    onSave: () -> Unit,
    onAddContact: (ContactType) -> Unit = {},
    onMakePrimary: (Int) -> Unit = {},
    onRemoveContact: (Int) -> Unit = {},
    onRetryContacts: () -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isBusy = isSaving || isRequestingStepUp
    val isDirty = isEditing && AccountProfileEditor.isDirty(
        ProfileDraft(
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
        ),
        account,
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Profile details",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (!isEditing) {
                TextButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Edit")
                }
            }
        }

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (isEditing) {
                    Text(
                        text = "Names and account date of birth can be edited here. Contact and clinical details use separate workflows.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = onFirstNameChange,
                        label = { Text("First name") },
                        singleLine = true,
                        isError = fieldErrors.containsKey("first_name"),
                        supportingText = fieldErrors["first_name"]?.let { error -> { Text(error) } },
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = middleName,
                        onValueChange = onMiddleNameChange,
                        label = { Text("Middle name") },
                        singleLine = true,
                        isError = fieldErrors.containsKey("middle_name"),
                        supportingText = fieldErrors["middle_name"]?.let { error -> { Text(error) } },
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = onLastNameChange,
                        label = { Text("Last name") },
                        singleLine = true,
                        isError = fieldErrors.containsKey("last_name"),
                        supportingText = fieldErrors["last_name"]?.let { error -> { Text(error) } },
                        enabled = !isBusy,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    DateOfBirthEditField(
                        value = dateOfBirth,
                        error = fieldErrors["date_of_birth"],
                        enabled = !isBusy,
                        onClick = { showDatePicker = true },
                    )
                }

                if (!isEditing) {
                    AccountDetailRow("First name", displayAccountValue(account.firstName))
                    AccountDetailRow("Middle name", displayAccountValue(account.middleName))
                    AccountDetailRow("Last name", displayAccountValue(account.lastName))
                    AccountDetailRow("Date of birth", formatAccountDate(account.dateOfBirth))
                }

                ContactInformationSection(
                    account = account,
                    contacts = contacts,
                    contactsError = contactsError,
                    actionsEnabled = !isEditing && !isBusy,
                    onAddContact = onAddContact,
                    onMakePrimary = onMakePrimary,
                    onRemoveContact = onRemoveContact,
                    onRetryContacts = onRetryContacts,
                )

                if (saveError != null) {
                    Text(
                        text = saveError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (isEditing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isBusy,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = onSave,
                            enabled = !isBusy && isDirty,
                            modifier = Modifier.weight(1f).height(48.dp),
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Save")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val today = remember { java.time.LocalDate.now(java.time.ZoneId.of("Asia/Manila")) }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dateOfBirth.toDatePickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    java.time.Instant.ofEpochMilli(utcTimeMillis)
                        .atZone(java.time.ZoneId.of("Asia/Manila"))
                        .toLocalDate()
                        .isBefore(today)
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateOfBirthChange(
                            java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.of("Asia/Manila"))
                                .toLocalDate()
                                .toString(),
                        )
                    }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun DateOfBirthEditField(
    value: String,
    error: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val display = value.takeIf { it.isNotBlank() }?.let { formatAccountDate(it) }
    OutlinedTextField(
        value = display.orEmpty(),
        onValueChange = {},
        label = { Text("Date of birth") },
        placeholder = { Text("Choose a date") },
        readOnly = true,
        enabled = enabled,
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        trailingIcon = {
            Icon(
                imageVector = Icons.Outlined.CalendarMonth,
                contentDescription = null,
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(enabled) {
                if (enabled) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        onClick()
                    }
                }
            }
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = buildString {
                    append("Date of birth, ")
                    append(display ?: "not set")
                    if (error != null) append(", $error")
                    append(". Double tap to choose a date.")
                }
            },
    )
}

@Composable
private fun ContactInformationSection(
    account: PatientAccount,
    contacts: List<AccountContact>,
    contactsError: String?,
    actionsEnabled: Boolean,
    onAddContact: (ContactType) -> Unit,
    onMakePrimary: (Int) -> Unit,
    onRemoveContact: (Int) -> Unit,
    onRetryContacts: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider()
        Text(
            text = "Contact information",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        if (contactsError != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Contact information is unavailable.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = onRetryContacts,
                    enabled = actionsEnabled,
                ) { Text("Retry") }
            }
        }

        ContactDetailRow(
            account = account,
            contact = contacts.firstOrNull { it.type == ContactType.EMAIL },
            type = ContactType.EMAIL,
            actionsEnabled = actionsEnabled,
            onAddContact = onAddContact,
            onMakePrimary = onMakePrimary,
            onRemoveContact = onRemoveContact,
        )
        ContactDetailRow(
            account = account,
            contact = contacts.firstOrNull { it.type == ContactType.PHONE },
            type = ContactType.PHONE,
            actionsEnabled = actionsEnabled,
            onAddContact = onAddContact,
            onMakePrimary = onMakePrimary,
            onRemoveContact = onRemoveContact,
        )
    }
}

@Composable
private fun ContactDetailRow(
    account: PatientAccount,
    contact: AccountContact?,
    type: ContactType,
    actionsEnabled: Boolean,
    onAddContact: (ContactType) -> Unit,
    onMakePrimary: (Int) -> Unit,
    onRemoveContact: (Int) -> Unit,
) {
    val label = if (type == ContactType.EMAIL) "Email" else "Phone"
    val accountValue = if (type == ContactType.EMAIL) account.email else account.phone
    val value = contact?.maskedValue?.takeIf { it.isNotBlank() }
        ?: displayAccountValue(accountValue)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (type == ContactType.EMAIL) Icons.Outlined.Email else Icons.Outlined.Phone,
                    contentDescription = null,
                    tint = EyecareColors.current.accentText,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (contact?.isPrimary == true) {
                Text(
                    text = "Primary",
                    style = MaterialTheme.typography.labelSmall,
                    color = EyecareColors.current.accentText,
                )
            } else if (contact?.verifiedAt == null && contact != null) {
                Text(
                    text = "Pending",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            if (contact == null && accountValue.isNullOrBlank()) {
                TextButton(
                    onClick = { onAddContact(type) },
                    enabled = actionsEnabled,
                ) { Text("Add $label") }
            }
            if (contact != null && !contact.isPrimary && contact.verifiedAt != null) {
                TextButton(
                    onClick = { onMakePrimary(contact.id) },
                    enabled = actionsEnabled,
                ) { Text("Make primary") }
            }
            if (contact != null && !contact.isPrimary) {
                TextButton(
                    onClick = { onRemoveContact(contact.id) },
                    enabled = actionsEnabled,
                ) {
                    Text(
                        text = "Remove",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

private fun displayAccountValue(value: String?): String = value?.takeIf { it.isNotBlank() } ?: "Not provided"

private fun formatAccountDate(value: String?): String {
    val parts = value?.split("-") ?: return "Not provided"
    if (parts.size != 3) return displayAccountValue(value)
    val month = parts[1].toIntOrNull()?.let { accountMonthNames.getOrNull(it - 1) }
        ?: return displayAccountValue(value)
    return "$month ${parts[2].toIntOrNull() ?: parts[2]}, ${parts[0]}"
}

private val accountMonthNames = listOf(
    "January",
    "February",
    "March",
    "April",
    "May",
    "June",
    "July",
    "August",
    "September",
    "October",
    "November",
    "December",
)

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
            enabled = state.code.length == 6 && !state.isVerifying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isVerifying) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text("Verify")
            }
        }
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
